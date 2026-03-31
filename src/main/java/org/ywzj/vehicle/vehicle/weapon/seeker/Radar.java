package org.ywzj.vehicle.vehicle.weapon.seeker;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.ywzj.vehicle.api.entity.TargetObstruction;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class Radar {

    public static Entity checkTarget(Entity fromEntity, List<Entity> entities, Entity target) {
        if (!entities.contains(target)) {
            return null;
        }
        // 干扰物
        List<TargetObstruction> obstructions = entities.stream()
                .filter(entity -> entity instanceof TargetObstruction && entity.distanceTo(target) < 8)
                .map(TargetObstruction.class::cast)
                .toList();
        int rcsWeight = rcsWeight(fromEntity, target);
        int totalWeight = rcsWeight + obstructions.size();
        if (totalWeight == 0) {
            return target;
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(totalWeight);
        if (randomIndex < rcsWeight) {
            return target;
        } else {
            return (Entity) obstructions.get(randomIndex - rcsWeight);
        }
    }

    public static List<Entity> scanTargets(Entity radarOwner, Vec3 worldRadarPosition, double maxScanDistance, Function<Vec3, Boolean> check) {
        Level level = radarOwner.level();
        List<Entity> detectedEntities = new ArrayList<>();
        List<TargetObstruction> targetObstructions = new ArrayList<>();
        double maxScanDistanceSqr = maxScanDistance * maxScanDistance;
        AABB scanBox = new AABB(worldRadarPosition.subtract(maxScanDistance, maxScanDistance, maxScanDistance),
                worldRadarPosition.add(maxScanDistance, maxScanDistance, maxScanDistance));
        for (Entity entity : level.isClientSide() ? getClientLevelEntities(radarOwner, scanBox) : level.getEntities(radarOwner, scanBox)) {
            float rcs = 1;
            if (entity instanceof AbstractVehicle vehicle) {
                rcs = vehicle.physicsEngine.radarCrossSection;
            }
            if (entity.getVehicle() != null
                    || !entity.isAlive()
                    || entity instanceof PartEntity<?>
                    || entity.getBoundingBox().getSize() < 1
                    || entity.distanceToSqr(radarOwner) > maxScanDistanceSqr * rcs * rcs) {
                continue;
            }
            // 在当前扫描范围
            if (!check.apply(entity.getBoundingBox().getCenter())) {
                continue;
            }
            // 速度门
            if (entity.getDeltaMovement().subtract(radarOwner.getDeltaMovement()).length() < 0.1f) {
                continue;
            }
            // 背景无回波
            Vec3 entityPos = entity.position();
            Vec3 checkPos = entityPos.add(entityPos.subtract(worldRadarPosition).normalize().scale(128));
            if (checkPos.y < radarOwner.level().getHeight(Heightmap.Types.MOTION_BLOCKING, radarOwner.getBlockX(), radarOwner.getBlockZ())) {
                continue;
            }
            detectedEntities.add(entity);
            if (entity instanceof TargetObstruction targetObstruction) {
                targetObstructions.add(targetObstruction);
            }
        }
        // 干扰物
        detectedEntities.removeIf(target -> {
            if (!(target instanceof TargetObstruction)) {
                long nearby = targetObstructions.stream().filter(ob -> ((Entity) ob).distanceTo(target) < 8).count();
                int weight = rcsWeight(radarOwner, target);
                int totalWeight = weight + (int) nearby;
                if (totalWeight == 0) {
                    return false;
                }
                int randomIndex = ThreadLocalRandom.current().nextInt(totalWeight);
                return randomIndex >= weight;
            }
            return false;
        });
        return detectedEntities;
    }

    public static Entity findTarget(WeaponUnit weaponUnit, float fov) {
        RadarUnit radarUnit = weaponUnit.getRadarUnit();
        if (radarUnit == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Entity bestEntity = null;
        double minDegree = Double.MAX_VALUE;
        double maxScanDistanceSqr = radarUnit.getMaxScanDistance() * radarUnit.getMaxScanDistance();
        Vec3 worldedRadarPosition = radarUnit.worldRadarPosition();
        // 雷达已探测目标
        for (RadarUnit.DetectedObject detectedObject : radarUnit.getDetectedEntities().values()) {
            Entity entity = detectedObject.entity;
            float rcs = 1;
            if (entity instanceof AbstractVehicle vehicle) {
                rcs = vehicle.physicsEngine.radarCrossSection;
            }
            // 基础校验
            if (entity == camera.getEntity()
                    || entity.getVehicle() != null
                    || entity == weaponUnit.getVehicle()
                    || !entity.isAlive()
                    || entity.isSpectator()
                    || entity.getBoundingBox().getSize() < 1
                    || entity.position().distanceToSqr(worldedRadarPosition) > maxScanDistanceSqr * rcs * rcs) {
                continue;
            }
            Vec3 vLock = entity.getBoundingBox().getCenter().subtract(weaponUnit.worldPivotPosition());
            Vec3 vAim = weaponUnit.worldVec();
            double degree = Math.toDegrees(VectorUtil.angleBetween(vLock, vAim));
            if (degree <= fov && degree < minDegree) {
                minDegree = degree;
                bestEntity = entity;
            }
        }
        return bestEntity;
    }

    public static List<Entity> getClientLevelEntities(Entity radarOwner, AABB scanBox) {
        List<Entity> entities = new ArrayList<>();
        HashSet<Integer> entityIds = new HashSet<>();
        radarOwner.level().getEntities(radarOwner, scanBox).forEach(entity -> {
            entities.add(entity);
            entityIds.add(entity.getId());
        });
        entities.addAll(LocalVehiclePlayer.instance.serverEntities.values().stream()
                .filter(serverEntity -> !entityIds.contains(serverEntity.entity.getId()))
                .map(serverEntity -> serverEntity.entity)
                .toList());
        return entities;
    }

    public static int rcsWeight(Entity radarEntity, Entity target) {
        double v39 = Math.abs(target.getDeltaMovement().dot(target.position().subtract(radarEntity.position()).normalize()));
        float rcs = 1;
        if (target instanceof AbstractVehicle vehicle) {
            rcs = vehicle.physicsEngine.radarCrossSection;
        }
        // y = ln(1 + speed * rcs) * scale
        return (int) (Math.log1p(v39 * rcs) * 100);
    }

}
