package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleCannon extends AbstractVehicleWeapon<VehicleCannonWeaponData> {

    public VehicleCannon(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleCannonWeaponData data, String serializeId) {
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
            BulletEntity bulletEntity = new BulletEntity(vehicle.level(), vehicle, shooter, aimContext.position, getData().getExplosion(), data.getWeaponId());
            bulletEntity.shootFromRotation(vehicle, aimContext.direction.x, aimContext.direction.y, 0, data.getVelocity(), data.getInaccuracy());
            bulletEntity.setDamage(data.getDamage());
            bulletEntity.headShot = data.getHeadshotMultiplier();
            vehicle.level().addFreshEntity(bulletEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
        return true;
    }

}
