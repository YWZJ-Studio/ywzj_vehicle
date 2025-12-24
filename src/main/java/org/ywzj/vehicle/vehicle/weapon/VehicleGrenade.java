package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.SmokeGrenadeEntity;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleGrenade extends AbstractVehicleWeapon<VehicleGrenadeWeaponData> {

    public VehicleGrenade(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleGrenadeWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
    }

    @Override
    public boolean shoot(List<AimContext> aimContexts, LivingEntity shooter) {
        if (!check(aimContexts, shooter)) {
            return false;
        }
        if (isCoolingDown() || isReloading() || !consumeAmmo(aimContexts.size())) {
            return false;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (AimContext aimContext : aimContexts) {
            if ("smoke".equals(data.getGrenade())) {
                SmokeGrenadeEntity smokeGrenadeEntity = new SmokeGrenadeEntity(shooter, shooter.level());
                smokeGrenadeEntity.setBaseData(data);
                smokeGrenadeEntity.setPos(aimContext.position);
                smokeGrenadeEntity.shootFromRotation(this.getVehicle(), aimContext.direction.x, aimContext.direction.y, 0, 1f, 0);
                smokeGrenadeEntity.setXRot(aimContext.direction.x);
                smokeGrenadeEntity.setYRot(aimContext.direction.y);
                vehicle.level().addFreshEntity(smokeGrenadeEntity);
                vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
            }
        }
        return true;
    }

}
