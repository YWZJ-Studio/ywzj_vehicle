package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;

public class RadarUnitPojo extends RotatableUnitPojo {

    @SerializedName("scan_sector_angle")
    public float scanSectorAngle = 45;

    @SerializedName("max_distance")
    public float maxDistance = 256;

}
