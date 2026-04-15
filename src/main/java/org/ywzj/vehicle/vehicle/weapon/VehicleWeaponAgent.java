package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleWeaponAgent extends AbstractVehicleWeapon<BaseVehicleWeaponData> {

    public VehicleWeaponAgent(AbstractVehicle vehicle, WeaponUnit weaponUnit, int index) {
        super(vehicle, weaponUnit, index, new BaseVehicleWeaponData(), null);
    }

    @Override
    public boolean shoot(List<AimContext> aimContexts, LivingEntity shooter) {
        return weaponUnit.getCurrentWeapon().get().shoot(aimContexts, shooter);
    }

}
