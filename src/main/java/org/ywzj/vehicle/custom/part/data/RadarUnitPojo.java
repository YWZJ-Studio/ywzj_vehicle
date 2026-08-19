package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;

public class RadarUnitPojo extends RotatableUnitPojo {

    @SerializedName("radar_type")
    public String radarType = "";

    @SerializedName("scan_sector_angle")
    public float scanSectorAngle = 45;

    @SerializedName("max_scan_distance")
    public float maxScanDistance = 256;

    @SerializedName("anti_ground")
    public boolean antiGround = false;

    @SerializedName("ui_hide")
    public boolean uiHide = false;

}
