package org.ywzj.vehicle.vehicle.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-level cache of {@link SectionCollision} snapshots, so vehicle physics can ask what geometry
 * is where without touching a live {@code PalettedContainer}.
 * <p>
 * Split into two phases on purpose:
 * <ul>
 *   <li>{@link #prepare} runs on the tick thread. It reads chunk data, builds any missing
 *       snapshots, and answers the broad-phase question "could anything here collide at all?"</li>
 *   <li>{@link #collectBoxes} and {@link Cursor} read only finished snapshots. No locks, no chunk
 *       access, no palette access — safe to call from a worker thread once {@code prepare} has
 *       returned.</li>
 * </ul>
 * That split is what lets vehicle physics move off the tick thread later: the only step that
 * must stay on it is {@code prepare}.
 * <p>
 * Snapshots are invalidated by {@link #invalidate}, driven from a {@code LevelChunk.setBlockState}
 * mixin plus chunk load/unload events. Invalidation removes the entry outright, so a snapshot
 * object is never mutated and a reader mid-query always sees a self-consistent section.
 */
public final class ChunkCollisionCache {

    /**
     * Soft cap on retained snapshots. A mixed section costs ~4KB for its cell index plus its
     * merged boxes, which for ordinary terrain run to a few hundred — call it 8KB a section, so
     * roughly 16MB per level if every retained section is mixed.
     */
    private static final int MAX_SECTIONS = 2048;

    /** Snapshots untouched for this many ticks are dropped once the cache is over budget. */
    private static final int RETAIN_TICKS = 200;

    private static final Map<Level, ChunkCollisionCache> CACHES = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, SectionCollision> sections = new ConcurrentHashMap<>();
    private volatile int tick;

    private ChunkCollisionCache() {}

    public static ChunkCollisionCache of(Level level) {
        return CACHES.computeIfAbsent(level, unused -> new ChunkCollisionCache());
    }

    /** Drops a level's cache entirely. Called when the level unloads. */
    public static void forget(Level level) {
        CACHES.remove(level);
    }

    /** Drops the snapshot covering a changed block. Cheap enough for every block update. */
    public static void invalidate(Level level, BlockPos pos) {
        ChunkCollisionCache cache = CACHES.get(level);
        if (cache != null) {
            cache.sections.remove(SectionPos.asLong(pos));
        }
    }

    /** Drops every snapshot belonging to a chunk. Used on chunk load and unload. */
    public static void invalidateChunk(Level level, ChunkPos chunkPos) {
        ChunkCollisionCache cache = CACHES.get(level);
        if (cache == null) {
            return;
        }
        cache.sections.keySet().removeIf(key ->
                SectionPos.x(key) == chunkPos.x && SectionPos.z(key) == chunkPos.z);
    }

    /**
     * Tick-thread only. Ensures every section overlapping {@code bounds} has a snapshot, and
     * reports whether any of them could produce a contact.
     * <p>
     * A {@code false} return is exact, not a heuristic: it means every overlapping section was
     * proven to hold nothing with a collision shape, so the query would have produced an empty
     * contact list. Callers may skip it entirely — that is the whole win for aircraft and ships,
     * which spend nearly all their time over sections that contain nothing to hit.
     */
    public boolean prepare(Level level, AABB bounds) {
        if (level.isDebug()) {
            // Debug worlds synthesise block states rather than storing them; snapshotting them
            // would be wrong. Report "maybe solid" so the caller falls back to live reads.
            return true;
        }
        this.tick = (int) level.getGameTime();

        int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(bounds.minX));
        int minSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.minY));
        int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ));
        int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxX));
        int maxSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxY));
        int maxSectionZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxZ));

        boolean anySolid = false;
        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                ChunkAccess chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
                    SectionCollision snapshot = sections.get(key);
                    if (snapshot == null) {
                        snapshot = build(chunk, sectionY);
                        sections.put(key, snapshot);
                    }
                    snapshot.lastUseTick = tick;
                    anySolid |= !snapshot.isEmpty();
                }
            }
        }

        if (sections.size() > MAX_SECTIONS) {
            evict();
        }
        return anySolid;
    }

    /**
     * Unloaded chunks are treated as empty rather than being force-loaded.
     * <p>
     * This is a deliberate departure from {@code Level.getBlockState}, which loads and if
     * necessary generates the chunk. Driving world generation from a physics inner loop is a
     * latent hitch, and it cannot be done off the tick thread at all. A vehicle hull reaching
     * into unloaded terrain now passes through it instead of stalling the server.
     */
    private static SectionCollision build(@Nullable ChunkAccess chunk, int sectionY) {
        if (chunk == null) {
            return SectionCollision.EMPTY;
        }
        int index = chunk.getSectionIndexFromSectionY(sectionY);
        LevelChunkSection[] chunkSections = chunk.getSections();
        if (index < 0 || index >= chunkSections.length) {
            return SectionCollision.EMPTY;
        }
        return SectionCollision.snapshot(chunkSections[index]);
    }

    private void evict() {
        int cutoff = tick - RETAIN_TICKS;
        Iterator<Map.Entry<Long, SectionCollision>> iterator = sections.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().lastUseTick < cutoff) {
                iterator.remove();
            }
        }
        if (sections.size() > MAX_SECTIONS) {
            // Everything is in active use by something far larger than the budget assumed.
            // Dropping it all is better than growing without bound; it rebuilds lazily.
            sections.clear();
        }
    }

    /**
     * A read cursor over prepared snapshots.
     * <p>
     * Holds the last section it resolved, which matters because hull sample points are
     * generated face by face and consecutive points nearly always fall in the same section —
     * so the map lookup collapses to a {@code long} compare for most queries.
     * <p>
     * Not thread-safe by itself: give each thread (or each vehicle) its own.
     */
    public final class Cursor {

        private long lastKey = Long.MIN_VALUE;
        private SectionCollision lastSection = SectionCollision.EMPTY;

        private SectionCollision resolve(int x, int y, int z) {
            long key = SectionPos.asLong(x >> 4, y >> 4, z >> 4);
            if (key != lastKey) {
                SectionCollision snapshot = sections.get(key);
                lastSection = snapshot == null ? SectionCollision.EMPTY : snapshot;
                lastKey = key;
            }
            return lastSection;
        }

        /**
         * The colliding block state at a block position, or {@code null} if nothing there has a
         * collision shape.
         * <p>
         * Reads prepared snapshots only, so this is safe off the tick thread. A section that
         * was never prepared reads as empty.
         */
        @Nullable
        public BlockState collisionAt(int x, int y, int z) {
            return resolve(x, y, z).collisionAt(SectionCollision.cellIndex(x & 15, y & 15, z & 15));
        }

        /**
         * World-space height of the tallest collision box in a block cell, so a caller stepping
         * onto it knows how far up "on top" is. Equal to {@code y} when the cell holds nothing.
         */
        public double collisionTop(int x, int y, int z) {
            return y + resolve(x, y, z).collisionTop(SectionCollision.cellIndex(x & 15, y & 15, z & 15));
        }

        /** Forgets the memoised section. Call after anything may have invalidated the cache. */
        public void reset() {
            lastKey = Long.MIN_VALUE;
            lastSection = SectionCollision.EMPTY;
        }

    }

    public Cursor cursor() {
        return new Cursor();
    }

    /**
     * Collects the merged collision boxes overlapping {@code bounds} into {@code out}, in world
     * coordinates. The broad phase for the inverted query.
     * <p>
     * Reads prepared snapshots only, so this is safe off the tick thread. Sections that were
     * never prepared, or that were proven empty, contribute nothing — an empty result means the
     * hull has nothing to collide with.
     */
    /**
     * As {@link #collectBoxes(AABB, List)}, but writing primitives into a reusable buffer.
     * <p>
     * This is the form physics should use. The {@link AABB} overload has to allocate one immutable
     * object per merged box, and they go straight into a list, so they escape and no amount of JIT
     * cleverness removes them. A vehicle sitting on terrain runs this twice a tick.
     */
    public void collectBoxes(AABB bounds, BoxBuffer out) {
        forEachBox(bounds, out::add);
    }

    public void collectBoxes(AABB bounds, List<AABB> out) {
        forEachBox(bounds, (x0, y0, z0, x1, y1, z1) -> out.add(new AABB(x0, y0, z0, x1, y1, z1)));
    }

    /** Receives each merged box as primitives. */
    private interface BoxSink {
        void accept(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }

    private void forEachBox(AABB bounds, BoxSink out) {
        int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(bounds.minX));
        int minSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.minY));
        int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ));
        int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxX));
        int maxSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxY));
        int maxSectionZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxZ));

        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    SectionCollision snapshot = sections.get(SectionPos.asLong(sectionX, sectionY, sectionZ));
                    if (snapshot == null || snapshot.isEmpty()) {
                        continue;
                    }
                    int originX = SectionPos.sectionToBlockCoord(sectionX);
                    int originY = SectionPos.sectionToBlockCoord(sectionY);
                    int originZ = SectionPos.sectionToBlockCoord(sectionZ);
                    for (long packed : snapshot.collisionBoxes()) {
                        double x0 = originX + SectionCollision.boxMinX(packed);
                        double y0 = originY + SectionCollision.boxMinY(packed);
                        double z0 = originZ + SectionCollision.boxMinZ(packed);
                        double x1 = x0 + SectionCollision.boxSizeX(packed);
                        double y1 = y0 + SectionCollision.boxSizeY(packed);
                        double z1 = z0 + SectionCollision.boxSizeZ(packed);
                        if (x1 <= bounds.minX || x0 >= bounds.maxX
                                || y1 <= bounds.minY || y0 >= bounds.maxY
                                || z1 <= bounds.minZ || z0 >= bounds.maxZ) {
                            continue;
                        }
                        out.accept(x0, y0, z0, x1, y1, z1);
                    }
                }
            }
        }
    }

    /**
     * The snapshot for a section, or {@code null} if it was never prepared. For the debug
     * overlay — physics should go through a {@link Cursor}.
     */
    @Nullable
    public SectionCollision snapshotAt(long sectionKey) {
        return sections.get(sectionKey);
    }

    /** Number of retained snapshots, for the debug overlay's readout. */
    public int cachedSectionCount() {
        return sections.size();
    }

    /** Approximate retained bytes across all snapshots, for the debug overlay's readout. */
    public long footprint() {
        long total = 0;
        for (SectionCollision snapshot : sections.values()) {
            total += snapshot.footprint();
        }
        return total;
    }

}
