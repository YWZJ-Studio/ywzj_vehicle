package org.ywzj.vehicle.vehicle.collision;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable collision snapshot of one 16³ chunk section.
 * <p>
 * The point of copying the data out instead of reading the live section is threading.
 * {@link net.minecraft.world.level.chunk.PalettedContainer} guards writes with
 * {@code acquire()}/{@code release()} and a {@code ThreadingDetector}; reading a palette off
 * the tick thread races with any block update. A snapshot is built once on the tick thread
 * (see {@link ChunkCollisionCache#prepare}) and is thereafter read-only, so any number of
 * threads may query it without locking.
 * <p>
 * Occupancy follows each state's <em>collision shape</em>, not {@code isSolid()}. That matters
 * because the query is box-vs-box: a slab really is half a cell tall, a carpet really is one
 * sixteenth, and a vehicle now rides at the height its wheels are actually resting at instead of
 * at the top of whatever cell the geometry happened to live in.
 * <p>
 * Snapshots are never mutated. Invalidation replaces the whole object.
 */
public abstract sealed class SectionCollision {

    public static final int SECTION_SIZE = 4096;

    /** No collision anywhere in this section. Shared; carries no per-section state. */
    public static final SectionCollision EMPTY = new Empty();

    /**
     * Game time this snapshot was last asked for, used for age-based eviction. Written from
     * whichever thread queried it; races are benign because the value is only a hint and an
     * {@code int} write cannot tear.
     */
    volatile int lastUseTick;

    /**
     * The block state colliding at a cell, or {@code null} if that cell holds nothing with a
     * collision shape. {@code localIndex} is {@code (y << 8) | (z << 4) | x} with each coordinate
     * in [0, 16).
     */
    @Nullable
    public abstract BlockState collisionAt(int localIndex);

    /**
     * The cell's collision boxes, packed cell-relative (see {@link #packBox}), or {@code null}
     * when the cell holds nothing. {@link #FULL_CUBE} for an ordinary whole block.
     */
    @Nullable
    abstract long[] boxesAt(int localIndex);

    /** True when this snapshot can never report a contact, so callers can skip it wholesale. */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    private boolean occupied(int localIndex) {
        long[] boxes = boxesAt(localIndex);
        return boxes != null && boxes.length > 0;
    }

    /**
     * Height of the tallest collision box in a cell, in blocks above the cell floor. {@code 1.0}
     * for a whole block, {@code 0.5} for a bottom slab, {@code 1.5} for a fence.
     * <p>
     * This is what "how high is the thing I am standing on?" means, and it is what
     * {@code PhysicsEngine.climb} steps up to. Zero when the cell holds nothing.
     */
    public double collisionTop(int localIndex) {
        long[] boxes = boxesAt(localIndex);
        if (boxes == null || boxes.length == 0) {
            return 0;
        }
        int top = 0;
        for (long box : boxes) {
            top = Math.max(top, rawMin(box, 1) + rawSize(box, 1));
        }
        return top / 16.0;
    }

    /** How this section was stored. For the debug overlay and for diagnosing memory use. */
    public Kind kind() {
        return this instanceof Uniform ? Kind.UNIFORM
                : this instanceof ByteIndexed ? Kind.BYTE_INDEXED
                : this instanceof ShortIndexed ? Kind.SHORT_INDEXED
                : Kind.EMPTY;
    }

    public enum Kind {
        EMPTY, UNIFORM, BYTE_INDEXED, SHORT_INDEXED
    }

    /** Approximate retained size in bytes, for the cache's memory budget. */
    public abstract int footprint();

    int mergedFootprint() {
        long[] boxes = merged;
        return boxes == null ? 0 : boxes.length * 8;
    }

    // ------------------------------------------------------------------------------------------
    // Box packing
    // ------------------------------------------------------------------------------------------

    /**
     * Boxes are packed one per {@code long} in sixteenths of a block: min in bits 0-29 and size in
     * bits 30-59, ten bits per axis. Sixteenths because that is the grid nearly every vanilla
     * collision shape is authored on; the few that are not round outward, which grows a box by at
     * most a thirty-second of a block and can therefore only ever add a contact the exact shape
     * would have had at a slightly different position — never remove one.
     * <p>
     * Ten bits covers a merged run of sixteen cells (256 sixteenths) with room to spare for shapes
     * that overhang their cell, like a fence at 24.
     */
    private static final int COORD_BITS = 10;
    private static final long COORD_MASK = (1L << COORD_BITS) - 1;
    private static final int MAX_COORD = (int) COORD_MASK;
    private static final int SIZE_SHIFT = 3 * COORD_BITS;

    /** Cell holds nothing that collides. */
    static final long[] NO_COLLISION = new long[0];

    /** An ordinary whole block. Shared, so the merge can identity-compare the common case. */
    static final long[] FULL_CUBE = {packBox(0, 0, 0, 16, 16, 16)};

    private static long packBox(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        return (long) minX
                | ((long) minY << COORD_BITS)
                | ((long) minZ << (2 * COORD_BITS))
                | ((long) sizeX << SIZE_SHIFT)
                | ((long) sizeY << (SIZE_SHIFT + COORD_BITS))
                | ((long) sizeZ << (SIZE_SHIFT + 2 * COORD_BITS));
    }

    private static int rawMin(long packed, int axis) {
        return (int) ((packed >>> (axis * COORD_BITS)) & COORD_MASK);
    }

    private static int rawSize(long packed, int axis) {
        return (int) ((packed >>> (SIZE_SHIFT + axis * COORD_BITS)) & COORD_MASK);
    }

    public static double boxMinX(long packed) { return rawMin(packed, 0) / 16.0; }
    public static double boxMinY(long packed) { return rawMin(packed, 1) / 16.0; }
    public static double boxMinZ(long packed) { return rawMin(packed, 2) / 16.0; }
    public static double boxSizeX(long packed) { return rawSize(packed, 0) / 16.0; }
    public static double boxSizeY(long packed) { return rawSize(packed, 1) / 16.0; }
    public static double boxSizeZ(long packed) { return rawSize(packed, 2) / 16.0; }

    /**
     * Collision boxes for a state, packed cell-relative, cached forever.
     * <p>
     * Block states are canonical and finite, so this converges after a few seconds of play and
     * every later snapshot resolves a shape with one map lookup.
     */
    private static final Map<BlockState, long[]> SHAPE_BOXES = new ConcurrentHashMap<>();

    static long[] boxesOf(BlockState state) {
        return SHAPE_BOXES.computeIfAbsent(state, SectionCollision::computeBoxes);
    }

    private static long[] computeBoxes(BlockState state) {
        if (state.isAir()) {
            return NO_COLLISION;
        }
        // A dynamic-shape block resolves its shape from the world — a moving piston reads its
        // block entity — so there is nothing state-derived to bake. Treating it as a whole block
        // is exactly what the old isSolid() test did for every one of them, so this is
        // conservative in the same direction the previous behaviour already was.
        if (state.getBlock().hasDynamicShape()) {
            return FULL_CUBE;
        }
        // For a cached state this is a field read: BlockStateBase.Cache precomputes the shape
        // against EmptyBlockGetter at BlockPos.ZERO, which is exactly what is being asked for.
        VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (shape.isEmpty()) {
            return NO_COLLISION;
        }
        if (Block.isShapeFullBlock(shape)) {
            return FULL_CUBE;
        }
        List<AABB> parts = shape.toAabbs();
        if (parts.isEmpty()) {
            return NO_COLLISION;
        }
        long[] boxes = new long[parts.size()];
        for (int i = 0; i < boxes.length; i++) {
            AABB part = parts.get(i);
            int minX = floorSixteenths(part.minX);
            int minY = floorSixteenths(part.minY);
            int minZ = floorSixteenths(part.minZ);
            boxes[i] = packBox(minX, minY, minZ,
                    Math.max(1, ceilSixteenths(part.maxX) - minX),
                    Math.max(1, ceilSixteenths(part.maxY) - minY),
                    Math.max(1, ceilSixteenths(part.maxZ) - minZ));
        }
        return boxes;
    }

    private static int floorSixteenths(double blocks) {
        return Mth.clamp(Mth.floor(blocks * 16), 0, MAX_COORD);
    }

    private static int ceilSixteenths(double blocks) {
        return Mth.clamp(Mth.ceil(blocks * 16), 0, MAX_COORD);
    }

    // ------------------------------------------------------------------------------------------
    // Merging
    // ------------------------------------------------------------------------------------------

    /**
     * Above this the merge gives up on shapes and falls back to whole cells, which the
     * checkerboard worst case bounds at exactly this many — so a snapshot's box list can never
     * cost more than it did before shapes existed. Only a section packed with unmergeable detail,
     * thousands of fences, reaches it, and there the exact shapes are not worth the memory.
     */
    private static final int MAX_BOXES = 2048;

    /**
     * Solid geometry greedy-merged into as few boxes as possible, packed section-relative in
     * sixteenths.
     * <p>
     * Built on first use and cached. Derived purely from immutable data, so the lazy init races
     * benignly — two threads may both compute it and one result is discarded.
     */
    public long[] collisionBoxes() {
        long[] boxes = merged;
        if (boxes == null) {
            boxes = isEmpty() ? NO_COLLISION : merge(false);
            merged = boxes;
        }
        return boxes;
    }

    private volatile long[] merged;

    /**
     * Three-axis greedy merge: extend a run along X, widen it along Z while whole rows match,
     * then raise it along Y while whole slabs match. Covers every occupied cell exactly once.
     * <p>
     * Two cells only merge when their shapes are identical, and a run only extends along an axis
     * the shape spans end to end — so a floor of slabs collapses to one flat box per plane, a row
     * of stairs collapses to two boxes running its length, and a fence post stays a post. Merging
     * a half-height shape along Y, or a fence post along X, would invent geometry that is not
     * there, which is the one thing a broad phase must never do.
     *
     * @param wholeCells treat every occupied cell as a full block, the pre-shape behaviour
     */
    private long[] merge(boolean wholeCells) {
        boolean[] used = new boolean[SECTION_SIZE];
        long[] out = new long[128];
        int count = 0;

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int start = cellIndex(x, y, z);
                    if (used[start] || !occupied(start)) {
                        continue;
                    }
                    long[] shape = wholeCells ? FULL_CUBE : boxesAt(start);

                    int width = 1;
                    if (spans(shape, 0)) {
                        while (x + width < 16 && matches(used, cellIndex(x + width, y, z), shape, wholeCells)) {
                            width++;
                        }
                    }
                    int depth = 1;
                    if (spans(shape, 2)) {
                        while (z + depth < 16 && rowMatches(used, x, y, z + depth, width, shape, wholeCells)) {
                            depth++;
                        }
                    }
                    int height = 1;
                    if (spans(shape, 1)) {
                        while (y + height < 16 && slabMatches(used, x, y + height, z, width, depth, shape, wholeCells)) {
                            height++;
                        }
                    }
                    for (int dy = 0; dy < height; dy++) {
                        for (int dz = 0; dz < depth; dz++) {
                            for (int dx = 0; dx < width; dx++) {
                                used[cellIndex(x + dx, y + dy, z + dz)] = true;
                            }
                        }
                    }

                    for (long box : shape) {
                        if (count == out.length) {
                            out = Arrays.copyOf(out, count * 2);
                        }
                        // A run only ever extends along an axis the shape spans, so on that axis
                        // the box already measures a full 16 and the extension is exact.
                        out[count++] = packBox(
                                x * 16 + rawMin(box, 0),
                                y * 16 + rawMin(box, 1),
                                z * 16 + rawMin(box, 2),
                                rawSize(box, 0) + (width - 1) * 16,
                                rawSize(box, 1) + (height - 1) * 16,
                                rawSize(box, 2) + (depth - 1) * 16);
                    }
                    if (count > MAX_BOXES && !wholeCells) {
                        return merge(true);
                    }
                }
            }
        }
        return count == out.length ? out : Arrays.copyOf(out, count);
    }

    /** True when every box in a shape runs the full width of its cell along {@code axis}. */
    private static boolean spans(long[] shape, int axis) {
        for (long box : shape) {
            if (rawMin(box, axis) != 0 || rawSize(box, axis) != 16) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(boolean[] used, int localIndex, long[] shape, boolean wholeCells) {
        if (used[localIndex] || !occupied(localIndex)) {
            return false;
        }
        return wholeCells || sameShape(boxesAt(localIndex), shape);
    }

    /**
     * Reference equality first, which hits whenever the two cells share a palette entry or are
     * both whole blocks. The content compare only runs for two different states that happen to
     * have the same shape, where it buys a merge that would otherwise be missed.
     */
    private static boolean sameShape(long[] a, long[] b) {
        return a == b || Arrays.equals(a, b);
    }

    private boolean rowMatches(boolean[] used, int x, int y, int z, int width, long[] shape, boolean wholeCells) {
        for (int dx = 0; dx < width; dx++) {
            if (!matches(used, cellIndex(x + dx, y, z), shape, wholeCells)) {
                return false;
            }
        }
        return true;
    }

    private boolean slabMatches(boolean[] used, int x, int y, int z, int width, int depth, long[] shape, boolean wholeCells) {
        for (int dz = 0; dz < depth; dz++) {
            if (!rowMatches(used, x, y, z + dz, width, shape, wholeCells)) {
                return false;
            }
        }
        return true;
    }

    public static int cellIndex(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    // ------------------------------------------------------------------------------------------
    // Snapshotting
    // ------------------------------------------------------------------------------------------

    /**
     * Copies a section's collision geometry out on the tick thread.
     * <p>
     * The two early exits carry the cost of this whole design. {@code hasOnlyAir()} is a
     * counter read, and {@code maybeHas} scans the section's <em>palette</em> — a handful of
     * entries — not its 4096 cells. A section whose palette contains nothing that collides
     * provably contains no collision, so air, water and foliage sections resolve to {@link #EMPTY}
     * without a single cell read and without allocating. Only sections that genuinely hold
     * geometry pay the full scan, and they pay it once.
     */
    public static SectionCollision snapshot(@Nullable LevelChunkSection section) {
        if (section == null || section.hasOnlyAir() || !section.maybeHas(SectionCollision::mayCollide)) {
            return EMPTY;
        }

        short[] index = new short[SECTION_SIZE];
        List<BlockState> palette = new ArrayList<>();
        List<long[]> paletteBoxes = new ArrayList<>();
        Reference2IntMap<BlockState> ids = new Reference2IntOpenHashMap<>();
        // Block states are canonical instances, so reference identity is a valid and cheap key.
        // A negative id memoises "this state does not collide", so water is resolved once.
        ids.defaultReturnValue(0);
        int cells = 0;

        for (int i = 0; i < SECTION_SIZE; i++) {
            BlockState state = section.getBlockState(i & 15, i >> 8, (i >> 4) & 15);
            if (state.isAir()) {
                // A field read, ahead of the hash lookup, for the cell most cells are.
                continue;
            }
            int id = ids.getInt(state);
            if (id < 0) {
                continue;
            }
            if (id == 0) {
                long[] boxes = boxesOf(state);
                if (boxes.length == 0) {
                    ids.put(state, -1);
                    continue;
                }
                palette.add(state);
                paletteBoxes.add(boxes);
                id = palette.size();
                ids.put(state, id);
            }
            cells++;
            index[i] = (short) id;
        }

        if (cells == 0) {
            // maybeHas is allowed to be conservative (a GlobalPalette always answers true, and
            // a palette can retain entries no cell uses any more), so this is reachable.
            return EMPTY;
        }
        long[][] boxTable = paletteBoxes.toArray(new long[0][]);
        if (cells == SECTION_SIZE && palette.size() == 1) {
            return new Uniform(palette.get(0), boxTable[0]);
        }
        BlockState[] states = palette.toArray(new BlockState[0]);
        if (states.length <= 255) {
            byte[] narrow = new byte[SECTION_SIZE];
            for (int i = 0; i < SECTION_SIZE; i++) {
                narrow[i] = (byte) index[i];
            }
            return new ByteIndexed(states, boxTable, narrow);
        }
        return new ShortIndexed(states, boxTable, index);
    }

    private static boolean mayCollide(BlockState state) {
        return boxesOf(state).length > 0;
    }

    private static final class Empty extends SectionCollision {

        @Override
        public BlockState collisionAt(int localIndex) {
            return null;
        }

        @Override
        long[] boxesAt(int localIndex) {
            return null;
        }

        @Override
        public int footprint() {
            return 0;
        }

    }

    /** Every cell is the same state — deep stone, a filled section of one material. */
    private static final class Uniform extends SectionCollision {

        private final BlockState state;
        private final long[] boxes;

        private Uniform(BlockState state, long[] boxes) {
            this.state = state;
            this.boxes = boxes;
        }

        @Override
        public BlockState collisionAt(int localIndex) {
            return state;
        }

        @Override
        long[] boxesAt(int localIndex) {
            return boxes;
        }

        @Override
        public int footprint() {
            return 16 + mergedFootprint();
        }

    }

    /** The common case: fewer than 256 distinct colliding states, one byte per cell. */
    private static final class ByteIndexed extends SectionCollision {

        private final BlockState[] palette;
        private final long[][] paletteBoxes;
        private final byte[] index;

        private ByteIndexed(BlockState[] palette, long[][] paletteBoxes, byte[] index) {
            this.palette = palette;
            this.paletteBoxes = paletteBoxes;
            this.index = index;
        }

        @Override
        public BlockState collisionAt(int localIndex) {
            int id = index[localIndex] & 0xFF;
            return id == 0 ? null : palette[id - 1];
        }

        @Override
        long[] boxesAt(int localIndex) {
            int id = index[localIndex] & 0xFF;
            return id == 0 ? null : paletteBoxes[id - 1];
        }

        @Override
        public int footprint() {
            return SECTION_SIZE + palette.length * 16 + mergedFootprint() + 32;
        }

    }

    /** Fallback for a section with 256+ distinct colliding states. Vanishingly rare. */
    private static final class ShortIndexed extends SectionCollision {

        private final BlockState[] palette;
        private final long[][] paletteBoxes;
        private final short[] index;

        private ShortIndexed(BlockState[] palette, long[][] paletteBoxes, short[] index) {
            this.palette = palette;
            this.paletteBoxes = paletteBoxes;
            this.index = index;
        }

        @Override
        public BlockState collisionAt(int localIndex) {
            int id = index[localIndex] & 0xFFFF;
            return id == 0 ? null : palette[id - 1];
        }

        @Override
        long[] boxesAt(int localIndex) {
            int id = index[localIndex] & 0xFFFF;
            return id == 0 ? null : paletteBoxes[id - 1];
        }

        @Override
        public int footprint() {
            return SECTION_SIZE * 2 + palette.length * 16 + mergedFootprint() + 32;
        }

    }

}
