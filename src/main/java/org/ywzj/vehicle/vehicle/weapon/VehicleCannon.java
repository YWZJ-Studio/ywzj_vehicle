package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

public class VehicleCannon extends AbstractVehicleWeapon<VehicleCannonWeaponData> {

    public VehicleCannon(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleCannonWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
    }

    @Override
    public void shoot(List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo(ammoSpawnPositions.size())) {
            return;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (Vec3 ammoSpawnPosition : ammoSpawnPositions) {
            BulletEntity bulletEntity = new BulletEntity(vehicle.level(), shooter, ammoSpawnPosition, getData().getExplosion());
            bulletEntity.shootFromRotation(vehicle, ammoXRot, ammoYRot, 0, data.getVelocity(), 0f);
            bulletEntity.setDamage(data.getDamage());
            bulletEntity.setHeadShot(data.getHeadshotMultiplier());
            vehicle.level().addFreshEntity(bulletEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
    }

}
