package org.ywzj.vehicle.misc.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.custom.weapon.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;

public class VehicleCannon extends AbstractVehicleWeapon<BaseVehicleWeaponData> {
    public VehicleCannon(AbstractVehicle vehicle, int index, BaseVehicleWeaponData data) {
        super(vehicle, index, data);
    }

    public long getShootInterval() {
        return this.getData().getShootInterval();
    }

    @Override
    public void shoot(Vec3 origin, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo()) {
            return;
        }
        shooter.sendSystemMessage(Component.literal("Remaining ammo: " + remainAmmo));
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();
        BulletEntity bulletEntity = new BulletEntity(vehicle.level(), shooter, origin);
        bulletEntity.shootFromRotation(vehicle, ammoXRot, ammoYRot, 0, 10.0f, 1f);

        bulletEntity.setDamage(data.getDamage());
        bulletEntity.setHeadShot(data.getHeadshotMultiplier());

        vehicle.level().addFreshEntity(bulletEntity);
    }
}
