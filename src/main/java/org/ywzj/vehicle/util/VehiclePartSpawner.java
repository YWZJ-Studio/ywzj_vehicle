package org.ywzj.vehicle.util;

import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.misc.VehiclePart;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.PartUnit;

public final class VehiclePartSpawner {

    private VehiclePartSpawner() {}

    public static void spawnDestroyedParts(AbstractVehicle vehicle) {
        if (vehicle.level().isClientSide()) {
            return;
        }
        for (PartUnit<?> partUnit : vehicle.getPartUnits()) {
            if (!partUnit.isDetachable()) {
                continue;
            }
            VehiclePart vehiclePart = partUnit.detach();
            if (vehiclePart == null) {
                continue;
            }
            vehiclePart.setDestroyed();
            vehiclePart.setDeltaMovement(flingVelocity(vehicle, partUnit));
            vehicle.level().addFreshEntity(vehiclePart);
        }
    }

    /**
     * 部件初速度：沿自身 pivot 相对载具 center 的方向，附加少量随机与向上的分量。
     */
    private static Vec3 flingVelocity(AbstractVehicle vehicle, PartUnit<?> partUnit) {
        Vec3 center = vehicle.position().add(vehicle.centerOffset);
        Vec3 direction = partUnit.worldPivotPosition().subtract(center);
        var random = vehicle.level().random;
        if (direction.lengthSqr() < 1e-6) {
            direction = new Vec3(random.nextDouble() - 0.5, 0.2, random.nextDouble() - 0.5);
        }
        Vec3 randomOffset = new Vec3(
                (random.nextDouble() - 0.5) * 0.3,
                (random.nextDouble() - 0.5) * 0.05,
                (random.nextDouble() - 0.5) * 0.3
        );
        direction = direction.normalize().add(randomOffset).normalize();
        double speed = vehicle.physicsEngine.physicsInfo.destroyExplosionVelocity + random.nextDouble() * 0.2;
        return direction.scale(speed);
    }

}
