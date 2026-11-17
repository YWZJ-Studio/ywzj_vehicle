package org.ywzj.vehicle.custom.part.data;

import net.minecraft.world.phys.Vec3;

public class SuspensionUnitData extends PartUnitData {

    private final float springStiffness;
    private final float damping;
    private final float acceleration;
    private final float restLength;
    private final float maxCompression;
    private final float maxExtension;
    private final float maxDroop;
    private final Vec3 centerOffset;

    public SuspensionUnitData(SuspensionUnitPojo pojo) {
        super(pojo);
        springStiffness = pojo.springStiffness;
        damping = pojo.damping;
        acceleration = pojo.acceleration;
        restLength = pojo.restLength;
        maxCompression = Math.min(restLength, pojo.maxCompression);
        maxExtension = pojo.maxExtension;
        maxDroop = pojo.maxDroop == null ? Float.POSITIVE_INFINITY : Math.max(0, pojo.maxDroop);
        centerOffset = pojo.centerOffset == null ? Vec3.ZERO : pojo.centerOffset;
    }

    public float getSpringStiffness() {
        return springStiffness;
    }

    public float getDamping() {
        return damping;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public float getRestLength() {
        return restLength;
    }

    public float getMaxCompression() {
        return maxCompression;
    }

    public float getMaxExtension() {
        return maxExtension;
    }

    public float getMaxDroop() {
        return maxDroop;
    }

    public Vec3 getCenterOffset() {
        return centerOffset;
    }

}
