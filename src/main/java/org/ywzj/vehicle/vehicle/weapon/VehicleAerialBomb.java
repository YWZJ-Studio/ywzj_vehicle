package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.weapon.data.VehicleAerialBombWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.AerialBombEntity;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleAerialBomb extends AbstractVehicleWeapon<VehicleAerialBombWeaponData> {

    public VehicleAerialBomb(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleAerialBombWeaponData data, String serializeId) {
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
        WeaponUnit weaponUnit = getWeaponUnit().getRootParentWeaponUnit();
        for (AimContext aimContext : aimContexts) {
            AerialBombEntity entity = new AerialBombEntity(AllEntities.AERIAL_BOMB.get(), vehicle.level(), data.getWeaponId());
            entity.explosion = data.getExplosion();
            entity.fuseDelayTick = data.getFuseDelayTick();
            entity.penetrationDepth = data.getPenetrationDepth();
            entity.dragCoefficient = data.getDragCoefficient();
            entity.maxG = data.getMaxG();
            entity.referenceSpeed = data.getReferenceSpeed();
            entity.homing = data.isHoming();
            if (data.isHoming()) {
                entity.weaponUnit = weaponUnit;
                Entity lockedEntity = weaponUnit.getLockedEntity();
                if (lockedEntity != null) {
                    entity.targetEntity = lockedEntity;
                } else {
                    entity.targetPos = aimContext.position;
                }
            }
            entity.shoot(vehicle, this.getDisplayName(), aimContext.from, aimContext.direction.x, aimContext.direction.y, data.getInaccuracy(), this.getWeaponUnit().getOwner());
            vehicle.level().addFreshEntity(entity);
        }
        return true;
    }

}
