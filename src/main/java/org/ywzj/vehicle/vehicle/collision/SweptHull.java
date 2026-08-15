package org.ywzj.vehicle.vehicle.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Intersectionf;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.structure.OBB;

import java.util.List;

/**
 * Stops a hull from stepping over geometry it should have hit.
 * <p>
 * Contacts are sampled once per tick, at the pose the vehicle starts it in, and
 * {@code AbstractVehicle.move} is a bare {@code setPos} with no collision of its own. So a vehicle
 * displaced further in one tick than a wall is thick lands on the far side having generated no
 * contact anywhere: it does not hit the wall, it teleports past it. At a block per tick a vehicle
 * can miss a one-block wall entirely, and hollow stairs — thin shells with air behind — are the
 * worst case, because a hull that ends up inside one is then surrounded by contacts pointing in
 * contradictory directions and jams.
 * <p>
 * Conservative advancement fixes it without a swept SAT: take the step, ask whether the hull ends
 * up overlapping, and if it does, bisect for the last moment it did not. Cost is one OBB test per
 * tick in the common case where nothing is in the way, and a handful of extra tests only on the
 * tick something is actually hit.
 * <p>
 * This is a <em>backstop</em>, not the collision response. It guarantees the hull never ends a tick
 * on the far side of something solid; deciding what the velocity should do about that is still
 * {@code PhysicsEngine}'s job, and it gets to make that decision next tick with real contacts
 * because the hull is now resting against the obstacle instead of inside it.
 * <p>
 * <b>Known blind spot, deliberately measurable.</b> The guarantee above is void whenever the hull
 * already overlaps something as the step begins — see {@link Outcome#ALREADY_INSIDE}. That is the
 * one case where advancing into a solid beats welding the vehicle in place, but it means the
 * backstop switches itself off exactly where overlap is routine. Pass a {@link Probe} to find out
 * how often that is happening in practice rather than reasoning about it.
 */
public final class SweptHull {

    /** Bisection steps. Six gives 1/64 of a block on a one-block step — well under contact slop. */
    private static final int ITERATIONS = 6;

    /**
     * Fraction of the step held back on impact, so the hull stops a hair short rather than exactly
     * touching. Landing exactly on the surface leaves contact generation right on the boundary,
     * which is the sort of knife-edge that has bitten this code repeatedly.
     */
    private static final double SKIN = 0.01;

    /** Why {@link #timeOfImpact} returned what it did. */
    public enum Outcome {

        /** Nothing near the swept path; the test was skipped. */
        NO_BOXES,
        /** The whole step is free. */
        CLEAR,
        /**
         * The hull overlapped something before the step even started, so the sweep gave up and
         * allowed the full movement. No tunnelling guarantee holds on a step that reports this.
         */
        ALREADY_INSIDE,
        /** The step was shortened to stop against something. */
        CLIPPED

    }

    /**
     * Scratch space for asking a sweep what it did, filled only when one is supplied. Diagnostics
     * only — allocate one per traced vehicle and reuse it; the untraced path passes null and pays
     * nothing.
     */
    public static final class Probe {

        public Outcome outcome = Outcome.NO_BOXES;

        /** Boxes offered to the sweep. */
        public int boxes;

        /** Whichever box stopped the step, or the one the hull started inside. */
        @Nullable
        public AABB blocker;

        public double toi = 1.0;

        /** Deepest overlap found by the last {@link #measurePenetration} call. */
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

    /** Thinnest the trimmed hull is allowed to get, so a deep skirt cannot erase it entirely. */
    private static final float MIN_HALF_HEIGHT = 0.05f;

    private SweptHull() {}

    /**
     * Fills {@code dest} with {@code source} minus its climb skirt — the same box with its
     * underside raised to the skirt plane — and returns it.
     * <p>
     * <b>This, not the full hull, is what may be swept against the world.</b> Everything below the
     * skirt is geometry the vehicle is designed to drive over: the skirt is exactly the band the
     * contact stage ignores so that steps can be climbed. Sweeping the full hull therefore treats
     * the ground the vehicle is standing on as an obstacle, which breaks the backstop twice over —
     * every step begins overlapping, so {@link Outcome#ALREADY_INSIDE} disables the clip whenever
     * the vehicle is on the ground, and any step tall enough to reach the underside clips the
     * vehicle to a standstill instead of letting it climb. Both were visible in a play-test
     * capture: 2% of substeps {@code CLEAR}, 34% {@code ALREADY_INSIDE}, 61% {@code CLIPPED} at a
     * mean taken step of 0.06 blocks.
     *
     * @param skirt local Y of the skirt plane, i.e. {@code VehicleCubeOBB.climbSkirt()}
     * @param dest  scratch hull, overwritten; keep one per vehicle
     */
    public static OBB climbHull(OBB source, double skirt, OBB dest) {
        float extentY = source.extents().y;
        // Skirt is measured in the hull's own frame from its centre, so the band's height is how
        // far it sits above the underside.
        float band = (float) skirt + extentY;
        float halfTrim = Math.min(Math.max(band, 0) * 0.5f, extentY - MIN_HALF_HEIGHT);
        if (halfTrim <= 0) {
            dest.center().set(source.center());
            dest.extents().set(source.extents());
            dest.rotation().set(source.rotation());
            return dest;
        }
        // Raising the underside by the band means shrinking the half-height by half of it and
        // moving the centre up by the same amount, along the hull's own up axis rather than the
        // world's — a rolled vehicle's skirt tilts with it.
        dest.rotation().set(source.rotation());
        dest.extents().set(source.extents().x, extentY - halfTrim, source.extents().z);
        dest.center().set(0, halfTrim, 0).rotate(source.rotation()).add(source.center());
        return dest;
    }

    /**
     * How much of {@code movement} the hull may take before it would overlap something.
     *
     * @param boxes world boxes near the swept path; may be empty
     * @return a fraction of {@code movement} in [0, 1], 1 when the whole step is clear
     */
    public static double timeOfImpact(OBB obb, BoxBuffer boxes, Vec3 movement) {
        return timeOfImpact(obb, boxes, movement, null);
    }

    /**
     * As {@link #timeOfImpact(OBB, List, Vec3)}, recording into {@code probe} why the answer came
     * out the way it did.
     */
    public static double timeOfImpact(OBB obb, BoxBuffer boxes, Vec3 movement,
                                      @Nullable Probe probe) {
        return timeOfImpact(obb, boxes, movement.x, movement.y, movement.z, probe);
    }

    /**
     * As above, without a {@link Vec3}. The mover loop calls this several times per step, and
     * keeping Minecraft's types out of it makes the whole path testable outside the game.
     */
    public static double timeOfImpact(OBB obb, BoxBuffer boxes,
                                      double moveX, double moveY, double moveZ,
                                      @Nullable Probe probe) {
        if (probe != null) {
            probe.reset();
            probe.boxes = boxes.size();
        }
        if (boxes.isEmpty()
                || moveX * moveX + moveY * moveY + moveZ * moveZ < 1.0e-12) {
            return 1.0;
        }
        // The hull's own centre is never moved. Earlier versions displaced it and restored it in a
        // finally block, which worked but left a shared OBB briefly holding a pose no caller had
        // asked for — a hazard once physics runs off the tick thread. The probe position is just
        // arithmetic; there is nothing to mutate.
        Vector3f centre = obb.center();
        double ox = centre.x;
        double oy = centre.y;
        double oz = centre.z;
        Matrix3f basis = obb.rotation().get(new Matrix3f());
        Vector3f extents = obb.extents();

        int atEnd = firstOverlap(basis, extents, boxes, ox + moveX, oy + moveY, oz + moveZ);
        if (atEnd < 0) {
            if (probe != null) {
                probe.outcome = Outcome.CLEAR;
            }
            return 1.0;
        }
        // Already inside something at the start of the step. Refusing to move would weld the
        // vehicle in place forever; let it move and let depenetration deal with the overlap,
        // which is the one case where advancing into a solid is the lesser evil.
        int atStart = firstOverlap(basis, extents, boxes, ox, oy, oz);
        if (atStart >= 0) {
            if (probe != null) {
                probe.outcome = Outcome.ALREADY_INSIDE;
                probe.blocker = boxes.get(atStart);
            }
            return 1.0;
        }
        double clear = 0.0;
        double blocked = 1.0;
        int blocker = atEnd;
        for (int i = 0; i < ITERATIONS; i++) {
            double mid = (clear + blocked) * 0.5;
            int hit = firstOverlap(basis, extents, boxes,
                    ox + moveX * mid, oy + moveY * mid, oz + moveZ * mid);
            if (hit >= 0) {
                blocked = mid;
                blocker = hit;
            } else {
                clear = mid;
            }
        }
        double toi = Math.max(0.0, clear - SKIN);
        if (probe != null) {
            probe.outcome = Outcome.CLIPPED;
            probe.blocker = boxes.get(blocker);
            probe.toi = toi;
        }
        return toi;
    }

    /**
     * Deepest overlap between the hull where it now stands and any of {@code boxes}, written into
     * {@code probe}. Purely a measurement — nothing acts on it — so it is only ever called for a
     * vehicle being traced.
     */
    public static void measurePenetration(OBB obb, BoxBuffer boxes, Probe probe) {
        probe.penetration = 0;
        probe.penetrationAxis.zero();
        probe.penetrator = null;
        Matrix3f basis = obb.rotation().get(new Matrix3f());
        Vector3f centre = obb.center();
        for (int i = 0, size = boxes.size(); i < size; i++) {
            // Cheap rejection first: the MTV routine allocates a vector per separating axis, and
            // over a swept path most boxes are nowhere near the hull.
            if (!OBB.intersectsBox(basis, centre.x, centre.y, centre.z, obb.extents(),
                    boxes.minX(i), boxes.minY(i), boxes.minZ(i),
                    boxes.maxX(i), boxes.maxY(i), boxes.maxZ(i))) {
                continue;
            }
            // Only here, on a box that is genuinely overlapping, is materialising one worth it —
            // and this whole method runs for traced vehicles only.
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

    /** Index of the first box the hull overlaps with its centre at the given point, or -1. */
    private static int firstOverlap(Matrix3f basis, Vector3f extents, BoxBuffer boxes,
                                    double x, double y, double z) {
        float cx = (float) x;
        float cy = (float) y;
        float cz = (float) z;
        for (int i = 0, size = boxes.size(); i < size; i++) {
            if (OBB.intersectsBox(basis, cx, cy, cz, extents,
                    boxes.minX(i), boxes.minY(i), boxes.minZ(i),
                    boxes.maxX(i), boxes.maxY(i), boxes.maxZ(i))) {
                return i;
            }
        }
        return -1;
    }

}
