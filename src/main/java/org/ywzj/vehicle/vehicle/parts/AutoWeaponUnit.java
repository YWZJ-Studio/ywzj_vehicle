package org.ywzj.vehicle.vehicle.parts;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Comparator;
import java.util.List;

public class AutoWeaponUnit extends WeaponUnit {

    public AutoWeaponUnit(int index, AbstractVehicle vehicle, WeaponUnitData data) {
        super(index, vehicle, data);
    }

    public void tick() {
        if (!vehicle.level().isClientSide()) {
            double radius = 32.0;
            AABB box = vehicle.getBoundingBox().inflate(radius);
            List<Entity> entities = vehicle.level().getEntities(
                    vehicle,
                    box,
                    this::shouldAim
            );
            entities.sort(Comparator.comparingDouble(e -> -e.getDeltaMovement().lengthSqr()));
            if (!entities.isEmpty()) {
                Entity target = entities.get(0);
                aim(target.position().add(target.getDeltaMovement()));
                if (Math.abs(getXRot() - getXAimRot()) < 5 && Math.abs(getYRot() - getYAimRot()) < 5) {
                    shoot(0, aimContexts(), vehicle.getDriver());
                }
            }
        }
        super.tick();
    }

    public boolean shouldAim(Entity entity) {
        return entity.isAlive()
                && entity != vehicle
                && !(entity instanceof Projectile projectile && projectile.getOwner() == vehicle)
                && !(entity instanceof ItemEntity)
                && entity.getDeltaMovement().length() > 1;
    }

}
