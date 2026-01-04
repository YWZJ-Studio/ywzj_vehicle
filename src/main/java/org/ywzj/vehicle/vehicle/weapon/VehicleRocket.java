package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.weapon.data.VehicleRocketWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.RocketEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleRocket extends AbstractVehicleWeapon<VehicleRocketWeaponData> {

    public VehicleRocket(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleRocketWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
    }

    @Override
    public boolean shoot(List<AimContext> aimContexts, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo(aimContexts.size())) {
            return false;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (AimContext aimContext : aimContexts) {
            RocketEntity rocketEntity = new RocketEntity(AllEntities.ROCKET.get(), vehicle.level());
            rocketEntity.damage = data.getDamage();
            rocketEntity.explosion = data.getExplosion();
            rocketEntity.shoot(this.getVehicle(), this.getDisplayName(), aimContext.position, aimContext.direction.x, aimContext.direction.y, this.getWeaponUnit().getOwner());
            rocketEntity.setDeltaMovement(rocketEntity.getLookAngle().scale(data.getVelocity()).add(getVehicle().getDeltaMovement()));
            vehicle.level().addFreshEntity(rocketEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
        return true;
    }

}
