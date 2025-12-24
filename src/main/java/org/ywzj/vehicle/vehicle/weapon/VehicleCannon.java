package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleCannon extends AbstractVehicleWeapon<VehicleCannonWeaponData> {

    public VehicleCannon(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleCannonWeaponData data, String serializeId) {
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
            BulletEntity bulletEntity = new BulletEntity(vehicle.level(), shooter, aimContext.position, getData().getExplosion());
            bulletEntity.shootFromRotation(vehicle, aimContext.direction.x, aimContext.direction.y, 0, data.getVelocity(), 0.6f);
            bulletEntity.setDamage(data.getDamage());
            bulletEntity.setHeadShot(data.getHeadshotMultiplier());
            vehicle.level().addFreshEntity(bulletEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
        return true;
    }

}
