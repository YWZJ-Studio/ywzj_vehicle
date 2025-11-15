package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.SmokeGrenadeEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

public class VehicleGrenade extends AbstractVehicleWeapon<VehicleGrenadeWeaponData> {

    public VehicleGrenade(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleGrenadeWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
    }

    @Override
    public void shoot(List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        if (!check(ammoSpawnPositions, ammoXRot, ammoYRot, shooter)) {
            return;
        }
        if (isCoolingDown() || isReloading() || !consumeAmmo(ammoSpawnPositions.size())) {
            return;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (Vec3 ammoSpawnPosition : ammoSpawnPositions) {
            if ("smoke".equals(data.getType())) {
                SmokeGrenadeEntity smokeGrenadeEntity = new SmokeGrenadeEntity(shooter, shooter.level());
                smokeGrenadeEntity.setBaseData(data);
                smokeGrenadeEntity.setPos(ammoSpawnPosition);
                smokeGrenadeEntity.shootFromRotation(this.getVehicle(), ammoXRot, ammoYRot, 0, 1f, 0);

                vehicle.level().playSound(null, vehicle, AllSounds.SMOKE_GRENADE_LAUNCHER.get(), SoundSource.PLAYERS, 16f, 1f);
                vehicle.level().addFreshEntity(smokeGrenadeEntity);
            }
        }
    }

}
