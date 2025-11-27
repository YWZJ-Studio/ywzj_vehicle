package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.part.PartUnitEntry;
import org.ywzj.vehicle.custom.pojo.EnergyInfo;
import org.ywzj.vehicle.custom.pojo.ViewInfo;

import java.util.List;

public class BaseVehicleDataPojo {

    @SerializedName("max_health")
    public float maxHealth = 100f;

    @SerializedName("view_info")
    public ViewInfo viewInfo;

    @SerializedName("energy_info")
    public EnergyInfo energyInfo;

    @SerializedName("structure_model")
    public ResourceLocation structureModel = null;

    @SerializedName("parts")
    public List<PartUnitEntry<?, ?>> parts = List.of();

}
