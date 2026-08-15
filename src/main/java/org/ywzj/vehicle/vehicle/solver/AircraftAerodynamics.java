package org.ywzj.vehicle.vehicle.solver;

import org.joml.Vector3f;

/**
 * Arcade flight response for rotary- and fixed-wing vehicles: attitude is driven, not solved.
 * <p>
 * The contact solver owns translation and impacts for every vehicle, but an aircraft's attitude
 * must not come out of it. Pitch and roll that emerge from contact torque feel floaty and fight
 * the pilot, which is why flight simulators take attitude from the flight model and why Superb
 * Warfare — whose aircraft feel is the target here — drives orientation directly and lerps it.
 * <p>
 * Reading that mod's flight code, the gap between it and a physically-derived model turns out to
 * be narrow and to sit almost entirely in three places, all of them here:
 * <ol>
 *   <li><b>Drag is anisotropic.</b> An aircraft slips cheaply along its nose and expensively
 *       across it. Isotropic drag is what makes a helicopter feel like it is sliding on ice
 *       regardless of where it points; this is the single biggest contributor to feel.</li>
 *   <li><b>Pitch converts to acceleration, scaled by current speed.</b> Diving trades height for
 *       speed and climbing trades it back, rather than pitch being a purely visual attitude.</li>
 *   <li><b>Control authority ramps with how long the input is held</b>, so a tap nudges and a hold
 *       commits. Without it, roll input is twitchy at exactly the moment precision matters.</li>
 * </ol>
 * Deliberately not a lift/drag/angle-of-attack model. That is more correct and reads worse: the
 * goal is a machine that goes where it is pointed, with enough inertia to feel heavy.
 * <p>
 * MC-free, so it can be driven directly from a test harness.
 */
public final class AircraftAerodynamics {

    /** Drag retained per tick at zero speed, pointing where it is going. Near 1 is slippery. */
    public float baseDrag = 0.95f;

    /** How much extra drag speed itself adds. Sets the top speed for a given thrust. */
    public float speedDrag = 0.015f;

    /**
     * Extra drag retained when travelling along the nose rather than across it. This is the
     * anisotropy: at full alignment the aircraft keeps {@code baseDrag + alignmentBonus} of its
     * speed, sideways only {@code baseDrag}.
     */
    public float alignmentBonus = 0.02f;

    /** Vertical drag, kept separate so climb and sink rates tune independently of cruise. */
    public float verticalDrag = 0.95f;

    public float minDrag = 0.01f;
    public float maxDrag = 0.99f;

    /**
     * Acceleration along the nose per unit of speed, at full pitch. Diving accelerates, climbing
     * decelerates. Scaled by speed so it trades energy rather than manufacturing it.
     */
    public float pitchCoupling = 0.035f;

    /** Pitch, in degrees, at which {@link #pitchCoupling} is fully applied. */
    public float pitchCouplingFull = 15.0f;

    /** Per-tick roll retained with no input: an aircraft rolls level on its own. */
    public float rollCentring = 0.99f;

    /** Roll and pitch retained per tick while on the ground, so it settles flat. */
    public float groundLevelling = 0.98f;

    /** Ticks of held input over which control authority ramps to full. */
    public int authorityRampTicks = 7;

    /**
     * Most a tilted rotor's thrust is scaled up by, to make up the vertical component it loses.
     * A helicopter moves by tilting its disc, which points thrust away from vertical — so without
     * this, going anywhere costs altitude and the pilot has to ride the collective constantly. The
     * cap stops a near-inverted machine generating absurd thrust.
     */
    public float maxTiltCompensation = 1.6f;

    /** Below this much of the rotor still pointing up, compensation gives up rather than diverge. */
    public float minTiltCosine = 0.35f;

    /** Collective added per tick while sinking with no pilot input, as a fraction of full range. */
    public float altitudeHoldRate = 0.006f;

    /** Vertical speed inside which altitude hold does nothing, so it cannot hunt. */
    public float altitudeHoldDeadband = 0.02f;

    /**
     * Fraction of sideways velocity a winged aircraft sheds per tick.
     * <p>
     * Fixed-wing already varies its drag with angle of attack, which covers the pitch plane. It
     * has nothing for sideslip, so a plane can drift across its own fuselage as cheaply as it
     * flies along it — the same skating that anisotropic drag fixes for helicopters, one axis over.
     */
    public float sideslipDrag = 0.15f;

    private int heldTicks;

    private final Vector3f facing = new Vector3f();

    /**
     * Control authority for an input held this many ticks. A tap nudges, a hold commits — which is
     * what stops roll being twitchy at low deflection without making it sluggish at high.
     */
    public float authority(boolean held) {
        heldTicks = held ? Math.min(heldTicks + 1, authorityRampTicks) : 0;
        return authorityRampTicks <= 0 ? 1 : (float) heldTicks / authorityRampTicks;
    }

    public void resetAuthority() {
        heldTicks = 0;
    }

    /**
     * Applies drag and the pitch/speed trade to a velocity, in place.
     *
     * @param velocity blocks per tick, modified in place
     * @param yawDeg   where the nose points
     * @param pitchDeg positive is nose-down, matching Minecraft's convention
     * @param onGround suppresses the pitch trade, since a grounded aircraft is not flying
     */
    public void applyToVelocity(Vector3f velocity, float yawDeg, float pitchDeg, boolean onGround) {
        float yaw = (float) Math.toRadians(-yawDeg);
        facing.set((float) -Math.sin(yaw), 0, (float) Math.cos(yaw));

        float horizontal = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        // How much of the motion is along the nose, 1 either way down the fuselage and 0 across
        // it. Using |cos| rather than the angle keeps flying backwards as cheap as forwards, which
        // matters for helicopters, and costs no trigonometry.
        float alignment = horizontal > 1.0e-4f
                ? Math.abs((velocity.x * facing.x + velocity.z * facing.z) / horizontal)
                : 1.0f;

        if (!onGround) {
            float pitchFactor = clamp(pitchDeg / pitchCouplingFull, -1, 1);
            float speed = velocity.length();
            float trade = pitchCoupling * pitchFactor * speed;
            velocity.add(facing.x * trade, 0, facing.z * trade);
            // Diving buys speed from height and climbing sells it back. Applying the vertical part
            // through the same term is what stops a climb being free.
            velocity.y -= trade * pitchFactor * 0.5f;
        }

        float drag = clamp(baseDrag - speedDrag * velocity.length() + alignmentBonus * alignment,
                minDrag, maxDrag);
        velocity.x *= drag;
        velocity.z *= drag;
        velocity.y *= verticalDrag;
    }

    /**
     * Rolls level over time, and flattens onto the ground when there.
     *
     * @return the new roll
     */
    public float settleRoll(float rollDeg, boolean rollInput, boolean onGround) {
        if (onGround) {
            return rollDeg * groundLevelling;
        }
        return rollInput ? rollDeg : rollDeg * rollCentring;
    }

    /** Pitch flattens on the ground only; in the air it is entirely the pilot's. */
    public float settlePitch(float pitchDeg, boolean onGround) {
        return onGround ? pitchDeg * groundLevelling : pitchDeg;
    }

    /**
     * Velocity to remove along the aircraft's lateral axis this tick, given how fast it is
     * sliding sideways. Apply along the body's right axis.
     */
    public float sideslipBleed(float lateralVelocity) {
        return lateralVelocity * sideslipDrag;
    }

    /**
     * How much to scale rotor thrust so that tilting the disc does not cost altitude.
     * <p>
     * Thrust acts along the machine's own up axis, so at a tilt of θ only cos θ of it holds the
     * aircraft up. Dividing by that restores the vertical component and lets a helicopter
     * translate at a constant height — which is the difference between a machine that flies and
     * one that sinks every time the pilot asks it to go somewhere.
     *
     * @param upY vertical component of the vehicle's up axis, i.e. cos of its tilt
     */
    public float liftTiltCompensation(float upY) {
        if (upY <= minTiltCosine) {
            return maxTiltCompensation;
        }
        return Math.min(maxTiltCompensation, 1.0f / upY);
    }

    /**
     * Collective correction to hold altitude, as a fraction of full collective range.
     * <p>
     * Superb Warfare's helicopters do this whenever the pilot is not touching the collective, and
     * it is most of why theirs hold height through a manoeuvre. Deadbanded so it settles instead
     * of hunting around zero.
     *
     * @return positive to raise collective, negative to lower it, zero inside the deadband
     */
    public float altitudeHoldDelta(float verticalVelocity) {
        if (verticalVelocity < -altitudeHoldDeadband) {
            return altitudeHoldRate;
        }
        if (verticalVelocity > altitudeHoldDeadband) {
            return -altitudeHoldRate;
        }
        return 0;
    }

    /**
     * Attitude the running gear implies, for a grounded aircraft.
     * <p>
     * Lifted from Superb Warfare's terrain compaction: probe the gear points, take the height of
     * each above the surface, and lean the aircraft toward the slope. Kinematic on purpose — a
     * parked aircraft should sit on the terrain, not negotiate with a solver about it.
     *
     * @param frontDrop  how far the forward gear is above its surface
     * @param rearDrop   how far the rear gear is above its surface
     * @param wheelbase  distance between them, along the fuselage
     * @return target pitch in degrees, positive nose-down
     */
    public static float terrainPitch(float frontDrop, float rearDrop, float wheelbase) {
        if (wheelbase <= 1.0e-4f) {
            return 0;
        }
        return (float) Math.toDegrees(Math.atan2(frontDrop - rearDrop, wheelbase));
    }

    /** Moves an angle a fraction of the way toward a target, the shortest way round. */
    public static float lerpAngle(float current, float target, float rate) {
        float delta = target - current;
        while (delta > 180) {
            delta -= 360;
        }
        while (delta < -180) {
            delta += 360;
        }
        return current + delta * rate;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

}
