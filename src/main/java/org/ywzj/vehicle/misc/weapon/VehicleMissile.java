package org.ywzj.vehicle.misc.weapon;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.weapon.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class VehicleMissile extends AbstractVehicleWeapon<BaseVehicleWeaponData> {
    public VehicleMissile(AbstractVehicle vehicle, WeaponUnit unit, int index, BaseVehicleWeaponData data) {
        super(vehicle, unit, index, data);
    }

    public long getShootInterval() {
        return this.getData().getShootInterval();
    }

    @Override
    public void shoot(Vec3 origin, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo()) {
            return;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        MissileEntity missileEntity = new MissileEntity(AllEntities.MISSILE.get(), vehicle.level());
        missileEntity.shoot(this.getVehicle(), this.getName(), origin, this.getWeaponUnit().getOwner());
        vehicle.level().playSound(null, vehicle, AllSounds.MISSILE_LAUNCH.get(), SoundSource.PLAYERS, 16f, 1f);
        vehicle.level().addFreshEntity(missileEntity);
    }
}
