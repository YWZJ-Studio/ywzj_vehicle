package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.ActiveProtectionGrenadeEntity;
import org.ywzj.vehicle.entity.weapon.FragGrenadeEntity;
import org.ywzj.vehicle.entity.weapon.SmokeGrenadeEntity;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
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
        if (isCoolingDown() || isReloading() || !consumeAmmo(aimContexts)) {
            return false;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (AimContext aimContext : aimContexts) {
            LivingEntity owner = this.getWeaponUnit().getOwner();
            if ("smoke".equals(data.getGrenade())) {
                SmokeGrenadeEntity entity = new SmokeGrenadeEntity(AllEntities.SMOKE_GRENADE.get(), vehicle.level(), data);
                entity.shoot(vehicle, this.getDisplayName(), aimContext.from, aimContext.direction.x, aimContext.direction.y,
                        data.getVelocity(), data.getInaccuracy(), owner);
                vehicle.level().addFreshEntity(entity);
            } else if ("frag".equals(data.getGrenade())) {
                FragGrenadeEntity entity = new FragGrenadeEntity(AllEntities.FRAG_GRENADE.get(), vehicle.level(), data);
                entity.shoot(vehicle, this.getDisplayName(), aimContext.from, aimContext.direction.x, aimContext.direction.y,
                        data.getVelocity(), data.getInaccuracy(), owner);
                vehicle.level().addFreshEntity(entity);
            } else if ("aps".equals(data.getGrenade())) {
                ActiveProtectionGrenadeEntity entity = new ActiveProtectionGrenadeEntity(AllEntities.APS_GRENADE.get(), vehicle.level(), data);
                entity.shoot(vehicle, this.getDisplayName(), aimContext.from, aimContext.direction.x, aimContext.direction.y,
                        data.getVelocity(), data.getInaccuracy(), owner);
                vehicle.level().addFreshEntity(entity);
            }
            vehicle.queueRecoil(getWeaponUnit(), data.getRecoil());
        }
        return true;
    }

}
