package org.ywzj.vehicle.vehicle.collision;

import org.joml.Vector3f;

/**
 * Resolves a desired movement against a set of contact planes, so a hull slides instead of stopping.
 * <p>
 * Ported in spirit from Erin Catto's Box3D ({@code src/mover.c}, {@code b3SolvePlanes} and
 * {@code b3ClipVector}, MIT). Written fresh against this codebase's types; the algorithm — a
 * projected Gauss-Seidel relaxation over half-space constraints with clamped accumulated push — is
 * his.
 * <p>
 * <b>Why this replaces a scalar time of impact.</b> {@link SweptHull} answers "how far along this
 * exact direction may I go", which has one degree of freedom, so the only thing it can do about an
 * obstacle is stop. That is why a vehicle meeting a slope halts rather than riding up it, and why a
 * hull touching two surfaces at once is stuck at whichever it met first. A plane solver answers a
 * different question — "what is the nearest movement to the one I wanted that violates nothing" —
 * and its answer has three degrees of freedom, so the natural outcome against a slope is to slide
 * along it and against a corner is to settle into it.
 * <p>
 * <b>Push limits are the interesting knob.</b> A plane with an unbounded limit is a wall. A plane
 * with a small limit can only nudge, which is how geometry a vehicle is meant to ride over is
 * expressed as a property of the contact rather than as a special case in the movement code.
 * <p>
 * Allocation-free and instance-free; planes live in a caller-owned {@link Planes} buffer.
 */
public final class PlaneSolver {

    /** Matches the contact slop used elsewhere. Keeps a resting hull from jittering. */
    public static final float SLOP = 0.005f;

    /** Relaxation sweeps. Box3D uses 20; convergence is usually far quicker. */
    private static final int MAX_ITERATIONS = 20;

    private PlaneSolver() {}

    /**
     * A set of half-space constraints, stored flat and reused.
     * <p>
     * Each plane is {@code dot(normal, delta) + baseSeparation >= 0}, where {@code baseSeparation}
     * is the signed distance from the hull to the surface before any movement — negative when the
     * hull is already overlapping.
     */
    public static final class Planes {

        private static final int STRIDE = 6;

        private float[] data = new float[STRIDE * 16];
        private boolean[] clipVelocity = new boolean[16];
        private int count;

        public void clear() {
            count = 0;
        }

        public int size() {
            return count;
        }

        public boolean isEmpty() {
            return count == 0;
        }

        /**
         * @param nx,ny,nz        surface normal, pointing away from the surface toward the hull
         * @param baseSeparation  signed distance before movement; negative means already overlapping
         * @param pushLimit       most this plane may move the hull; {@link Float#MAX_VALUE} for a wall
         * @param clipVelocity    whether velocity into this plane should be removed afterwards
         */
        public void add(float nx, float ny, float nz, float baseSeparation, float pushLimit,
                        boolean clipVelocity) {
            if ((count + 1) * STRIDE > data.length) {
                float[] grown = new float[data.length * 2];
                System.arraycopy(data, 0, grown, 0, data.length);
                data = grown;
                boolean[] grownClip = new boolean[this.clipVelocity.length * 2];
                System.arraycopy(this.clipVelocity, 0, grownClip, 0, this.clipVelocity.length);
                this.clipVelocity = grownClip;
            }
            int base = count * STRIDE;
            data[base] = nx;
            data[base + 1] = ny;
            data[base + 2] = nz;
            data[base + 3] = baseSeparation;
            data[base + 4] = pushLimit;
            data[base + 5] = 0;
            this.clipVelocity[count] = clipVelocity;
            count++;
        }

        public float normalX(int i) {
            return data[i * STRIDE];
        }

        public float normalY(int i) {
            return data[i * STRIDE + 1];
        }

        public float normalZ(int i) {
            return data[i * STRIDE + 2];
        }

        public float baseSeparation(int i) {
            return data[i * STRIDE + 3];
        }

        /** How far this plane actually pushed during the last solve. Zero means it never engaged. */
        public float push(int i) {
            return data[i * STRIDE + 5];
        }

        private void setPush(int i, float push) {
            data[i * STRIDE + 5] = push;
        }

        private float pushLimit(int i) {
            return data[i * STRIDE + 4];
        }

    }

    /**
     * Finds the movement closest to {@code targetDelta} that violates no plane, writing it to
     * {@code out}. Also records each plane's accumulated push, which {@link #clipVector} needs.
     *
     * @return iterations used, for diagnostics
     */
    public static int solve(Planes planes, Vector3f targetDelta, Vector3f out) {
        for (int i = 0, n = planes.size(); i < n; i++) {
            planes.setPush(i, 0);
        }
        out.set(targetDelta);
        if (planes.isEmpty()) {
            return 0;
        }

        int iteration = 0;
        for (; iteration < MAX_ITERATIONS; iteration++) {
            float totalPush = 0;
            for (int i = 0, n = planes.size(); i < n; i++) {
                float nx = planes.normalX(i);
                float ny = planes.normalY(i);
                float nz = planes.normalZ(i);
                // Separation this plane would have after taking the movement worked out so far.
                // The slop lets a resting hull sit a hair inside without the solver fighting it.
                float separation = nx * out.x + ny * out.y + nz * out.z
                        + planes.baseSeparation(i) + SLOP;
                float push = -separation;

                // Clamping the accumulated push rather than the increment is what makes this a
                // proper inequality solver: a plane that over-pushed on one sweep can be walked
                // back by a later one, which is how several planes reach a consistent answer.
                float accumulated = planes.push(i);
                float clamped = Math.max(0, Math.min(accumulated + push, planes.pushLimit(i)));
                planes.setPush(i, clamped);
                push = clamped - accumulated;

                out.x += push * nx;
                out.y += push * ny;
                out.z += push * nz;
                totalPush += Math.abs(push);
            }
            if (totalPush < SLOP) {
                break;
            }
        }
        return iteration;
    }

    /**
     * Removes the component of {@code velocity} heading into any plane that actually pushed.
     * <p>
     * Kept separate from the position solve on purpose. Letting depenetration write velocity is
     * how a hull picks up speed it was never given — the mechanism behind every launch and hop in
     * this codebase's history. Planes that did not engage, and planes marked as not clipping, are
     * skipped, so a soft push-out cannot become momentum.
     */
    public static void clipVector(Planes planes, Vector3f velocity) {
        for (int i = 0, n = planes.size(); i < n; i++) {
            if (planes.push(i) == 0 || !planes.clipVelocity[i]) {
                continue;
            }
            float nx = planes.normalX(i);
            float ny = planes.normalY(i);
            float nz = planes.normalZ(i);
            float into = Math.min(0, velocity.x * nx + velocity.y * ny + velocity.z * nz);
            velocity.x -= into * nx;
            velocity.y -= into * ny;
            velocity.z -= into * nz;
        }
    }

}
