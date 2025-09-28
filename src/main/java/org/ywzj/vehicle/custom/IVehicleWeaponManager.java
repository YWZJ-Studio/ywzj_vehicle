package org.ywzj.vehicle.custom;

import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;

import java.util.Map;
import java.util.Optional;

public interface IVehicleWeaponManager {
    Map<ResourceLocation, VehicleWeaponIndex<?, ?>> getIndexes();

    Optional<VehicleWeaponIndex<?, ?>> getIndex(ResourceLocation id);
}
