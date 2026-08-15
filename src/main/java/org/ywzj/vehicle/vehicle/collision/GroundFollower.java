package org.ywzj.vehicle.vehicle.collision;

/**
 * Holds a hull at a rest height above the ground with a soft spring, instead of teleporting it there.
 * <p>
 * The "pogo stick" from Erin Catto's Box3D character mover ({@code samples/mover.cpp}, MIT),
 * written fresh here. A ray probes downward, and the difference between the measured distance and
 * the rest length drives a damped spring integrated implicitly, so it is unconditionally stable at
 * any stiffness — no explicit-Euler blow-up, no per-vehicle tuning to keep it from exploding.
 * <p>
 * <b>What this replaces.</b> Three separate mechanisms in {@code PhysicsEngine} are all bad
 * approximations of this one constraint: {@code climb()}'s step-up teleport, the
 * {@code SUPPORT_LIFT} given to a buried hull, and the flat {@code CENTRE_KICK}. Each moves the
 * vehicle upward on its own schedule with its own threshold, and between them they have produced a
 * launch on a fixed cadence, a permanent hop, and a deadlock. A spring has none of those failure
 * modes: ground rising underneath simply raises the rest position, so a staircase lifts the hull
 * continuously. That <em>is</em> "blocky terrain as a slope", obtained from geometry rather than
 * from a special case.
 * <p>
 * <b>It writes position, never velocity.</b> Box3D adds the pogo velocity to the target position
 * and deliberately keeps it out of the body's velocity. That discipline is the whole reason a
 * spring cannot fling: every launch in this codebase's history came from a depenetration mechanism
 * that was allowed to leave momentum behind. {@link #step} returns a displacement, and the caller
 * must not integrate it into velocity.
 */
public final class GroundFollower {

    /** Spring frequency. Higher tracks terrain more tightly and rides more harshly. */
    public float hertz = 4.0f;

    /** Fraction of critical damping. Below 1 overshoots; 0.7 settles quickly without visible bounce. */
    public float dampingRatio = 0.7f;

    /**
     * Distance the probe should sit above the surface at rest — the vehicle's ground clearance.
     * <b>Must exceed the tallest step the vehicle is meant to ride over</b>, since a step taller
     * than the clearance reaches the hull itself and is a wall by definition.
     */
    public float restLength = 1.0f;

    /** Probe length. Past this there is no ground and the spring disengages. */
    public float maxLength = 2.0f;

    /** Cap on how fast the spring may raise the hull, in blocks per tick. Stops a shove on a spike. */
    public float maxRise = 0.5f;

    /** Returned by {@link #probe} when the footprint found nothing to stand on. */
    public static final double NO_GROUND = Double.NEGATIVE_INFINITY;

    private float velocity;
    private boolean onGround;

    /**
     * Highest surface beneath a hull's footprint, world Y, or {@link #NO_GROUND}.
     * <p>
     * A footprint query rather than a single ray, because a vehicle spanning several blocks should
     * follow the highest ground under any part of it rather than whatever happens to sit under its
     * centre. Surfaces above {@code ceiling} are skipped — those are walls the hull is beside, not
     * floors it is on.
     */
    public static double probe(BoxBuffer boxes, double centreX, double centreZ,
                               double halfX, double halfZ, double ceiling) {
        double top = NO_GROUND;
        for (int i = 0, n = boxes.size(); i < n; i++) {
            if (boxes.maxY(i) > ceiling) {
                continue;
            }
            if (boxes.maxX(i) <= centreX - halfX || boxes.minX(i) >= centreX + halfX) {
                continue;
            }
            if (boxes.maxZ(i) <= centreZ - halfZ || boxes.minZ(i) >= centreZ + halfZ) {
                continue;
            }
            if (boxes.maxY(i) > top) {
                top = boxes.maxY(i);
            }
        }
        return top;
    }

    public boolean onGround() {
        return onGround;
    }

    /** Spring velocity, for diagnostics. Not the body's velocity and must not be added to it. */
    public float springVelocity() {
        return velocity;
    }

    public void reset() {
        velocity = 0;
        onGround = false;
    }

    /**
     * Advances the spring one step.
     *
     * @param measuredLength   distance from the probe origin down to the surface, or a value at or
     *                         beyond {@link #maxLength} when nothing was hit
     * @param verticalVelocity the hull's own vertical velocity, used only to disengage while rising
     * @param dt               step length in ticks
     * @return vertical displacement to apply to the position this step; never a velocity
     */
    public float step(float measuredLength, float verticalVelocity, float dt) {
        if (measuredLength >= maxLength || dt <= 0) {
            onGround = false;
            velocity = 0;
            return 0;
        }
        // Already moving up under its own power — a jump, a ramp launch, thrust. Pulling it back
        // down to rest length here is what would make a vehicle feel glued to the terrain.
        if (verticalVelocity > 0) {
            onGround = false;
            velocity = 0;
            return 0;
        }
        onGround = true;

        // Implicit integration of a damped spring. Solving for the new velocity rather than
        // stepping the old one is what makes this stable no matter how stiff the spring is; the
        // explicit form needs hertz small enough for the step, which is the tuning trap that made
        // the earlier support lift oscillate.
        float omega = (float) (2.0 * Math.PI * hertz);
        float omegaH = omega * dt;
        velocity = (velocity - omega * omegaH * (measuredLength - restLength))
                / (1.0f + 2.0f * dampingRatio * omegaH + omegaH * omegaH);

        float displacement = velocity * dt;
        if (displacement > maxRise) {
            displacement = maxRise;
            velocity = maxRise / dt;
        }
        return displacement;
    }

}
