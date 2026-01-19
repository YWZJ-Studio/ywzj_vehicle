package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;

public class DefenseStats {

    @SerializedName("damage_threshold")
    public float damageThreshold = 50;

    @SerializedName("impact_kinetic_damage_coefficient")
    public float impactKineticDamageCoefficient = 0.1f;

}
