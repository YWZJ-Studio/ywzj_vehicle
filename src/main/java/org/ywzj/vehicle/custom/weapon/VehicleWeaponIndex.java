package org.ywzj.vehicle.custom.weapon;

import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

/**
 * 代表了一个具体的包含参数配置的武器索引<br/>
 * @param <T> 载具武器的实际实现类
 * @param <D> 配置数据类型
 * @param id 载具武器id
 * @param type 载具武器类型
 * @param data 载具武器配置数据
 */
public record VehicleWeaponIndex<T extends AbstractVehicleWeapon<D>, D extends BaseVehicleWeaponData>(
        ResourceLocation id,
        VehicleWeaponType<T, D> type,
        D data
) {
    public T create(AbstractVehicle vehicle, WeaponUnit unit, int index) {
        return type.create(vehicle, unit, index, data);
    }
}
