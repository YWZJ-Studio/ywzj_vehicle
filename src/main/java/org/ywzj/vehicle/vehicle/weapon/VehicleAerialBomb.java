package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.weapon.data.VehicleAerialBombWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.AerialBombEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

public class VehicleAerialBomb extends AbstractVehicleWeapon<VehicleAerialBombWeaponData> {

    public VehicleAerialBomb(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleAerialBombWeaponData data, String serializeId) {
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
            AerialBombEntity aerialBombEntity = new AerialBombEntity(AllEntities.AERIAL_BOMB.get(), vehicle.level());
            aerialBombEntity.shoot(this.getVehicle(), this.getDisplayName(), ammoSpawnPosition, ammoXRot, ammoYRot, this.getWeaponUnit().getOwner());
            vehicle.level().addFreshEntity(aerialBombEntity);
        }
    }

}
