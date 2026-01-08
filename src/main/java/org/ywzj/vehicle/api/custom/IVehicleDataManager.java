package org.ywzj.vehicle.api.custom;

import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;

import java.util.Map;
import java.util.Optional;

public interface IVehicleDataManager {

    Map<ResourceLocation, BaseVehicleData> getVehicleData();

    Optional<BaseVehicleData> getVehicleData(ResourceLocation id);

}
