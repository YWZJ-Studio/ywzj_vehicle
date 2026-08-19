package org.ywzj.vehicle.vehicle.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.structure.OBB;

import java.util.Arrays;

/**
 * Stops a hull from stepping over geometry it should have hit using conservative advancement.
 * The hull is trimmed to its climb skirt, bisected until the last moment it did not overlap a box,
 * and rested against obstacles instead of teleporting past them; an embedded hull can still escape
 * what it started inside while being stopped by new geometry.
 */
public final class SweptHull {

    /** Bisection steps; six gives 1/64 of a block on a one-block step. */
    private static final int ITERATIONS = 6;

    /**
     * Depth of overlap treated as resolved rather than as a collision, in blocks;
     * only used to inset the cast hull sideways.
     */
    public static final float SLOP = 0.005f;

    /**
     * Fraction of the step held back on impact so the hull stops short of touching rather than
     * landing exactly on the boundary.
     */
    private static final double SKIN = 0.01;

    /** Why timeOfImpact returned what it did. */
    public enum Outcome {

        /** No boxes near the swept path; the test was skipped. */
        NO_BOXES,
        /** Movement handed to the cast was zero so nothing was tested, distinct from NO_BOXES. */
        NO_MOTION,
        /** The whole step is free. */
        CLEAR,
        /**
         * The hull overlapped something before the step started; motion within or out of those boxes
         * is free, but entering new geometry clips.
         */
        ALREADY_INSIDE,
        /** The step was shortened to stop against geometry. */
        CLIPPED

    }

    /**
     * Scratch space for asking a sweep what it did; allocate one per traced vehicle and reuse.
     */
    public static final class Probe {

        public Outcome outcome = Outcome.NO_BOXES;

        /** Number of boxes offered to the sweep. */
        public int boxes;

        /** Box that stopped the step, or one the hull started inside. */
        @Nullable
        public AABB blocker;

        public double toi = 1.0;

        /** Deepest overlap found by the last measurePenetration call. */
        public double penetration;

        /** Direction and depth of that overlap, pointing out of the hull. */
        public final Vector3f penetrationAxis = new Vector3f();

        /** Box the hull is deepest inside. */
        @Nullable
        public AABB penetrator;

        public void reset() {
            outcome = Outcome.NO_BOXES;
            boxes = 0;
            blocker = null;
            toi = 1.0;
            penetration = 0;
            penetrationAxis.zero();
            penetrator = null;
        }

    }

    /**
     * Narrows the world's boxes to the handful near one substep; owned by the caller, not thread-safe.
     */
    public static final class Broadphase {

        private final BoxBuffer nearby = new BoxBuffer();
        private final Matrix3f basisScratch = new Matrix3f();
        private SectionSource source;

        /**
         * Points this broadphase at a section source; sections not held read as empty.
         */
        public void init(SectionSource source) {
            this.source = source;
        }

        /**
         * Boxes that could matter to a cast along the movement; returned buffer is scratch.
         */
        public BoxBuffer near(OBB hull, double moveX, double moveY, double moveZ) {
            Vector3f centre = hull.center();
            Vector3f extents = hull.extents();
            // Project extents onto world axes through rotation; not the circumscribed sphere, which
            // defeats the narrowing for the large hulls it exists for.
            Matrix3f m = hull.rotation().get(basisScratch);
            double reachX = Math.abs(m.m00()) * extents.x + Math.abs(m.m10()) * extents.y
                    + Math.abs(m.m20()) * extents.z + MARGIN;
            double reachY = Math.abs(m.m01()) * extents.x + Math.abs(m.m11()) * extents.y
                    + Math.abs(m.m21()) * extents.z + MARGIN;
            double reachZ = Math.abs(m.m02()) * extents.x + Math.abs(m.m12()) * extents.y
                    + Math.abs(m.m22()) * extents.z + MARGIN;
            double minX = centre.x + Math.min(0, moveX) - reachX;
            double minY = centre.y + Math.min(0, moveY) - reachY;
            double minZ = centre.z + Math.min(0, moveZ) - reachZ;
            double maxX = centre.x + Math.max(0, moveX) + reachX;
            double maxY = centre.y + Math.max(0, moveY) + reachY;
            double maxZ = centre.z + Math.max(0, moveZ) + reachZ;
            nearby.clear();
            if (source != null) {
                ChunkCollisionCache.collectBoxes(source, minX, minY, minZ, maxX, maxY, maxZ, nearby);
            }
            return nearby;
        }

        /** Slack on the query bound so rounding never drops a box the cast would hit. */
        private static final double MARGIN = 0.5;

    }

    /**
     * Thinnest the trimmed hull is allowed to get; the shortfall above the skirt plane is made up above it.
     */
    private static final float MIN_HALF_HEIGHT = 0.05f;

    private SweptHull() {}

    /**
     * Fills dest with source minus its climb skirt; this trimmed hull is swept against the world,
     * with the underside raised to the skirt plane.
     *
     * @param skirt local Y of the skirt plane.
     * @param dest scratch hull, overwritten; keep one per vehicle.
     */
    public static OBB climbHull(OBB source, double skirt, OBB dest) {
        float extentY = source.extents().y;
        // Skirt is measured in the hull's frame from its centre, so band height is how far it
        // sits above the underside.
        float band = (float) skirt + extentY;
        if (band <= 0) {
            dest.center().set(source.center());
            dest.extents().set(source.extents());
            dest.rotation().set(source.rotation());
            return dest;
        }
        // Underside at skirt; if hull is shorter than ride band, make up shortfall above to keep
        // nothing below the skirt able to block. Clamp top to keep cast box inside the real hull.
        float bottom = Math.min(-extentY + band, extentY - 2 * MIN_HALF_HEIGHT);
        float top = Math.max(extentY, bottom + 2 * MIN_HALF_HEIGHT);
        // Inset sideways by contact slop so surfaces already resting against the hull are invisible.
        float inset = Math.min(SLOP,
                Math.min(source.extents().x, source.extents().z) * 0.5f);
        // Along the hull's own up axis; a rolled hull's skirt tilts with it.
        dest.rotation().set(source.rotation());
        dest.extents().set(source.extents().x - inset, (top - bottom) * 0.5f,
                source.extents().z - inset);
        dest.center().set(0, (top + bottom) * 0.5f, 0).rotate(source.rotation())
                .add(source.center());
        return dest;
    }

    /**
     * How much of the movement the hull may take before overlapping something.
     *
     * @return a fraction in [0, 1]; 1 when the whole step is clear.
     */
    public static double timeOfImpact(OBB obb, BoxBuffer boxes, Vec3 movement) {
        return timeOfImpact(obb, boxes, movement, null);
    }

    /**
     * As timeOfImpact, recording into probe why the answer came out the way it did.
     */
    public static double timeOfImpact(OBB obb, BoxBuffer boxes, Vec3 movement,
                                      @Nullable Probe probe) {
        return timeOfImpact(obb, boxes, movement.x, movement.y, movement.z, probe);
    }

    /**
     * As above, without a Vec3; builds a throwaway frame for a single cast.
     */
    public static double timeOfImpact(OBB obb, BoxBuffer boxes,
                                      double moveX, double moveY, double moveZ,
                                      @Nullable Probe probe) {
        return timeOfImpact(obb, boxes, moveX, moveY, moveZ, probe,
                new OBB.SatFrame().set(obb.rotation()));
    }

    /**
     * As above, against a caller-prepared SatFrame; must match obb.rotation().
     */
    public static double timeOfImpact(OBB obb, BoxBuffer boxes,
                                      double moveX, double moveY, double moveZ,
                                      @Nullable Probe probe, OBB.SatFrame frame) {
        if (probe != null) {
            probe.reset();
            probe.boxes = boxes.size();
        }
        if (boxes.isEmpty()) {
            return 1.0;
        }
        if (moveX * moveX + moveY * moveY + moveZ * moveZ < 1.0e-12) {
            if (probe != null) {
                probe.outcome = Outcome.NO_MOTION;
            }
            return 1.0;
        }
        // The hull's centre is never moved; the probe position is just arithmetic.
        Vector3f centre = obb.center();
        double ox = centre.x;
        double oy = centre.y;
        double oz = centre.z;
        Vector3f extents = obb.extents();

        int atEnd = firstOverlap(frame, extents, boxes, ox + moveX, oy + moveY, oz + moveZ);
        if (atEnd < 0) {
            if (probe != null) {
                probe.outcome = Outcome.CLEAR;
            }
            return 1.0;
        }
        // Already inside something at the start. Motion within or out of those boxes is free,
        // new geometry still clips.
        int atStart = firstOverlap(frame, extents, boxes, ox, oy, oz);
        if (atStart >= 0) {
            // Excuse every box the hull starts inside, not just the first found.
            int[] ignore = allOverlaps(frame, extents, boxes, ox, oy, oz);
            if (probe != null) {
                probe.outcome = Outcome.ALREADY_INSIDE;
                probe.blocker = boxes.get(atStart);
            }
            if (firstOverlapExcluding(frame, extents, boxes,
                    ox + moveX, oy + moveY, oz + moveZ, ignore) < 0) {
                return 1.0;
            }
            return bisect(frame, extents, boxes, ox, oy, oz, moveX, moveY, moveZ, ignore, probe);
        }
        return bisect(frame, extents, boxes, ox, oy, oz, moveX, moveY, moveZ, null, probe);
    }

    /**
     * Largest fraction of movement that lands the hull in nothing it is not already in.
     *
     * @param ignore indices of boxes the hull started inside; overlaps with those do not count as a hit.
     */
    private static double bisect(OBB.SatFrame frame, Vector3f extents, BoxBuffer boxes,
                                 double ox, double oy, double oz,
                                 double moveX, double moveY, double moveZ,
                                 int @Nullable [] ignore, @Nullable Probe probe) {
        double clear = 0.0;
        double blocked = 1.0;
        int blocker = firstOverlapExcluding(frame, extents, boxes,
                ox + moveX, oy + moveY, oz + moveZ, ignore);
        for (int i = 0; i < ITERATIONS; i++) {
            double mid = (clear + blocked) * 0.5;
            int hit = firstOverlapExcluding(frame, extents, boxes,
                    ox + moveX * mid, oy + moveY * mid, oz + moveZ * mid, ignore);
            if (hit >= 0) {
                blocked = mid;
                blocker = hit;
            } else {
                clear = mid;
            }
        }
        double toi = Math.max(0.0, clear - SKIN);
        if (probe != null) {
            // Embedded hull keeps ALREADY_INSIDE verdict but reports the actual allowed fraction.
            if (probe.outcome != Outcome.ALREADY_INSIDE) {
                probe.outcome = Outcome.CLIPPED;
            }
            if (blocker >= 0) {
                probe.blocker = boxes.get(blocker);
            }
            probe.toi = toi;
        }
        return toi;
    }

    /**
     * Deepest overlap between the hull and any boxes, written into probe; purely a measurement.
     */
    public static void measurePenetration(OBB obb, BoxBuffer boxes, Probe probe) {
        probe.penetration = 0;
        probe.penetrationAxis.zero();
        probe.penetrator = null;
        OBB.SatFrame frame = new OBB.SatFrame().set(obb.rotation());
        Vector3f centre = obb.center();
        for (int i = 0, size = boxes.size(); i < size; i++) {
            // Cheap rejection first; most boxes are nowhere near the hull.
            if (!OBB.intersectsBox(frame, centre.x, centre.y, centre.z, obb.extents(),
                    boxes.minX(i), boxes.minY(i), boxes.minZ(i),
                    boxes.maxX(i), boxes.maxY(i), boxes.maxZ(i))) {
                continue;
            }
            // Only on a genuinely overlapping box is materializing one worth it.
            AABB box = boxes.get(i);
            Vector3f mtv = obb.calculateMTV(box);
            float depth = mtv.length();
            if (depth > probe.penetration) {
                probe.penetration = depth;
                probe.penetrationAxis.set(mtv);
                probe.penetrator = box;
            }
        }
    }

    /**
     * As firstOverlap, skipping a set of box indices; ignore holds the boxes the hull began inside.
     */
    private static int firstOverlapExcluding(OBB.SatFrame frame, Vector3f extents, BoxBuffer boxes,
                                             double x, double y, double z, int @Nullable [] ignore) {
        if (ignore == null) {
            return firstOverlap(frame, extents, boxes, x, y, z);
        }
        float cx = (float) x;
        float cy = (float) y;
        float cz = (float) z;
        for (int i = 0, size = boxes.size(); i < size; i++) {
            if (containsIndex(ignore, i)) {
                continue;
            }
            if (OBB.intersectsBox(frame, cx, cy, cz, extents,
                    boxes.minX(i), boxes.minY(i), boxes.minZ(i),
                    boxes.maxX(i), boxes.maxY(i), boxes.maxZ(i))) {
                return i;
            }
        }
        return -1;
    }

    /** Whether value is in indices; indices are the handful of boxes a hull starts embedded in. */
    private static boolean containsIndex(int[] indices, int value) {
        for (int index : indices) {
            if (index == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every box the hull overlaps with its centre at the given point; array is tiny.
     */
    private static int[] allOverlaps(OBB.SatFrame frame, Vector3f extents, BoxBuffer boxes,
                                     double x, double y, double z) {
        float cx = (float) x;
        float cy = (float) y;
        float cz = (float) z;
        int[] found = new int[4];
        int count = 0;
        for (int i = 0, size = boxes.size(); i < size; i++) {
            if (OBB.intersectsBox(frame, cx, cy, cz, extents,
                    boxes.minX(i), boxes.minY(i), boxes.minZ(i),
                    boxes.maxX(i), boxes.maxY(i), boxes.maxZ(i))) {
                if (count == found.length) {
                    found = Arrays.copyOf(found, count * 2);
                }
                found[count++] = i;
            }
        }
        return count == found.length ? found : Arrays.copyOf(found, count);
    }

    /**
     * Index of the first box the hull overlaps at its current pose, or -1.
     */
    public static int firstOverlappingBox(OBB obb, BoxBuffer boxes) {
        Vector3f centre = obb.center();
        return firstOverlap(new OBB.SatFrame().set(obb.rotation()), obb.extents(), boxes,
                centre.x, centre.y, centre.z);
    }

    /** Index of the first box overlapping the hull at the given centre, or -1. */
    private static int firstOverlap(OBB.SatFrame frame, Vector3f extents, BoxBuffer boxes,
                                    double x, double y, double z) {
        float cx = (float) x;
        float cy = (float) y;
        float cz = (float) z;
        for (int i = 0, size = boxes.size(); i < size; i++) {
            if (OBB.intersectsBox(frame, cx, cy, cz, extents,
                    boxes.minX(i), boxes.minY(i), boxes.minZ(i),
                    boxes.maxX(i), boxes.maxY(i), boxes.maxZ(i))) {
                return i;
            }
        }
        return -1;
    }

}
