package org.ywzj.vehicle.custom.vehicle;

import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public record VehicleIndex<E extends AbstractVehicle, D extends BaseVehicleData>(
        ResourceLocation id,
        VehicleType<E, D> type,
        D data
) {

}
