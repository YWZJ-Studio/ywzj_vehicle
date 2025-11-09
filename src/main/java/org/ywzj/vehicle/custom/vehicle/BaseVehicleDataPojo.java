package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.part.PartUnitEntry;

import java.util.List;

public class BaseVehicleDataPojo {

    @SerializedName("structure_model")
    public ResourceLocation structureModel = null;

    @SerializedName("parts")
    public List<PartUnitEntry<?, ?>> parts = List.of();

}
