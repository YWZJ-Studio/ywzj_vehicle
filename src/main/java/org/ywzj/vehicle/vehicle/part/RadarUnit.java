package org.ywzj.vehicle.vehicle.part;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.ywzj.vehicle.custom.part.data.RadarUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientRadarAction;
import org.ywzj.vehicle.network.message.ServerVehicleWarn;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.pojo.WarnType;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.weapon.seeker.Radar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RadarUnit extends RotatableUnit<RadarUnitData> {

    private Vec3 radarOffset = Vec3.ZERO;
    private String radarType;
    private float scanSectorAngle;
    private float maxScanDistance;
    private final HashMap<Integer, DetectedObject> detectedObjects = new HashMap<>();
    private Entity lockedEntity;
    private boolean yRotAdd = true;
    private boolean xRotAdd = true;
    private boolean on;
    private final ThreadPoolExecutor radarExecutor = new ThreadPoolExecutor(
            4,
            4,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    public RadarUnit(int index, AbstractVehicle vehicle, RadarUnitData data) {
        super(index, vehicle, data);
        this.radarType = data.getRadarType();
        this.scanSectorAngle = data.getScanSectorAngle();
        this.maxScanDistance = data.getMaxScanDistance();
        this.on = true;
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
        tickTargets();
        tickLock();
        if (vehicle.level().isClientSide()) {
            if (vehicle.hasPower() && on) {
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
                    yAimRot = rot.y + yRotSpeed / 2;
                } else {
                    yAimRot = rot.y - yRotSpeed / 2;
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

    public void tickTargets() {
        long timeNow = System.currentTimeMillis();
        float range = Math.min(360, yRotMax - yRotMin);
        long life = Math.max((long) (range / yRotSpeed / 20 * 1000L) * 2, 100);
        detectedObjects.values().removeIf(detectedObject -> detectedObject.detectedTime + life < timeNow);
        // 服务端通知雷达搜索给目标载具乘客
        if (!vehicle.level().isClientSide() && vehicle.tickCount % 20 == 0) {
            radarExecutor.execute(() -> {
                List<Entity> scannedEntities = Radar.scanTargets(vehicle, worldRadarPosition(), maxScanDistance, entityPos -> {
                    Vec2 aimRot = aimRot(entityPos);
                    return !(aimRot.y < yRotMin) && !(aimRot.y > yRotMax);
                });
                scannedEntities.forEach(scannedEntity -> {
                    if (scannedEntity instanceof AbstractVehicle toVehicle) {
                        ServerVehicleWarn serverVehicleWarn = new ServerVehicleWarn();
                        serverVehicleWarn.fromEntityId = this.vehicle.getId();
                        serverVehicleWarn.toEntityId = toVehicle.getId();
                        serverVehicleWarn.warnType = WarnType.RADAR_SEARCH;
                        serverVehicleWarn.info = getRadarType();
                        for (Entity entity : toVehicle.getPassengers()) {
                            if (entity instanceof ServerPlayer player) {
                                Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), serverVehicleWarn);
                            }
                        }
                    }
                });
            });
        }
    }

    public void tickScan() {
        if (parentPartUnit == null) {
            return;
        }
        if (LocalVehiclePlayer.instance.getPlayer() != parentPartUnit.getOwner()) {
            return;
        }
        // 雷达截获目标
        List<Entity> entities = Radar.detectTargets(vehicle, worldRadarPosition(), maxScanDistance, entityPos -> {
            Vec2 aimRot = aimRot(entityPos);
            return !(aimRot.y < yRotMin) && !(aimRot.y > yRotMax)
                    && !(Math.abs(aimRot.y - yRot) > yRotSpeed / 2)
                    && !(Math.abs(aimRot.x - xRot) > scanSectorAngle / 2);
        });
        entities.forEach(this::detect);
        // 客户端通知雷达截获给服务端
        for (DetectedObject detectedObject : detectedObjects.values()) {
            ClientRadarAction clientRadarAction = new ClientRadarAction();
            clientRadarAction.action = ClientRadarAction.Action.DETECT;
            clientRadarAction.toEntityId = detectedObject.entity.getId();
            Channel.CHANNEL.sendToServer(clientRadarAction);
        }
    }

    public void detect(Entity entity) {
        DetectedObject detectedObject = detectedObjects.get(entity.getId());
        if (detectedObject != null) {
            detectedObject.entity = entity;
        }
        AABB aabb = entity.getBoundingBox();
        Vec3 detectedPosition = aabb.getCenter();
        long timeNow = System.currentTimeMillis();
        if (detectedObject == null) {
            detectedObject = new DetectedObject();
            detectedObject.entity = entity;
            detectedObject.detectedPosition = detectedPosition;
            detectedObject.detectedTime = timeNow;
            detectedObjects.put(entity.getId(), detectedObject);
        } else {
            detectedObject.detectedPosition = detectedPosition;
            detectedObject.detectedTime = timeNow;
        }
    }

    public void tickLock() {
        if (vehicle.level().isClientSide()) {
            if (lockedEntity != null) {
                // 干扰物影响
                Entity checkEntity = Radar.checkTarget(vehicle, detectedObjects.values().stream()
                        .map(detectedObject -> detectedObject.entity).toList(), lockedEntity);
                if (checkEntity != lockedEntity) {
                    setLockedEntity(checkEntity);
                }
                // 雷达发生远程与本地实体切换
                if (lockedEntity != null) {
                    RadarUnit.DetectedObject detectedObject = getDetectedEntities().get(lockedEntity.getId());
                    if (detectedObject != null && detectedObject.entity != lockedEntity) {
                        setLockedEntity(detectedObject.entity);
                    }
                }
            }
        }
    }

    public void toggle(Boolean on) {
        if (on == null) {
            this.on = !this.on;
        } else {
            this.on = on;
        }
        if (!this.on) {
            setLockedEntity(null);
        }
    }

    public HashMap<Integer, DetectedObject> getDetectedEntities() {
        return detectedObjects;
    }

    public Vec3 worldRadarPosition() {
        return worldPosition(radarOffset);
    }

    public String getRadarType() {
        return radarType;
    }

    public float getScanSectorAngle() {
        return scanSectorAngle;
    }

    public float getMaxScanDistance() {
        return maxScanDistance;
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
        if (vehicle.level().isClientSide()) {
            // 将客户端锁定目标通知服务端
            ClientRadarAction clientRadarAction = new ClientRadarAction();
            clientRadarAction.action = ClientRadarAction.Action.LOCK;
            clientRadarAction.toEntityId = lockedEntity == null ? -1 : lockedEntity.getId();
            Channel.CHANNEL.sendToServer(clientRadarAction);
        }
    }

    public boolean isUiHide() {
        return data.isUiHide();
    }

    public boolean isOn() {
        return on;
    }

    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {}

    public static class DetectedObject {

        public Entity entity;
        public Vec3 detectedPosition;
        public Long detectedTime;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DetectedObject that = (DetectedObject) o;
            return Objects.equals(entity, that.entity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entity);
        }

    }

}
