package org.ywzj.vehicle.custom.part.data;

public class SuspensionUnitData extends PartUnitData {

    private final float springStiffness;
    private final float damping;
    private final float restLength;
    private final float maxCompression;
    private final float maxExtension;
    private final float maxDroop;

    public SuspensionUnitData(SuspensionUnitPojo pojo) {
        super(pojo);
        springStiffness = pojo.springStiffness;
        damping = pojo.damping;
        restLength = pojo.restLength;
        maxCompression = Math.min(restLength, pojo.maxCompression);
        maxExtension = pojo.maxExtension;
        maxDroop = pojo.maxDroop == null ? Float.POSITIVE_INFINITY : Math.max(0, pojo.maxDroop);
    }

    public float getSpringStiffness() {
        return springStiffness;
    }

    public float getDamping() {
        return damping;
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

}
