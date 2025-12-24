package org.ywzj.vehicle.custom.part.data;

public class RadarUnitData extends RotatableUnitData {

    protected float scanSectorAngle;
    protected float maxDistance;

    public RadarUnitData(String id) {
        super(id);
    }

    public RadarUnitData(RadarUnitPojo pojo) {
        super(pojo);
        this.scanSectorAngle = pojo.scanSectorAngle;
        this.maxDistance = pojo.maxDistance;
    }

    public float getScanSectorAngle() {
        return scanSectorAngle;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

}
