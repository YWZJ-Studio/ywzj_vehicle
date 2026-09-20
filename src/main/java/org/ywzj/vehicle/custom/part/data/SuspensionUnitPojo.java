package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;

public class SuspensionUnitPojo extends PartUnitPojo {

    @SerializedName("spring_stiffness")
    public float springStiffness = 200000f;

    @SerializedName("damping")
    public float damping = 800f;

    @SerializedName("rest_length")
    public float restLength = 0.1f;

    @SerializedName("max_compression")
    public float maxCompression = 0.1f;

    @SerializedName("max_extension")
    public float maxExtension = 0.025f;

    @SerializedName("max_droop")
    public Float maxDroop = 0.1f;

    public SuspensionUnitPojo() {
        this.isSeat = false;
    }

}
