package org.ywzj.vehicle.custom.pojo;

import com.google.gson.annotations.SerializedName;

public class RotInfo {

    @SerializedName("x_rot_speed")
    public float xRotSpeed = 1.0f;

    @SerializedName("y_rot_speed")
    public float yRotSpeed = 1.0f;

    @SerializedName("x_rot_max")
    public float xRotMax = 18;

    @SerializedName("x_rot_min")
    public float xRotMin = -18;

    @SerializedName("y_rot_max")
    public float yRotMax = Float.MAX_VALUE;

    @SerializedName("y_rot_min")
    public float yRotMin = -Float.MAX_VALUE;

    @SerializedName("x_rot")
    public float xRot = 0f;

    @SerializedName("y_rot")
    public float yRot = 0f;

    @SerializedName("need_power")
    public boolean needPower = true;

}
