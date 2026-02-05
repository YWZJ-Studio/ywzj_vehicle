package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.DecoyFlareEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleDecoyFlare extends AbstractVehicleWeapon<BaseVehicleWeaponData> {

    public VehicleDecoyFlare(AbstractVehicle vehicle, WeaponUnit unit, int index, BaseVehicleWeaponData data, String serializeId) {
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
            DecoyFlareEntity decoyFlareEntity = new DecoyFlareEntity(AllEntities.DECOY_FLARE.get(), vehicle.level());
            decoyFlareEntity.shoot(vehicle, getDisplayName(), aimContext.position, aimContext.direction.x, aimContext.direction.y, this.getWeaponUnit().getOwner());
            decoyFlareEntity.setDeltaMovement(decoyFlareEntity.getLookAngle().scale(data.getVelocity()).add(getVehicle().getDeltaMovement()));
            vehicle.level().addFreshEntity(decoyFlareEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
        }
        return true;
    }

}
