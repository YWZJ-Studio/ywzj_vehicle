package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;

public class RopeUnitPojo extends PartUnitPojo {

    @SerializedName("max_length")
    public float maxLength = 20f;

    public RopeUnitPojo() {
        this.isSeat = false;
    }

}
