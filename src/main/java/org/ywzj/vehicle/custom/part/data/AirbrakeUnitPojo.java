package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;

public class AirbrakeUnitPojo extends PartUnitPojo {

    @SerializedName("drag_k")
    public float dragK = 0.5f;

}
