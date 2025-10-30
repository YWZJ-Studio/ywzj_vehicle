package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.AerialBombEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

public class VehicleAerialBomb extends AbstractVehicleWeapon<BaseVehicleWeaponData> {

    public VehicleAerialBomb(AbstractVehicle vehicle, WeaponUnit unit, int index, BaseVehicleWeaponData data) {
        super(vehicle, unit, index, data);
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
            AerialBombEntity aerialBombEntity = new AerialBombEntity(AllEntities.AERIAL_BOMB.get(), vehicle.level());
            aerialBombEntity.shoot(this.getVehicle(), this.getName(), ammoSpawnPosition, ammoXRot, ammoYRot, this.getWeaponUnit().getOwner());
            vehicle.level().playSound(null, vehicle, AllSounds.BOMB_DROP.get(), SoundSource.PLAYERS, 16f, 1f);
            vehicle.level().addFreshEntity(aerialBombEntity);
        }
    }

}
