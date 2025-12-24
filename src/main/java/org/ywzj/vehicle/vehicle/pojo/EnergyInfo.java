package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;

public class EnergyInfo {

    @SerializedName("energy_type")
    public String energyType = "fuel";

    @SerializedName("energy_density")
    public float energyDensity = 1f;

    @SerializedName("energy_capacity")
    public float energyCapacity = 1f;

    @SerializedName("energy_consumption_per_tick")
    public float energyConsumptionPerTick = 0.00001f;

}
