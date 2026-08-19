package org.ywzj.vehicle.vehicle.solver;

import org.joml.Vector3f;

/**
 * Arcade flight response for rotary- and fixed-wing vehicles: attitude is driven, not solved.
 * Contact solver owns translation and impacts; flight model drives orientation. Implements
 * anisotropic drag, speed-coupled pitch, and control authority ramping. Not a lift/drag model.
 */
public final class AircraftAerodynamics {

    /** Drag retained per tick at zero speed, pointing where it is going. Near 1 is slippery. */
    public float baseDrag = 0.95f;

    /** How much extra drag speed itself adds. Sets the top speed for a given thrust. */
    public float speedDrag = 0.015f;

    /**
     * Extra drag retained when travelling along the nose rather than across it; the difference
     * between flying along the fuselage and sliding across it.
     */
    public float alignmentBonus = 0.02f;

    /** Vertical drag, kept separate so climb and sink rates tune independently of cruise. */
    public float verticalDrag = 0.95f;

    public float minDrag = 0.01f;
    public float maxDrag = 0.99f;

    /** Acceleration along the nose per unit of speed at full pitch; diving trades height for speed. */
    public float pitchCoupling = 0.035f;

    /**
     * Pitch in degrees at which pitchCoupling is fully applied. Small value keeps low-amplitude
     * corrections small while approaching Superb Warfare's behavior across the 5-15° band.
     */
    public float pitchCouplingFull = 5.0f;

    /** Per-tick roll retained with no input: an aircraft rolls level on its own. */
    public float rollCentring = 0.99f;

    /** Roll and pitch retained per tick while on the ground, so it settles flat. */
    public float groundLevelling = 0.98f;

    /** Ticks of held input over which control authority ramps to full. */
    public int authorityRampTicks = 7;

    /**
     * Maximum scale of rotor thrust to compensate for tilt; prevents a near-inverted machine from
     * generating absurd lift and avoids requiring constant collective input to translate.
     */
    public float maxTiltCompensation = 1.6f;

    /** Below this vertical component of the rotor axis, tilt compensation clamps to avoid divergence. */
    public float minTiltCosine = 0.35f;

    /**
     * Scale of disc tilt relative to airframe tilt. Helicopter acceleration without this is
     * gravity times tan(tilt), which at this mod's gravity would halve the speed Superb Warfare
     * reaches with the same lean. Applies to horizontal thrust only; vertical is untouched.
     */
    public float discTiltGain = 1.8f;

    /** Maximum collective change per tick for altitude hold, as a fraction of full range. */
    public float altitudeHoldRate = 0.006f;

    /**
     * Collective correction per unit vertical speed, as a fraction of full range. Proportional
     * control rather than fixed step to converge; high gain causes oscillation above 0.4.
     */
    public float altitudeHoldGain = 0.12f;

    /**
     * Vertical speed band inside which altitude hold does nothing. Float-noise guard that prevents
     * proportional correction from converging to the edge.
     */
    public float altitudeHoldDeadband = 0.0002f;

    /** Fraction of lateral velocity shed per tick; prevents fixed-wing from drifting sideways cheaply. */
    public float sideslipDrag = 0.15f;

    private int heldTicks;

    private final Vector3f facing = new Vector3f();

    /**
     * Control authority for an input held this many ticks. A tap nudges, hold commits.
     * @param held whether the input is currently pressed
     * @return authority as a fraction 0 to 1
     */
    public float authority(boolean held) {
        heldTicks = held ? Math.min(heldTicks + 1, authorityRampTicks) : 0;
        return authorityRampTicks <= 0 ? 1 : (float) heldTicks / authorityRampTicks;
    }

    public void resetAuthority() {
        heldTicks = 0;
    }

    /**
     * Applies drag and pitch-speed coupling to a velocity in place.
     * @param velocity blocks per tick, modified in place
     * @param yawDeg facing direction
     * @param pitchDeg positive nose-down
     * @param onGround suppresses pitch coupling
     */
    public void applyToVelocity(Vector3f velocity, float yawDeg, float pitchDeg, boolean onGround) {
        float yaw = (float) Math.toRadians(-yawDeg);
        facing.set((float) -Math.sin(yaw), 0, (float) Math.cos(yaw));

        float horizontal = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        // Motion along nose vs across it; using absolute cosine keeps backwards flight as cheap as forward.
        float alignment = horizontal > 1.0e-4f
                ? Math.abs((velocity.x * facing.x + velocity.z * facing.z) / horizontal)
                : 1.0f;

        if (!onGround) {
            float pitchFactor = clamp(pitchDeg / pitchCouplingFull, -1, 1);
            float speed = velocity.length();
            float trade = pitchCoupling * pitchFactor * speed;
            // Along the nose, not the floor; vertical cost is sin(pitch).
            float pitchRad = (float) Math.toRadians(pitchDeg);
            float sinPitch = (float) Math.sin(pitchRad);
            float cosPitch = (float) Math.cos(pitchRad);
            velocity.add(facing.x * trade * cosPitch, 0, facing.z * trade * cosPitch);
            velocity.y -= trade * sinPitch;
        }

        float drag = clamp(baseDrag - speedDrag * velocity.length() + alignmentBonus * alignment,
                minDrag, maxDrag);
        velocity.x *= drag;
        velocity.z *= drag;
        velocity.y *= verticalDrag;
    }

    /** Rolls level over time, and flattens onto the ground when there. */
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

    /** Lateral velocity to bleed per tick. Apply along the aircraft's right axis. */
    public float sideslipBleed(float lateralVelocity) {
        return lateralVelocity * sideslipDrag;
    }

    /**
     * Scale to restore vertical thrust when the rotor is tilted. Thrust acts along the machine's
     * up axis, so tilt costs vertical component; restoring it lets the helicopter translate at
     * constant height.
     * @param upY vertical component of vehicle's up axis
     */
    public float liftTiltCompensation(float upY) {
        if (upY <= minTiltCosine) {
            return maxTiltCompensation;
        }
        return Math.min(maxTiltCompensation, 1.0f / upY);
    }

    /**
     * Collective correction to hold altitude, as a fraction of full range. Deadbanded to prevent
     * hunting. Must be sampled after gravity if measuring the velocity before gravity causes a
     * permanent sink of one tick.
     * @param verticalVelocity vertical velocity after gravity
     * @return collective adjustment, zero inside deadband
     */
    public float altitudeHoldDelta(float verticalVelocity) {
        if (Math.abs(verticalVelocity) <= altitudeHoldDeadband) {
            return 0;
        }
        return clamp(-verticalVelocity * altitudeHoldGain, -altitudeHoldRate, altitudeHoldRate);
    }

    /**
     * Pitch for a grounded aircraft derived from gear heights. Kinematic attitude adjustment based
     * on terrain slope, not a solver.
     * @param frontDrop height of forward gear above surface
     * @param rearDrop height of rear gear above surface
     * @param wheelbase distance between gear along fuselage
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
