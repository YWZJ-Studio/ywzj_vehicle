package org.ywzj.vehicle.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EntityUtil {

    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator();

    @Nullable
    public static BulletHitResult findEntityOnPath(Projectile bulletEntity, Vec3 startVec, Vec3 endVec) {
        Vec3 hitPos = null;
        Entity hitEntity = null;
        boolean headshot = false;
        // 获取子弹 tick 路径上所有的实体
        List<Entity> entities = bulletEntity.level().getEntities(bulletEntity, bulletEntity.getBoundingBox().expandTowards(bulletEntity.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        Entity owner = bulletEntity.getOwner();
        for (Entity entity : entities) {
            // 禁止对自己造成伤害（如有需要可以增加 Config 开启对自己的伤害）
            if (!entity.equals(owner)) {
                // 射击无视自己的载具和该载具上的其他乘客
                if (owner != null && entity.isPassengerOfSameVehicle(owner)) {
                    continue;
                }
                BulletHitResult result = getHitResult(bulletEntity, entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                hitPos = result.getLocation();
                double distanceToHit = startVec.distanceTo(hitPos);
                if (entity.isAlive()) {
                    if (distanceToHit < closestDistance) {
                        hitEntity = entity;
                        closestDistance = distanceToHit;
                        headshot = result.isHeadshot();
                    }
                }
            }
        }
        return hitEntity != null ? new BulletHitResult(hitEntity, hitPos, headshot) : null;
    }

    @NotNull
    public static List<BulletHitResult> findEntitiesOnPath(Projectile bulletEntity, Vec3 startVec, Vec3 endVec) {
        List<BulletHitResult> hitEntities = new ArrayList<>();
        List<Entity> entities = bulletEntity.level().getEntities(bulletEntity, bulletEntity.getBoundingBox().expandTowards(bulletEntity.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        Entity owner = bulletEntity.getOwner();
        for (Entity entity : entities) {
            if (!entity.equals(owner)) {
                if (owner != null && entity.equals(owner.getVehicle())) {
                    continue;
                }
                BulletHitResult result = getHitResult(bulletEntity, entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                if (entity.isAlive()) {
                    hitEntities.add(result);
                }
            }
        }
        return hitEntities;
    }

    @Nullable
    protected static BulletHitResult getHitResult(Projectile bulletEntity, Entity entity, Vec3 startVec, Vec3 endVec) {
        if (entity instanceof AbstractVehicle) {
            Vec3 closestHitPos = VectorUtil.closestHitObbPosition(entity, startVec, endVec);
            if (closestHitPos != null) {
                return new BulletHitResult(bulletEntity, closestHitPos, false);
            } else {
                return null;
            }
        }
        AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, bulletEntity.getOwner());
        // 计算射线与实体 boundingBox 的交点
        Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);
        // 爆头判定
        if (hitPos == null) {
            return null;
        }
        Vec3 hitBoxPos = hitPos.subtract(entity.position());
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        // 没有配置的默认给一个
        boolean headshot = false;
        float eyeHeight = entity.getEyeHeight();
        if ((eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.25)) {
            headshot = true;
        }
        return new BulletHitResult(entity, hitPos, headshot);
    }

    public static boolean isOnBlockSurface(Entity entity, Vec3 pos) {
        BlockPos blockBelow = BlockPos.containing(pos.x, pos.y - 0.05, pos.z); // 稍微往下偏一点
        BlockState stateBelow = entity.level().getBlockState(blockBelow);
        if (stateBelow.isAir()) return false;
        VoxelShape shape = stateBelow.getCollisionShape(entity.level(), blockBelow);
        if (shape.isEmpty()) return false;
        double surfaceY = shape.max(Direction.Axis.Y) + blockBelow.getY();
        return pos.y - surfaceY <= 0.05;
    }

    public static double getGroundY(Level level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        while (blockPos.getY() > level.getMinBuildHeight() && level.getBlockState(blockPos).isAir()) {
            blockPos = blockPos.below();
        }
        return blockPos.getY() + 1.0;
    }

}
