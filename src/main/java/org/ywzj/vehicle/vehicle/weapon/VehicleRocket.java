package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.weapon.data.VehicleRocketWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.RocketEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

public class VehicleRocket extends AbstractVehicleWeapon<VehicleRocketWeaponData> {

    public VehicleRocket(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleRocketWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
    }

    @Override
    public boolean shoot(List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo(ammoSpawnPositions.size())) {
            return false;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (Vec3 ammoSpawnPosition : ammoSpawnPositions) {
            RocketEntity rocketEntity = new RocketEntity(AllEntities.ROCKET.get(), vehicle.level());
            rocketEntity.shoot(this.getVehicle(), this.getDisplayName(), ammoSpawnPosition, ammoXRot, ammoYRot, this.getWeaponUnit().getOwner());
            rocketEntity.setDeltaMovement(rocketEntity.getLookAngle().scale(data.getVelocity()).add(getVehicle().getDeltaMovement()));
            vehicle.level().addFreshEntity(rocketEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
        return true;
    }

}
