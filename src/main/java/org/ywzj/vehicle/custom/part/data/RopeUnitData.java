package org.ywzj.vehicle.custom.part.data;

public class RopeUnitData extends PartUnitData {

    private final float maxLength;

    public RopeUnitData(RopeUnitPojo pojo) {
        super(pojo);
        this.maxLength = Math.max(0, pojo.maxLength);
    }

    public float getMaxLength() {
        return maxLength;
    }

    public static RopeUnitData create(String id, float maxLength) {
        RopeUnitPojo pojo = new RopeUnitPojo();
        pojo.id = id;
        pojo.maxLength = maxLength;
        return new RopeUnitData(pojo);
    }

}
