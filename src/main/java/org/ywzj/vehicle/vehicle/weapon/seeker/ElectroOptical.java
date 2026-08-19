package org.ywzj.vehicle.vehicle.weapon.seeker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.api.entity.SightObstruction;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

public class ElectroOptical {

    public static Entity checkTarget(WeaponUnit weaponUnit, Entity target) {
        Vec3 checkStart = weaponUnit.worldPivotPosition();
        Vec3 checkEnd = target.getBoundingBox().getCenter();
        Level level = target.level();
        AbstractVehicle vehicle = weaponUnit.getVehicle();
        BlockHitResult result = level.clip(new ClipContext(checkStart, checkEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        // 锁定实体是否被不透光方块遮挡
        if (result.getType() != HitResult.Type.MISS) {
            BlockPos pos = result.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty() && state.canOcclude()) {
                return null;
            }
        }
        EntityHitResult entityHit = VectorUtil.hitEntity(vehicle, checkStart, checkEnd);
        if (entityHit != null) {
            Entity entity = entityHit.getEntity();
            // 锁定实体是否被视觉遮挡
            if (entity instanceof SightObstruction) {
                return null;
            }
        }
        return target;
    }

    public static Entity findTarget(WeaponUnit weaponUnit, Vec3 lookAtPos) {
        Vec3 opticalSightPosition = weaponUnit.worldOpticalSightPosition(1f);
        Vec3 direction = lookAtPos.subtract(opticalSightPosition).normalize();
        EntityHitResult entityHit = VectorUtil.hitEntity(weaponUnit.getVehicle(), opticalSightPosition, opticalSightPosition.add(direction.scale(LocalVehiclePlayer.renderDistance())));
        if (entityHit != null) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof SightObstruction) {
                return null;
            } else {
                return entity;
            }
        } else {
            return null;
        }
    }

}
