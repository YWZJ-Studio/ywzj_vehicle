package org.ywzj.vehicle.custom.part.data;

public class RadarUnitData extends RotatableUnitData {

    protected String radarType;
    protected float scanSectorAngle;
    protected float maxScanDistance;
    protected boolean uiHide;

    public RadarUnitData(String id) {
        super(id);
    }

    public RadarUnitData(RadarUnitPojo pojo) {
        super(pojo);
        this.radarType = pojo.radarType;
        this.scanSectorAngle = pojo.scanSectorAngle;
        this.maxScanDistance = pojo.maxScanDistance;
        this.uiHide = pojo.uiHide;
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

    public boolean isUiHide() {
        return uiHide;
    }

}
