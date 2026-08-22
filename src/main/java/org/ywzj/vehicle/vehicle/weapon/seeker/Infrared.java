package org.ywzj.vehicle.vehicle.weapon.seeker;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.ywzj.vehicle.api.entity.SightObstruction;
import org.ywzj.vehicle.api.entity.TargetObstruction;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.weapon.VehicleMissile;

import java.util.Optional;

public class Infrared {

    public static Entity checkTarget(WeaponUnit weaponUnit, Entity target) {
        Optional<AbstractVehicleWeapon<?>> weaponOptional = weaponUnit.getCurrentWeapon();
        Vec3 checkStart = weaponUnit.worldPivotPosition();
        Vec3 checkEnd = target.getBoundingBox().getCenter();
        if (weaponOptional.isPresent() && weaponOptional.get() instanceof VehicleMissile missile) {
            weaponUnit = missile.getWeaponUnit();
            // 目标是否仍在锁定框内
            checkStart = missile.getWeaponUnit().worldPivotPosition();
            Vec3 vLock = checkEnd.subtract(checkStart);
            Vec3 vAim = weaponUnit.worldVec();
            if (Math.toDegrees(VectorUtil.angleBetween(vLock, vAim)) > missile.getData().getSeekerFov()) {
                return null;
            }
        }
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
            // 锁定实体是否被干扰
            if (entity instanceof TargetObstruction) {
                return entity;
            }
        }
        return target;
    }

    public static Entity findTarget(WeaponUnit weaponUnit, float fov) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Entity bestEntity = null;
        AbstractVehicle vehicle = weaponUnit.getVehicle();
        Level level = vehicle.level();
        double minDegree = Double.MAX_VALUE;
        Vec3 worldPivotPosition = weaponUnit.worldPivotPosition();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            // 基础校验
            if (entity == camera.getEntity()
                    || entity.getVehicle() != null
                    || entity == weaponUnit.getVehicle()
                    || !entity.isAlive()
                    || entity instanceof PartEntity<?>
                    || entity.isSpectator()
                    || entity.getBoundingBox().getSize() < 1
                    || entity.position().distanceTo(worldPivotPosition) > 512) {
                continue;
            }
            Vec3 checkStart = weaponUnit.worldPivotPosition();
            Vec3 checkEnd = entity.getEyePosition();
            BlockHitResult result = level.clip(new ClipContext(checkStart, checkEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
            // 是否被不透光方块遮挡
            if (result.getType() != HitResult.Type.MISS) {
                BlockPos pos = result.getBlockPos();
                BlockState state = level.getBlockState(pos);
                if (!state.getCollisionShape(level, pos).isEmpty() && state.canOcclude()) {
                    continue;
                }
            }
            EntityHitResult entityHit = VectorUtil.hitEntity(vehicle, checkStart, checkEnd);
            if (entityHit != null) {
                // 是否被视觉遮挡
                if (entityHit.getEntity() instanceof SightObstruction) {
                    continue;
                }
            }
            Vec3 vLock = entity.getBoundingBox().getCenter().subtract(worldPivotPosition);
            Vec3 vAim = weaponUnit.worldVec();
            double degree = Math.toDegrees(VectorUtil.angleBetween(vLock, vAim));
            // 在锁定框内
            if (degree <= fov && degree < minDegree) {
                minDegree = degree;
                bestEntity = entity;
            }
        }
        return bestEntity;
    }

}
