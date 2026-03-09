package org.ywzj.vehicle.vehicle.parts;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.custom.part.data.RadarUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RadarUnit extends RotatableUnit<RadarUnitData> {

    private Vec3 radarOffset = Vec3.ZERO;
    private float scanSectorAngle;
    private float maxDistance;
    private final HashMap<Entity, DetectedObject> detectedObjects = new HashMap<>();
    private Entity lockedEntity;
    private boolean yRotAdd = true;
    private boolean xRotAdd = true;

    public RadarUnit(int index, AbstractVehicle vehicle, RadarUnitData data) {
        super(index, vehicle, data);
        this.scanSectorAngle = data.getScanSectorAngle();
        this.maxDistance = data.getMaxDistance();
    }

    @Override
    public void buildStructure(Map<VehicleCubeGroup, VehicleCubeGroup> vehicleCubeGroupCopy) {
        super.buildStructure(vehicleCubeGroupCopy);
        if (structureGroup != null) {
            this.radarOffset = structureGroup.pivotOffset;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (vehicle.level().isClientSide()) {
            if (vehicle.hasPower()) {
                tickScan();
            } else {
                detectedObjects.clear();
            }
        }
    }

    protected void tickRot() {
        if (!vehicle.level().isClientSide()) {
            if (lockedEntity != null) {
                Vec3 radarPos = worldPosition(radarOffset);
                Vec2 rot = worldVecToLocalRot(lockedEntity.position().subtract(radarPos));
                if (yRotAdd) {
                    yAimRot = rot.y + scanSectorAngle / 4;
                } else {
                    yAimRot = rot.y - scanSectorAngle / 4;
                }
                if (Math.abs(yAimRot - yRot) < yRotSpeed) {
                    yRotAdd = !yRotAdd;
                }
            } else {
                if (yAimRot >= yRotMax || yAimRot <= yRotMin) {
                    yRotAdd = !yRotAdd;
                }
                if (xAimRot >= xRotMax || xAimRot <= xRotMin) {
                    xRotAdd = !xRotAdd;
                }
                yAimRot = Mth.clamp(Mth.wrapDegrees(yAimRot + (yRotAdd ? yRotSpeed : -yRotSpeed)), yRotMin, yRotMax);
                xAimRot = Mth.clamp(Mth.wrapDegrees(xAimRot + (xRotAdd ? xRotSpeed : -xRotSpeed)), xRotMin, xRotMax);
            }
        }
        super.tickRot();
    }

    public void tickScan() {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 radarPos = worldPosition(radarOffset);
        Vec3 scanVec = worldVec();

        //todo: 测试
//        Vec2 scanRot = worldRot();
//        float x1 = scanRot.x + scanSectorAngle / 2;
//        float x2 = scanRot.x - scanSectorAngle / 2;
//        Vec3 v1 = VectorUtil.calculateViewVector(x1, scanRot.y);
//        Vec3 v2 = VectorUtil.calculateViewVector(x2, scanRot.y);
//        for (int i = 0; i < 10; i++) {
//            DebugUtil.particle(vehicle.level(), radarPos.add(v1.scale(i)));
//            DebugUtil.particle(vehicle.level(), radarPos.add(v2.scale(i)));
//        }

        long timeNow = System.currentTimeMillis();
        long life = (long) (360 / yRotSpeed / 20 * 1000L);
        detectedObjects.values().removeIf(detectedObject -> {
            long removeTime = detectedObject.detectedTime + life;
            if (removeTime < timeNow) {
                return true;
            } else {
                detectedObject.detectedPosition = detectedObject.entity.getBoundingBox().getCenter();
                return false;
            }
        });
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            // 基础校验
            if (entity == camera.getEntity()
                    || entity.getVehicle() != null
                    || entity == this.vehicle
                    || !entity.isAlive()
                    || entity.isSpectator()
                    || entity.getBoundingBox().getSize() < 1
                    || entity.distanceTo(vehicle) > maxDistance) {
                continue;
            }
            // 在当前扫描范围
            Vec3 targetPos = entity.getBoundingBox().getCenter();
            Vec3 toTarget = targetPos.subtract(radarPos);
            Vec3 direction = toTarget.normalize();
            double dot = scanVec.dot(direction);
            if (Math.toDegrees(Math.acos(dot)) > scanSectorAngle / 2) {
                continue;
            }
            // 背景无回波
            ClipContext ctx = new ClipContext(
                    radarPos,
                    radarPos.add(direction.scale(512)),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    camera.getEntity()
            );
            if (minecraft.level.clip(ctx).getType() != HitResult.Type.MISS) {
                continue;
            }
            DetectedObject detectedObject = detectedObjects.get(entity);
            AABB aabb = entity.getBoundingBox();
            Vec3 detectedPosition = aabb.getCenter();
            if (detectedObject == null) {
                detectedObject = new DetectedObject();
                detectedObject.entity = entity;
                detectedObject.detectedPosition = detectedPosition;
                detectedObject.detectedTime = timeNow;
                detectedObjects.put(entity, detectedObject);
            } else {
                detectedObject.detectedPosition = detectedPosition;
                detectedObject.detectedTime = timeNow;
            }
        }
    }

    public Collection<DetectedObject> getDetectedEntities() {
        return detectedObjects.values();
    }

    public Entity getLockedEntity() {
        return lockedEntity;
    }

    public void setLockedEntity(Entity lockedEntity) {
        this.lockedEntity = lockedEntity;
        if (lockedEntity == null) {
            yRotAdd = true;
            xRotAdd = true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {}

    public static class DetectedObject {

        public Entity entity;
        public Vec3 detectedPosition;
        public Long detectedTime;

    }

}
