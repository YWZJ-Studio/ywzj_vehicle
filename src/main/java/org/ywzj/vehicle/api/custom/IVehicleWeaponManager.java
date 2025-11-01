package org.ywzj.vehicle.api.custom;

import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;

import java.util.Map;
import java.util.Optional;

public interface IVehicleWeaponManager {
    /**
     * 获取所有载具武器索引
     * @return 载具武器索引映射
     */
    Map<ResourceLocation, VehicleWeaponIndex<?, ?>> getIndexes();

    /**
     * 根据ID获取载具武器索引
     * @param id 载具武器索引ID
     * @return 载具武器索引
     */
    Optional<VehicleWeaponIndex<?, ?>> getIndex(ResourceLocation id);
}
