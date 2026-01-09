package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.ActiveProtectionGrenadeEntity;
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

        Entity shooterEntity = shooter == null ? vehicle : shooter;
        for (AimContext aimContext : aimContexts) {
            if ("smoke".equals(data.getGrenade())) {
                SmokeGrenadeEntity smokeGrenadeEntity = new SmokeGrenadeEntity(shooterEntity, shooterEntity.level());
                smokeGrenadeEntity.setBaseData(data);
                smokeGrenadeEntity.setPos(aimContext.position);
                smokeGrenadeEntity.shootFromRotation(this.getVehicle(), aimContext.direction.x, aimContext.direction.y, 0, 1f, data.getInaccuracy());
                smokeGrenadeEntity.setXRot(aimContext.direction.x);
                smokeGrenadeEntity.setYRot(aimContext.direction.y);
                vehicle.level().addFreshEntity(smokeGrenadeEntity);
                vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
            }
            if ("aps".equals(data.getGrenade())) {
                ActiveProtectionGrenadeEntity activeProtectionGrenade = new ActiveProtectionGrenadeEntity(shooterEntity, shooterEntity.level());
                activeProtectionGrenade.setBaseData(data);
                activeProtectionGrenade.setPos(aimContext.position);
                activeProtectionGrenade.shootFromRotation(this.getVehicle(), aimContext.direction.x, aimContext.direction.y, 0, 1f, data.getInaccuracy());
                activeProtectionGrenade.setXRot(aimContext.direction.x);
                activeProtectionGrenade.setYRot(aimContext.direction.y);
                vehicle.level().addFreshEntity(activeProtectionGrenade);
                vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
            }
        }
        return true;
    }

}
