package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class BaseVehicleData {
    @SerializedName("structure_model")
    private ResourceLocation structureModel;

    private Map<String, PartUnitData> partUnits;

    public ResourceLocation getStructureModel() {
        return structureModel;
    }
}
