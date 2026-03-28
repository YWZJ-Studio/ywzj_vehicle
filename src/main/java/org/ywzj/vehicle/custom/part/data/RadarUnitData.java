package org.ywzj.vehicle.custom.part.data;

public class RadarUnitData extends RotatableUnitData {

    protected String radarType;
    protected float scanSectorAngle;
    protected float maxScanDistance;

    public RadarUnitData(String id) {
        super(id);
    }

    public RadarUnitData(RadarUnitPojo pojo) {
        super(pojo);
        this.radarType = pojo.radarType;
        this.scanSectorAngle = pojo.scanSectorAngle;
        this.maxScanDistance = pojo.maxScanDistance;
    }

    public String getRadarType() {
        return radarType;
    }

    public float getScanSectorAngle() {
        return scanSectorAngle;
    }

    public float getMaxScanDistance() {
        return maxScanDistance;
    }

}
