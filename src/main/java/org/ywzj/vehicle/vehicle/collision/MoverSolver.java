package org.ywzj.vehicle.vehicle.collision;

import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.structure.OBB;

/**
 * Moves a hull as far toward a target as the world allows, sliding rather than stopping.
 * <p>
 * The loop is Box3D's character mover ({@code samples/mover.cpp}, MIT), written fresh:
 * <pre>
 *   repeat:
 *       planes = collide(trial pose)          gather every contact plane where the hull now is
 *       delta  = solvePlanes(remaining)       redirect the motion, do not truncate it
 *       delta *= castHull(trial, delta)       then cast along the corrected direction
 *       trial += delta
 * </pre>
 * <p>
 * <b>Why redirect then cast, rather than cast alone.</b> A cast returns one number, so the only
 * thing it can say about an obstacle is "stop here". Redirecting first turns a slope into an
 * instruction to go up-and-along, and a corner into an instruction to settle, and only then does
 * the cast enforce that the corrected motion cannot pass through anything. Casting alone is what
 * left a vehicle wedged against a hill with its throttle open; solving alone would let a fast hull
 * skip a thin wall between one plane gather and the next. The pair is what Box3D uses and each
 * half covers the other's failure.
 * <p>
 * Re-gathering planes each iteration is what lets a hull that slides into something new during the
 * step react within the same step instead of a tick later.
 */
public final class MoverSolver {

    /** Collide-solve-cast rounds per movement. Box3D uses five. */
    private static final int MAX_ITERATIONS = 5;

    /** Remaining movement below which the loop stops early, in blocks. */
    private static final double TOLERANCE = 0.001;

    /**
     * Box count past which building a hierarchy beats scanning. Measured: at 100 boxes a tree query
     * is 0.9x a linear scan, at 1000 it is 8.7x, at 10000 it is 6.4x.
     */
    private static final int TREE_THRESHOLD = 128;

    /** Scratch owned by one vehicle, so a move allocates nothing. */
    public static final class Workspace {

        public final PlaneSolver.Planes planes = new PlaneSolver.Planes();
        /**
         * Broad phase over the tick's boxes, built once and queried per iteration.
         * <p>
         * The loop below walks the box set up to thirteen times a tick — five plane gathers and up
         * to eight overlap tests inside each cast. For a car over a handful of boxes that is
         * nothing; for a hull whose bound covers hundreds it is the dominant cost, and all but a
         * few of those boxes are nowhere near it. Built only past {@link #TREE_THRESHOLD}, because
         * below it the tree costs more to build than the scan costs to run.
         */
        final DynamicTree tree = new DynamicTree();
        final BoxBuffer nearby = new BoxBuffer();
        boolean usingTree;
        final OBB trial = new OBB(new Vector3f(), new Vector3f(), new org.joml.Quaternionf());
        final OBB castHull = new OBB(new Vector3f(), new Vector3f(), new org.joml.Quaternionf());
        final Vector3f target = new Vector3f();
        final Vector3f delta = new Vector3f();

        /** Rounds actually used by the last move, for diagnostics. */
        public int iterations;

        /** Whether the last move was shortened by a wall rather than completing freely. */
        public boolean blocked;

    }

    private MoverSolver() {}

    /**
     * Resolves {@code desired} against the world.
     *
     * @param hull          the hull to move; never mutated
     * @param rideHeight    box tops within this of the hull's underside are ridden over, not blocked
     * @param ridePushLimit how far such geometry may displace the hull
     * @param out           the movement actually taken
     */
    public static void move(OBB hull, BoxBuffer boxes,
                            double desiredX, double desiredY, double desiredZ,
                            double rideHeight, float ridePushLimit,
                            Workspace work, Vector3f out) {
        out.zero();
        work.iterations = 0;
        work.blocked = false;
        if (boxes.isEmpty()) {
            out.set((float) desiredX, (float) desiredY, (float) desiredZ);
            return;
        }

        Vector3f centre = hull.center();
        float trialX = centre.x;
        float trialY = centre.y;
        float trialZ = centre.z;

        // The cast hull has the ride band trimmed off its underside. Planes are still gathered
        // with the full hull, so rideable geometry produces a soft plane that stops the vehicle
        // burying itself in a step — but it must not stop the cast, or the vehicle is wedged
        // against every kerb it is supposed to drive over. Casting the full hull was exactly that
        // bug: the planes said "ride over this" and the cast said "you shall not pass".
        SweptHull.climbHull(hull, -hull.extents().y + rideHeight, work.castHull);
        float castLift = work.castHull.center().y - centre.y;

        double remainingX = desiredX;
        double remainingY = desiredY;
        double remainingZ = desiredZ;

        work.usingTree = boxes.size() >= TREE_THRESHOLD;
        if (work.usingTree) {
            work.tree.build(boxes);
        }
        // How far around the trial pose boxes can still matter: the hull's own reach, the step it
        // may take, and the speculative margin that lets a plane exist before contact.
        double reach = hull.extents().length()
                + Math.sqrt(desiredX * desiredX + desiredY * desiredY + desiredZ * desiredZ)
                + HullPlanes.SPECULATIVE_MARGIN + 1.0;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            work.iterations = iteration + 1;

            BoxBuffer active = boxes;
            if (work.usingTree) {
                work.nearby.clear();
                work.tree.query(boxes, trialX - reach, trialY - reach, trialZ - reach,
                        trialX + reach, trialY + reach, trialZ + reach, work.nearby);
                active = work.nearby;
            }

            work.planes.clear();
            HullPlanes.collect(hull, active, trialX, trialY, trialZ,
                    rideHeight, ridePushLimit, work.planes);

            work.target.set((float) remainingX, (float) remainingY, (float) remainingZ);
            PlaneSolver.solve(work.planes, work.target, work.delta);

            // Cast the corrected direction, with the trimmed hull placed at the trial position.
            work.castHull.center().set(trialX, trialY + castLift, trialZ);
            double fraction = SweptHull.timeOfImpact(work.castHull, active,
                    work.delta.x, work.delta.y, work.delta.z, null);
            if (fraction < 1.0) {
                work.blocked = true;
            }

            double stepX = work.delta.x * fraction;
            double stepY = work.delta.y * fraction;
            double stepZ = work.delta.z * fraction;

            trialX += (float) stepX;
            trialY += (float) stepY;
            trialZ += (float) stepZ;
            out.add((float) stepX, (float) stepY, (float) stepZ);

            remainingX -= stepX;
            remainingY -= stepY;
            remainingZ -= stepZ;

            if (stepX * stepX + stepY * stepY + stepZ * stepZ < TOLERANCE * TOLERANCE) {
                break;
            }
        }
    }

    /**
     * Removes velocity heading into anything the last {@link #move} was actually pushed by.
     * <p>
     * Must be called with the same workspace and no intervening move. Planes that never engaged,
     * and rideable geometry, are skipped — so being lifted over a step cannot become forward or
     * upward momentum, which is the mechanism behind every launch in this codebase's history.
     */
    public static void clipVelocity(Workspace work, Vector3f velocity) {
        PlaneSolver.clipVector(work.planes, velocity);
    }

}
