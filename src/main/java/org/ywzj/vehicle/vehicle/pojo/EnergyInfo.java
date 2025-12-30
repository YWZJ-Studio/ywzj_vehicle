package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EnergyInfo {

    @SerializedName("energy_type")
    public String energyType = "fuel";

    @SerializedName("energy_density")
    public float energyDensity = 1f;

    @SerializedName("energy_capacity")
    public float energyCapacity = 1f;

    @SerializedName("energy_consumption_per_tick")
    public float energyConsumptionPerTick = 0.00001f;

    @SerializedName("engine_particle_offsets")
    public List<Vec3> engineParticleOffsets = new ArrayList<>();

}
