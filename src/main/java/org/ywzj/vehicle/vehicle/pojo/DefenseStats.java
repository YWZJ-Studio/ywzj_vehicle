package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;

public class DefenseStats {

    @SerializedName("damage_threshold")
    public float damageThreshold = 50;

    @SerializedName("impact_multiplier")
    public float impactMultiplier = 0.1f;

    @SerializedName("damage_transfer_coefficient")
    public float damageTransferCoefficient = 1f;

}
