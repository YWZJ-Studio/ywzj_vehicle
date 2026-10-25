package org.ywzj.vehicle.util;

public final class ZoomSmoother {

    private static final double RESPONSE = 32.0;
    private double value;
    private double velocity;
    private double target;
    private long lastUpdateNanos;

    public ZoomSmoother(double initialValue) {
        value = target = initialValue;
        lastUpdateNanos = System.nanoTime();
    }

    public double update(double targetValue) {
        long now = System.nanoTime();
        double elapsed = Math.max(0, (now - lastUpdateNanos) * 1.0E-9);
        lastUpdateNanos = now;
        double offset = value - target;
        double motion = velocity + RESPONSE * offset;
        double decay = Math.exp(-RESPONSE * elapsed);
        double nextValue = target + (offset + motion * elapsed) * decay;
        velocity = (velocity - RESPONSE * motion * elapsed) * decay;
        if ((target - value) * (target - nextValue) <= 0) {
            value = target;
            velocity = 0;
        } else {
            value = nextValue;
        }
        target = targetValue;
        return value;
    }

}
