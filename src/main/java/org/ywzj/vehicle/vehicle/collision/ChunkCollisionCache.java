package org.ywzj.vehicle.vehicle.collision;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * Per-level cache of SectionCollision snapshots, so vehicle physics can ask what geometry is
 * where without touching a live PalettedContainer. Split into two phases: prepare() runs on the
 * tick thread and builds snapshots; collectBoxes(), anyBoxIn(), and Cursor read only finished
 * snapshots and are safe from worker threads. Snapshots are invalidated by invalidate(), driven
 * from a LevelChunk.setBlockState mixin plus chunk load/unload events. Pinning captures snapshot
 * references into a PinnedSections on the tick thread, making a solve deterministic off-thread.
 */
public final class ChunkCollisionCache implements SectionSource {

    /**
     * A frozen view over the sections one vehicle's physics may touch this tick.
     * Filled by prepareAndPin() on the tick thread, read by the solve wherever it runs.
     * Holds references only; SectionCollision is immutable, so the fill costs nothing to keep.
     * Reused per vehicle; never shared.
     */
    public static final class PinnedSections implements SectionSource {

        private final Long2ObjectOpenHashMap<SectionCollision> pinned =
                new Long2ObjectOpenHashMap<>();

        @Override
        @Nullable
        public SectionCollision section(long key) {
            return pinned.get(key);
        }

        public void clear() {
            pinned.clear();
        }

        void pin(long key, SectionCollision snapshot) {
            pinned.put(key, snapshot);
        }

    }

    /**
     * Soft cap on retained snapshots; roughly 16MB per level assuming mixed sections cost 8KB each.
     */
    private static final int MAX_SECTIONS = 2048;

    /** Snapshots untouched for this many ticks are dropped once the cache is over budget. */
    private static final int RETAIN_TICKS = 200;

    private static final Map<Level, ChunkCollisionCache> CACHES = new ConcurrentHashMap<>();

    private final Long2ObjectOpenHashMap<SectionCollision> sections = new Long2ObjectOpenHashMap<>();
    private final StampedLock sectionsLock = new StampedLock();
    private volatile int tick;

    /**
     * Per-thread traversal scratch for the per-section trees. The trees are shared across every
     * vehicle over a section, so the scratch cannot live on them; per thread keeps concurrent
     * readers safe when physics runs on worker threads.
     */
    private static final ThreadLocal<DynamicTree.QueryScratch> QUERY_SCRATCH =
            ThreadLocal.withInitial(DynamicTree.QueryScratch::new);

    private ChunkCollisionCache() {}

    public static ChunkCollisionCache of(Level level) {
        return CACHES.computeIfAbsent(level, unused -> new ChunkCollisionCache());
    }

    /** Drops a level's cache entirely. Called when the level unloads. */
    public static void forget(Level level) {
        CACHES.remove(level);
    }

    /** The live view of this cache, for queries that can and should see current data. */
    @Override
    @Nullable
    public SectionCollision section(long key) {
        return read(key);
    }

    private SectionCollision read(long key) {
        long stamp = sectionsLock.tryOptimisticRead();
        SectionCollision snapshot;
        try {
            snapshot = sections.get(key);
        } catch (RuntimeException raced) {
            // A probe over a table mid-rehash can trip a bounds check when the mask and key
            // array are read torn. Transient; fall through to the locked retry.
            snapshot = null;
            stamp = 0;
        }
        if (!sectionsLock.validate(stamp)) {
            stamp = sectionsLock.readLock();
            try {
                snapshot = sections.get(key);
            } finally {
                sectionsLock.unlockRead(stamp);
            }
        }
        return snapshot;
    }

    private void write(long key, SectionCollision snapshot) {
        long stamp = sectionsLock.writeLock();
        try {
            sections.put(key, snapshot);
        } finally {
            sectionsLock.unlockWrite(stamp);
        }
    }

    /** Drops the snapshot covering a changed block. Cheap enough for every block update. */
    public static void invalidate(Level level, BlockPos pos) {
        ChunkCollisionCache cache = CACHES.get(level);
        if (cache == null) {
            return;
        }
        long stamp = cache.sectionsLock.writeLock();
        try {
            cache.sections.remove(SectionPos.asLong(pos));
        } finally {
            cache.sectionsLock.unlockWrite(stamp);
        }
    }

    /** Drops every snapshot belonging to a chunk. Used on chunk load and unload. */
    public static void invalidateChunk(Level level, ChunkPos chunkPos) {
        ChunkCollisionCache cache = CACHES.get(level);
        if (cache == null) {
            return;
        }
        long stamp = cache.sectionsLock.writeLock();
        try {
            cache.sections.keySet().removeIf((long key) ->
                    SectionPos.x(key) == chunkPos.x && SectionPos.z(key) == chunkPos.z);
        } finally {
            cache.sectionsLock.unlockWrite(stamp);
        }
    }

    /**
     * Tick-thread only. Ensures every section overlapping bounds has a snapshot; reports whether
     * any could produce a contact. A false return is exact: every overlapping section holds no
     * collision shapes, so the query would be empty. Aircraft and ships skip this when over empty sections.
     */
    public boolean prepare(Level level, AABB bounds) {
        return prepareAndPin(level, bounds, null);
    }

    /**
     * Like prepare(), and additionally pins the region's snapshots into pin. Tick-thread only.
     * The pin replaces whatever the view held before, so re-pinning each tick never accumulates
     * stale sections. Empty snapshots are pinned too, keeping the view honest about its coverage.
     */
    public boolean prepareAndPin(Level level, AABB bounds, @Nullable PinnedSections pin) {
        if (pin != null) {
            pin.clear();
        }
        if (level.isDebug()) {
            // Debug worlds synthesize block states rather than storing them. Report true
            // so the caller falls back to live reads.
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
                    SectionCollision snapshot = read(key);
                    if (snapshot == null) {
                        snapshot = build(chunk, sectionY);
                        write(key, snapshot);
                    }
                    snapshot.lastUseTick = tick;
                    anySolid |= !snapshot.isEmpty();
                    if (pin != null) {
                        pin.pin(key, snapshot);
                    }
                }
            }
        }

        if (sections.size() > MAX_SECTIONS) {
            evict();
        }
        return anySolid;
    }

    /**
     * Unloaded chunks are treated as empty rather than force-loaded. Level.getBlockState loads
     * and generates chunks, but driving world generation from physics is a latent hitch that
     * cannot run off-thread. Hulls reaching into unloaded terrain pass through instead of stalling.
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
        long stamp = sectionsLock.writeLock();
        try {
            sections.values().removeIf(snapshot -> snapshot.lastUseTick < cutoff);
            if (sections.size() > MAX_SECTIONS) {
                // All sections are in active use. Drop everything; rebuilds lazily.
                sections.clear();
            }
        } finally {
            sectionsLock.unlockWrite(stamp);
        }
    }

    /**
     * A read cursor over prepared snapshots. Memoizes the last section since consecutive hull
     * sample points nearly always fall in the same section, collapsing the map lookup to a
     * long compare for most queries. Not thread-safe; give each thread or vehicle its own.
     */
    public static final class Cursor {

        private final SectionSource source;
        private long lastKey = Long.MIN_VALUE;
        private SectionCollision lastSection = SectionCollision.EMPTY;

        Cursor(SectionSource source) {
            this.source = source;
        }

        private SectionCollision resolve(int x, int y, int z) {
            long key = SectionPos.asLong(x >> 4, y >> 4, z >> 4);
            if (key != lastKey) {
                SectionCollision snapshot = source.section(key);
                lastSection = snapshot == null ? SectionCollision.EMPTY : snapshot;
                lastKey = key;
            }
            return lastSection;
        }

        /**
         * The colliding block state at a block position, or null if nothing there has a
         * collision shape. Reads prepared snapshots only, safe off-thread; unprepared sections read as empty.
         */
        @Nullable
        public BlockState collisionAt(int x, int y, int z) {
            return resolve(x, y, z).collisionAt(SectionCollision.cellIndex(x & 15, y & 15, z & 15));
        }

        /**
         * World-space height of the tallest collision box in a block cell. Equal to y when
         * the cell holds nothing.
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
        return new Cursor(this);
    }

    /** A cursor over an arbitrary source, such as a PinnedSections view for an off-thread solve. */
    public static Cursor cursorOver(SectionSource source) {
        return new Cursor(source);
    }

    /**
     * Whether any collision box intersects bounds, stopping at the first found. Faster than
     * gathering all boxes in the region for common cases like open sky or level ground.
     */
    public boolean anyBoxIn(AABB bounds) {
        return anyBoxIn(this, bounds);
    }

    /** Like anyBoxIn(AABB), resolving sections through source. */
    public static boolean anyBoxIn(SectionSource source, AABB bounds) {
        int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(bounds.minX));
        int minSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.minY));
        int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ));
        int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxX));
        int maxSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxY));
        int maxSectionZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxZ));

        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    SectionCollision snapshot = source.section(SectionPos.asLong(sectionX, sectionY, sectionZ));
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
                        if (x0 + SectionCollision.boxSizeX(packed) > bounds.minX && x0 < bounds.maxX
                                && y0 + SectionCollision.boxSizeY(packed) > bounds.minY && y0 < bounds.maxY
                                && z0 + SectionCollision.boxSizeZ(packed) > bounds.minZ && z0 < bounds.maxZ) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Collects the merged collision boxes overlapping bounds into out, in world coordinates.
     * The broad phase for the inverted query. Reads prepared snapshots only, safe off-thread;
     * unprepared or empty sections contribute nothing. This form is preferred; AABB overload
     * allocates one object per box.
     */
    public void collectBoxes(AABB bounds, BoxBuffer out) {
        forEachBox(this, bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ, out::add);
    }

    /** Like above but with primitive bounds; per-substep query allocates nothing. */
    public void collectBoxes(double minX, double minY, double minZ,
                             double maxX, double maxY, double maxZ, BoxBuffer out) {
        forEachBox(this, minX, minY, minZ, maxX, maxY, maxZ, out::add);
    }

    public void collectBoxes(AABB bounds, List<AABB> out) {
        forEachBox(this, bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ,
                (x0, y0, z0, x1, y1, z1) -> out.add(new AABB(x0, y0, z0, x1, y1, z1)));
    }

    /** Like collectBoxes(AABB, BoxBuffer), resolving sections through source. */
    public static void collectBoxes(SectionSource source, AABB bounds, BoxBuffer out) {
        forEachBox(source, bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ, out::add);
    }

    /** Primitive-bounds form over an arbitrary source; for the substep broadphase. */
    public static void collectBoxes(SectionSource source, double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ, BoxBuffer out) {
        forEachBox(source, minX, minY, minZ, maxX, maxY, maxZ, out::add);
    }

    /** Receives each merged box as primitives. */
    private interface BoxSink {
        void accept(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }

    private static void forEachBox(SectionSource source,
                                   double boundsMinX, double boundsMinY, double boundsMinZ,
                                   double boundsMaxX, double boundsMaxY, double boundsMaxZ, BoxSink out) {
        int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(boundsMinX));
        int minSectionY = SectionPos.blockToSectionCoord(Mth.floor(boundsMinY));
        int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(boundsMinZ));
        int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(boundsMaxX));
        int maxSectionY = SectionPos.blockToSectionCoord(Mth.floor(boundsMaxY));
        int maxSectionZ = SectionPos.blockToSectionCoord(Mth.floor(boundsMaxZ));

        DynamicTree.QueryScratch scratch = null;
        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    SectionCollision snapshot = source.section(SectionPos.asLong(sectionX, sectionY, sectionZ));
                    if (snapshot == null || snapshot.isEmpty()) {
                        continue;
                    }
                    int originX = SectionPos.sectionToBlockCoord(sectionX);
                    int originY = SectionPos.sectionToBlockCoord(sectionY);
                    int originZ = SectionPos.sectionToBlockCoord(sectionZ);
                    long[] boxes = snapshot.collisionBoxes();
                    DynamicTree tree = snapshot.collisionTree();
                    if (tree != null) {
                        if (scratch == null) {
                            scratch = QUERY_SCRATCH.get();
                        }
                        int count = tree.query(scratch,
                                boundsMinX - originX, boundsMinY - originY, boundsMinZ - originZ,
                                boundsMaxX - originX, boundsMaxY - originY, boundsMaxZ - originZ);
                        for (int i = 0; i < count; i++) {
                            emitBox(boundsMinX, boundsMinY, boundsMinZ,
                                    boundsMaxX, boundsMaxY, boundsMaxZ,
                                    out, boxes[scratch.result(i)], originX, originY, originZ);
                        }
                    } else {
                        for (long packed : boxes) {
                            emitBox(boundsMinX, boundsMinY, boundsMinZ,
                                    boundsMaxX, boundsMaxY, boundsMaxZ,
                                    out, packed, originX, originY, originZ);
                        }
                    }
                }
            }
        }
    }

    /**
     * Unpacks one merged box to world coordinates and hands it over if it genuinely intersects.
     * The exact test remains even behind a tree query, whose fattened proxies may return boxes
     * outside the bound. This preserves the strictly-exclusive filter callers depend on.
     */
    private static void emitBox(double boundsMinX, double boundsMinY, double boundsMinZ,
                                double boundsMaxX, double boundsMaxY, double boundsMaxZ,
                                BoxSink out, long packed,
                                int originX, int originY, int originZ) {
        double x0 = originX + SectionCollision.boxMinX(packed);
        double y0 = originY + SectionCollision.boxMinY(packed);
        double z0 = originZ + SectionCollision.boxMinZ(packed);
        double x1 = x0 + SectionCollision.boxSizeX(packed);
        double y1 = y0 + SectionCollision.boxSizeY(packed);
        double z1 = z0 + SectionCollision.boxSizeZ(packed);
        if (x1 <= boundsMinX || x0 >= boundsMaxX
                || y1 <= boundsMinY || y0 >= boundsMaxY
                || z1 <= boundsMinZ || z0 >= boundsMaxZ) {
            return;
        }
        out.accept(x0, y0, z0, x1, y1, z1);
    }

    /**
     * The snapshot for a section, or null if never prepared. For the debug overlay; physics
     * should go through a Cursor.
     */
    @Nullable
    public SectionCollision snapshotAt(long sectionKey) {
        return read(sectionKey);
    }

    /** Number of retained snapshots; for debug overlay readout. */
    public int cachedSectionCount() {
        return sections.size();
    }

    /** Approximate retained bytes across all snapshots; for debug overlay readout. */
    public long footprint() {
        long stamp = sectionsLock.readLock();
        try {
            long total = 0;
            for (SectionCollision snapshot : sections.values()) {
                total += snapshot.footprint();
            }
            return total;
        } finally {
            sectionsLock.unlockRead(stamp);
        }
    }

}
