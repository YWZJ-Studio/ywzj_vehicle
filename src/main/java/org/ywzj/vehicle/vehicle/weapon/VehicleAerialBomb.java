package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.weapon.data.VehicleAerialBombWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.AerialBombEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleAerialBomb extends AbstractVehicleWeapon<VehicleAerialBombWeaponData> {

    public VehicleAerialBomb(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleAerialBombWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
    }

    @Override
    public boolean shoot(List<AimContext> aimContexts, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo(aimContexts)) {
            return false;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (AimContext aimContext : aimContexts) {
            AerialBombEntity aerialBombEntity = new AerialBombEntity(AllEntities.AERIAL_BOMB.get(), vehicle.level());
            aerialBombEntity.explosion = data.getExplosion();
            aerialBombEntity.shoot(this.getVehicle(), this.getDisplayName(), aimContext.position, aimContext.direction.x, aimContext.direction.y, this.getWeaponUnit().getOwner());
            vehicle.level().addFreshEntity(aerialBombEntity);
        }
        return true;
    }

}
