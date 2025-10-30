package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class VehicleCannon extends AbstractVehicleWeapon<BaseVehicleWeaponData> {

    public VehicleCannon(AbstractVehicle vehicle, WeaponUnit unit, int index, BaseVehicleWeaponData data) {
        super(vehicle, unit, index, data);
    }

    @Override
    public void shoot(Vec3 origin, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo()) {
            return;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();
        BulletEntity bulletEntity = new BulletEntity(vehicle.level(), shooter, origin);
        bulletEntity.shootFromRotation(vehicle, ammoXRot, ammoYRot, 0, 10.0f, 0f);

        bulletEntity.setDamage(data.getDamage());
        bulletEntity.setHeadShot(data.getHeadshotMultiplier());

        vehicle.level().addFreshEntity(bulletEntity);
    }

}
