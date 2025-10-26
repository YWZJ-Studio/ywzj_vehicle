package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.weapon.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.RocketEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class VehicleRocket extends AbstractVehicleWeapon<BaseVehicleWeaponData> {

    public VehicleRocket(AbstractVehicle vehicle, WeaponUnit unit, int index, BaseVehicleWeaponData data) {
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

        RocketEntity rocketEntity = new RocketEntity(AllEntities.ROCKET.get(), vehicle.level());
        rocketEntity.shoot(this.getVehicle(), this.getName(), origin, ammoXRot, ammoYRot, this.getWeaponUnit().getOwner());
        vehicle.level().playSound(null, vehicle, AllSounds.MISSILE_LAUNCH.get(), SoundSource.PLAYERS, 16f, 1f);
        vehicle.level().addFreshEntity(rocketEntity);
    }

}
