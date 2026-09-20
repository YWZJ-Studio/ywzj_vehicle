package org.ywzj.vehicle.util;

public final class PhysicsHelper {

    public static final float TICKS_PER_SECOND = 20f;
    public static final float TICKS_PER_SECOND_SQUARED = TICKS_PER_SECOND * TICKS_PER_SECOND;
    public static final float GRAVITY = 9.8f;
    public static final float KILOGRAMS_PER_TONNE = 1000f;

    public static double accelerationPerTick(double forceNewtons, double massKilograms) {
        return forceNewtons / massKilograms / TICKS_PER_SECOND_SQUARED;
    }

    public static float forcePerTick(float forceNewtons) {
        return forceNewtons / TICKS_PER_SECOND_SQUARED;
    }

    public static float dampingPerTick(float damping) {
        return damping / TICKS_PER_SECOND;
    }

    public static double dragAccelerationPerTick(double dragFactor, double massKilograms, double speedSquared) {
        return dragFactor * speedSquared / massKilograms;
    }

}
