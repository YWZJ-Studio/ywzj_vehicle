package org.ywzj.vehicle.vehicle.parts;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.api.entity.SightObstruction;
import org.ywzj.vehicle.api.entity.TargetObstruction;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.gui.VehicleCrossHairOverlay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.sync.SyncDataHolder;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.network.message.ServerVehicleFire;
import org.ywzj.vehicle.network.message.ServerVehicleWarn;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.pojo.Bolt;
import org.ywzj.vehicle.vehicle.pojo.WarnType;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

import java.util.*;

/**
 * 默认武器站实现<br/>
 * 武器站是一种有方向机与高低机，可发射多类武器的载具可动部件<br/>
 * 其中方向机与高低机的结构模型可相互独立运动<br/>
 * 一个武器站可关联有多个子武器站，并在火控上联动<br/>
 * 多个武器站可纵向堆叠，并在方向机上联动旋转<br/>
 */
public class WeaponUnit extends RotatableUnit<WeaponUnitData> {

    // 武器站枢轴偏移，为武器站枢轴相对于载具枢轴的偏移
    private Vec3 pivotOffset;
    // 武器站炮闩
    private final List<Bolt> bolts = new ArrayList<>();
    // 当前使用的炮闩
    private int currentBoltIndex;
    // 发射模式
    private WeaponUnitData.FiringMode firingMode;
    // 武器站光瞄偏移，为开镜视角下玩家的摄像机相对于武器站枢轴的偏移
    private final Vec3 opticalSightOffset;
    // 武器站操作员镜头偏移，为操作员视角下玩家的摄像机相对于武器站枢轴的偏移
    private final Vec3 operatorViewOffset;
    // 操作员是否随武器站转动
    private boolean operatorOnWeaponUnit = true;
    // 开镜类型
    public WeaponUnitData.OpticalSightType opticalSightType;
    // 开镜缩放倍率
    private final float zoomMin;
    private final float zoomMax;
    // 当前开镜缩放倍率
    private float zoom;
    // 本武器站从属的父武器站
    private WeaponUnit parentWeaponUnit;
    // 本武器站附属的子武器站
    private final List<WeaponUnit> subWeaponUnits = new ArrayList<>();
    // 火控
    private final WeaponUnitData.FireControlSensorType fireControlSensorType;
    private final WeaponUnitData.FireControlLockType fireControlLockType;
    private boolean stabilizer;
    private Vec3 aimLockPosition;
    private Entity aimLockEntity;
    private boolean parentWeaponUnitAim;
    private RadarUnit radarUnit;
    private boolean irSensorOn;
    private int irCoolingTick;
    // 第三人称准心样式
    public WeaponUnitData.CrosshairStyle crosshairStyle = WeaponUnitData.CrosshairStyle.CIRCLE;
    // OBB结构
    private VehicleCubeGroup xTurnGroup;
    // 武器与选射
    public final List<AbstractVehicleWeapon<?>> weapons = new ArrayList<>();
    public final List<AbstractVehicleWeapon<?>> independentWeapons = new ArrayList<>();
    public final List<AbstractVehicleWeapon<?>> indexedWeapons = new ArrayList<>();
    private int currentWeaponIndex = -1;
    public SyncDataHolder<Integer> currentWeaponIndexHolder;
    private VehicleSound irTrackAlarmSound;

    public WeaponUnit(int index, AbstractVehicle vehicle, WeaponUnitData data) {
        super(index, vehicle, data);
        this.pivotOffset = data.getPivotOffset();
        this.seatOffset = data.getSeatOffset();
        if (data.getBolts() != null) {
            this.bolts.addAll(data.getBolts());
        } else {
            this.bolts.add(new Bolt(Vec3.ZERO, 0, 0, 0));
        }
        this.firingMode = data.getFiringMode();
        this.parentWeaponUnitAim = data.isParentWeaponUnitAim();
        this.opticalSightOffset = data.getOpticalSightOffset();
        this.operatorViewOffset = data.getOperatorViewOffset();
        this.operatorOnWeaponUnit = data.isOperatorOnWeaponUnit();
        this.fireControlSensorType = data.getFireControlSensorType();
        this.fireControlLockType = data.getFireControlLockType();
        this.opticalSightType = data.getOpticalSightType();
        this.zoomMin = data.getZoomMin();
        this.zoomMax = data.getZoomMax();
        this.zoom = this.zoomMin;
        this.crosshairStyle = data.getCrosshairStyle();

        this.currentWeaponIndexHolder = this.getSyncData().define(
                SyncDataSerializers.INT,
                this::setCurrentWeaponIndex,
                this::getCurrentWeaponIndex,
                currentWeaponIndex
        );
        currentWeaponIndex = 0;
    }

    @Override
    public void buildStructure(Map<VehicleCubeGroup, VehicleCubeGroup> vehicleCubeGroupCopy) {
        super.buildStructure(vehicleCubeGroupCopy);
        this.xTurnGroup = vehicleCubeGroupCopy.get(data.getRawXTurnGroup());
    }

    public void switchWeapon(boolean next) {
        int size = weapons.size();
        this.getCurrentWeapon().ifPresent(
                AbstractVehicleWeapon::onSwitchFrom
        );
        this.currentWeaponIndex = (this.currentWeaponIndex + (next ? 1 : size - 1)) % size;
        this.getCurrentWeapon().ifPresent(
                AbstractVehicleWeapon::onSwitchTo
        );
    }

    public void initWeapon(int index) {
        if (index < 0 || index >= weapons.size() || index == currentWeaponIndex) {
            return;
        }
        this.currentWeaponIndex = index;
    }

    @Override
    public void combineAndInit(Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        super.combineAndInit(partUnitsView, vehicle);
        int index = 0;
        // 武器
        for (var weaponInfo : data.getWeapons()) {
            VehicleWeaponIndex<?, ?> vehicleWeaponIndex = CommonAssetsManager.vehicleWeaponManager().getIndex(weaponInfo.id).orElse(null);
            if (vehicleWeaponIndex != null) {
                WeaponUnit parent = this;
                AbstractVehicleWeapon<?> weapon;
                if (weaponInfo.partUnitId != null && partUnitsView.get(weaponInfo.partUnitId) instanceof WeaponUnit subWeaponUnit) {
                    subWeaponUnit.setParentWeaponUnit(parent);
                    parent.addSubWeaponUnit(subWeaponUnit);
                    weapon = vehicleWeaponIndex.create(vehicle, subWeaponUnit, index, weaponInfo.saveId);
                } else {
                    weapon = vehicleWeaponIndex.create(vehicle, parent, index, weaponInfo.saveId);
                }
                if (vehicleWeaponIndex.data().independent) {
                    this.independentWeapons.add(weapon);
                } else {
                    this.weapons.add(weapon);
                }
                weapon.defineSyncData(this.getSyncData());
                indexedWeapons.add(weapon);
                index++;
            }
        }
        // 雷达
        for (String subPartUnitId : data.getSubPartUnitIds()) {
            if (partUnitsView.get(subPartUnitId) instanceof RadarUnit radarUnit) {
                this.radarUnit = radarUnit;
                addSubPartUnit(radarUnit);
            }
        }
    }

    @Override
    public void tick() {
        if (vehicle.level().isClientSide()) {
            tickStabilizer();
            tickFireControl();
        }
        super.tick();
        this.getCurrentWeapon().ifPresent(AbstractVehicleWeapon::tick);
        independentWeapons.forEach(AbstractVehicleWeapon::tick);
    }

    @Override
    public void updateRot() {
        if (structureGroup != null) {
            structureGroup.rotation = new Quaternionf(structureGroup.baseRotation).mul(Axis.YN.rotationDegrees(yRot));
            if (xTurnGroup == structureGroup) {
                structureGroup.rotation = structureGroup.rotation.mul(Axis.XP.rotationDegrees(xRot));
                return;
            }
        }
        if (xTurnGroup != null) {
            xTurnGroup.rotation = new Quaternionf(xTurnGroup.baseRotation).mul(Axis.XP.rotationDegrees(xRot));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickStabilizer() {
        if (stabilizer && aimLockPosition != null && getOwner() == LocalVehiclePlayer.instance.getPlayer()) {
            if (aimLockEntity != null) {
                AABB aabb = aimLockEntity.getBoundingBox();
                aimLockPosition = aabb.getCenter();
            }
            aim(aimLockPosition);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickFireControl() {
        if (vehicle.getPassengers().isEmpty()) {
            return;
        }
        if (aimLockEntity != null) {
            Vec3 checkStart = worldPivotPosition();
            Vec3 checkEnd = aimLockEntity.position();
            BlockHitResult result = vehicle.level().clip(new ClipContext(checkStart, checkEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
            // 锁定实体是否被不透光方块遮挡
            if (result.getType() != HitResult.Type.MISS) {
                BlockPos pos = result.getBlockPos();
                BlockState state = vehicle.level().getBlockState(pos);
                if (state.isAir() || state.canOcclude()) {
                    setAimLockEntity(null);
                    return;
                }
            }
            EntityHitResult entityHit = VectorUtil.hitEntity(vehicle, checkStart, checkEnd);
            if (entityHit != null) {
                WeaponUnitData.FireControlSensorType sensorType = getFireControlSensorType();
                Entity entity = entityHit.getEntity();
                // 锁定实体是否被视觉遮挡
                if (entity instanceof SightObstruction
                        && (sensorType == WeaponUnitData.FireControlSensorType.IR || sensorType == WeaponUnitData.FireControlSensorType.EO)) {
                    setAimLockEntity(null);
                    stabilizer = false;
                    return;
                }
                // 锁定实体是否被干扰
                if (entity instanceof TargetObstruction) {
                    setAimLockEntity(entity);
                }
            }
            // 若为红外锁定，目标是否仍在锁定框内
            if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR) {
                double x = VehicleCrossHairOverlay.getScreenAimX();
                double y = VehicleCrossHairOverlay.getScreenAimY();
                Vec3 screenPos = VectorUtil.worldToScreen(aimLockEntity.getBoundingBox().getCenter());
                if (screenPos.z < 0) {
                    setAimLockEntity(null);
                    return;
                }
                double dx = screenPos.x - x;
                double dy = screenPos.y - y;
                double distSq = dx * dx + dy * dy;
                if (distSq > 64 * 64) {
                    setAimLockEntity(null);
                    return;
                }
            }
            // 锁定实体是否已消失
            if (aimLockEntity != null) {
                if (!aimLockEntity.isAlive()) {
                    setAimLockEntity(null);
                    stabilizer = false;
                }
            }
        }
        // 红外导引头开启并冷却后搜索并锁定目标
        else if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR) {
            if (isIrSensorOn()) {
                irCoolingTick += 1;
                if (irCoolingTick > 30) {
                    Minecraft minecraft = Minecraft.getInstance();
                    Camera camera = minecraft.gameRenderer.getMainCamera();
                    Entity bestEntity = null;
                    double minDistSq = Double.MAX_VALUE;
                    double x = VehicleCrossHairOverlay.getScreenAimX();
                    double y = VehicleCrossHairOverlay.getScreenAimY();
                    for (Entity entity : minecraft.level.entitiesForRendering()) {
                        // 基础校验
                        if (entity == camera.getEntity()
                                || entity.getVehicle() != null
                                || entity == this.vehicle
                                || !entity.isAlive()
                                || entity.isSpectator()
                                || entity.getBoundingBox().getSize() < 1
                                || entity.distanceTo(vehicle) > 256) {
                            continue;
                        }
                        Vec3 screenPos = VectorUtil.worldToScreen(entity.getBoundingBox().getCenter());
                        if (screenPos.z < 0) {
                            continue;
                        }
                        double dx = screenPos.x - x;
                        double dy = screenPos.y - y;
                        double distSq = dx * dx + dy * dy;
                        // 在锁定框内
                        if (distSq < 64 * 64 && distSq < minDistSq) {
                            minDistSq = distSq;
                            bestEntity = entity;
                        }
                    }
                    if (bestEntity != null) {
                        setAimLockEntity(bestEntity);
                        irSensorOn = false;
                        irCoolingTick = 0;
                    }
                }
            }
        }
    }

    public Bolt getCurrentBolt() {
        return bolts.get(currentBoltIndex);
    }

    public void shoot(int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (weaponIndex < indexedWeapons.size()) {
            AbstractVehicleWeapon<?> weapon = indexedWeapons.get(weaponIndex);
            if (MinecraftForge.EVENT_BUS.post(new VehicleFireEvent.Pre(vehicle, weapon, operator))) {
                return;
            }
            if (weapon.shoot(aimContexts, operator)) {
                MinecraftForge.EVENT_BUS.post(new VehicleFireEvent.Post(vehicle, weapon, operator));
                int entityId = vehicle.getId();
                int operatorId = operator == null ? -1 : operator.getId();
                ServerVehicleFire packet = new ServerVehicleFire(entityId, operatorId, index, weapon.getIndex());
                Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle), packet);
            }
        }
    }

    public void aim(Vec3 worldPos) {
        Vec2 rot = aimRot(worldPos);
        Bolt bolt = getCurrentBolt();
        rot = new Vec2(rot.x - bolt.xRot(), rot.y + bolt.yRot());
        if (xAimRot != rot.x || yAimRot != rot.y) {
            if (vehicle.level().isClientSide()) {
                ClientVehicleAction control = new ClientVehicleAction();
                control.vehicleEntityId = vehicle.getId();
                control.partUnitIndex = index;
                control.xAimRot = rot.x;
                control.yAimRot = rot.y;
                Channel.CHANNEL.sendToServer(control);
            } else {
                xAimRot = rot.x;
                yAimRot = rot.y;
            }
        }
        subWeaponUnits.forEach(weaponUnit -> weaponUnit.aim(worldPos));
    }

    public Vec2 aimRot(Vec3 worldPosition) {
        Vec3 fromWorldPosition = vehicle.relativeRotPos(vehicle.position().add(xTurnGroup.globalTransform().pivot()), false);
        Vec3 worldAim = new Vec3(worldPosition.x - fromWorldPosition.x, worldPosition.y - fromWorldPosition.y, worldPosition.z - fromWorldPosition.z);
        return vecToRot(worldAim);
    }

    public Vec3 aimHitPosition() {
        List<Vec3> positions = aimContexts().stream().map(context -> context.position).toList();
        double x = positions.stream().mapToDouble(pos -> pos.x).average().orElse(0);
        double y = positions.stream().mapToDouble(pos -> pos.y).average().orElse(0);
        double z = positions.stream().mapToDouble(pos -> pos.z).average().orElse(0);
        AimContext aimContext = aimContext();
        Vec3 start = new Vec3(x, y, z);
        Vec3 end = start.add(VectorUtil.rotToVec(aimContext.direction.x, aimContext.direction.y).normalize().scale(256));
        return VectorUtil.hitPosition(vehicle, start, end);
    }

    public List<AimContext> aimContexts() {
        List<AimContext> positions = new ArrayList<>();
        for (Bolt bolt : bolts) {
            positions.add(aimContext(bolt));
        }
        return positions;
    }

    public AimContext aimContext() {
        return aimContext(getCurrentBolt());
    }

    public AimContext aimContext(Bolt bolt) {
        AimContext aimContext = new AimContext();
        if (xTurnGroup == null) {
            aimContext.position = this.vehicle.position();
            aimContext.direction = new Vec2(this.vehicle.getXRot(), this.vehicle.getYRot());
            return aimContext;
        }
        VehicleCubeGroup.GlobalTransform globalTransform = xTurnGroup.globalTransform();
        Quaternionf rotation = globalTransform.rotation();
        Quaternionf vehicleRotation = vehicle.rotYXZ();
        Vec3 boltPosition = worldBoltPosition(bolt, vehicleRotation, globalTransform);
        Vector3f worldRot = new Vector3f();
        vehicleRotation.mul(rotation).getEulerAnglesYXZ(worldRot);
        aimContext.direction = new Vec2((float) Math.toDegrees(worldRot.x) + bolt.xRot(), (float) Math.toDegrees(-worldRot.y) + bolt.yRot());
        Vec3 direction = VectorUtil.rotToVec(aimContext.direction.x, aimContext.direction.y);
        aimContext.position = boltPosition.add(direction.scale(bolt.barrelLength()));
        return aimContext;
    }

    public Vec3 worldCurrentBoltPosition() {
        Bolt bolt = getCurrentBolt();
        VehicleCubeGroup.GlobalTransform globalTransform = xTurnGroup.globalTransform();
        Quaternionf vehicleRotation = vehicle.rotYXZ();
        return worldBoltPosition(bolt, vehicleRotation, globalTransform);
    }

    public Vec3 worldBoltPosition(Bolt bolt, Quaternionf vehicleRotation, VehicleCubeGroup.GlobalTransform globalTransform) {
        Quaternionf rotation = globalTransform.rotation();
        Vector3f boltOffset = new Vector3f((float) bolt.offset().x, (float) bolt.offset().y, (float) bolt.offset().z);
        rotation.transform(boltOffset);
        Vec3 pivot = globalTransform.pivot();
        Vector3f rotatedBoltOffset = vehicleRotation.transform(pivot.add(boltOffset.x, boltOffset.y, boltOffset.z).toVector3f());
        return vehicle.position().add(rotatedBoltOffset.x, rotatedBoltOffset.y, rotatedBoltOffset.z);
    }

    public Vec3 worldPivotPosition() {
        return worldPositionWithBaseRot(pivotOffset);
    }

    public Vec3 worldOpticalSightPosition() {
        if (opticalSightOffset == null) {
            return worldOwnerViewPosition();
        }
        return worldPosition(pivotOffset.add(opticalSightOffset));
    }

    @Override
    public Vec3 worldOwnerViewPosition() {
        if (!operatorOnWeaponUnit && LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
            return worldPositionWithBaseRot(pivotOffset.add(operatorViewOffset));
        }
        if (operatorViewOffset == null) {
            float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
            return worldPosition(pivotOffset.add(new Vec3(0, eyeHeight, 0)));
        }
        return worldPosition(pivotOffset.add(operatorViewOffset));
    }

    @Override
    public Vec3 worldSeatPosition() {
        float eyeHeight = getOwner() == null ? 2 : owner.getEyeHeight();
        if (!operatorOnWeaponUnit) {
            return worldPositionWithBaseRot(new Vec3(seatOffset.x, seatOffset.y  - eyeHeight, seatOffset.z));
        }
        return worldPositionWithSelfRot(new Vec3(seatOffset.x, seatOffset.y  - eyeHeight, seatOffset.z));
    }

    public List<Bolt> getBolts() {
        return bolts;
    }

    public WeaponUnitData.FiringMode getFiringMode() {
        return firingMode;
    }

    public void setFiringMode(WeaponUnitData.FiringMode firingMode) {
        this.firingMode = firingMode;
    }

    public void countFire(int times) {
        this.currentBoltIndex = (this.currentBoltIndex + times) % bolts.size();
    }

    public WeaponUnitData.OpticalSightType getOpticalSightType() {
        return opticalSightType;
    }

    public float getZoom() {
        return zoom;
    }

    public void switchZoom() {
        if (zoom == zoomMax) {
            zoom = zoomMin;
        } else {
            zoom = zoomMax;
        }
    }

    public boolean isStabilizerOn() {
        return stabilizer;
    }

    @OnlyIn(Dist.CLIENT)
    public void switchStabilizer() {
        stabilizer = !stabilizer;
        if (stabilizer) {
            fireControlLock();
        } else {
            setAimLockEntity(null);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void fireControlLock() {
        if (aimLockEntity != null) {
            setAimLockEntity(null);
            stabilizer = false;
            return;
        }
        WeaponUnitData.FireControlLockType lockType = getFireControlLockType();
        WeaponUnitData.FireControlSensorType sensorType = getFireControlSensorType();
        if (lockType == WeaponUnitData.FireControlLockType.AIM_HIT
                || lockType == WeaponUnitData.FireControlLockType.NONE) {
            // 稳定器
            if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON
                    || LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                aimLockPosition = LocalVehiclePlayer.instance.cameraAimHit(-LocalVehiclePlayer.CAMERA_UPWARD_ANGLE, 0);
            } else if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                aimLockPosition = LocalVehiclePlayer.instance.cameraAimHit(0, 0);
            }
            // 光电锁定
            if (sensorType == WeaponUnitData.FireControlSensorType.EO
                    && lockType == WeaponUnitData.FireControlLockType.AIM_HIT) {
                EntityHitResult entityHit = VectorUtil.hitEntity(vehicle, worldPivotPosition(), aimLockPosition);
                if (entityHit != null) {
                    Entity entity = entityHit.getEntity();
                    if (entity instanceof SightObstruction) {
                        setAimLockEntity(null);
                    } else {
                        setAimLockEntity(entity);
                        stabilizer = true;
                    }
                } else {
                    setAimLockEntity(null);
                }
            }
        }
        // 范围锁定
        else if (lockType == WeaponUnitData.FireControlLockType.AIM_FRUSTUM) {
            if (sensorType == WeaponUnitData.FireControlSensorType.RF && radarUnit != null) {
                double x = VehicleCrossHairOverlay.getScreenAimX();
                double y = VehicleCrossHairOverlay.getScreenAimY();
                List<Entity> aimDetectedEntities = radarUnit.getDetectedEntities().stream().sorted(Comparator.comparingDouble(detectedObject -> {
                    Vec3 screenPos = VectorUtil.worldToScreen(detectedObject.detectedPosition);
                    double dx = screenPos.x - x;
                    double dy = screenPos.y - y;
                    return dx * dx + dy * dy;
                })).map(detectedObject -> detectedObject.entity).toList();
                if (!aimDetectedEntities.isEmpty()) {
                    Vec3 screenPos = VectorUtil.worldToScreen(aimDetectedEntities.get(0).position());
                    double dx = screenPos.x - x;
                    double dy = screenPos.y - y;
                    if (dx * dx + dy * dy < 64 * 64) {
                        setAimLockEntity(aimDetectedEntities.get(0));
                    }
                }
            } else if (sensorType == WeaponUnitData.FireControlSensorType.IR) {
                irSensorOn = !irSensorOn;
                irCoolingTick = 0;
                if (aimLockEntity != null) {
                    setAimLockEntity(null);
                }
            }
        }
    }

    public Vec3 getAimLockPosition() {
        return aimLockPosition;
    }

    public void setAimLockPosition(Vec3 aimLockPosition) {
        this.aimLockPosition = aimLockPosition;
    }

    public Collection<RadarUnit.DetectedObject> getRadarDetectedEntities() {
        if (radarUnit != null) {
            return radarUnit.getDetectedEntities();
        }
        return List.of();
    }

    public Entity getAimLockEntity() {
        return aimLockEntity;
    }

    public void setAimLockEntity(Entity aimLockEntity) {
        Entity lastAimLockEntity = this.aimLockEntity;
        this.aimLockEntity = aimLockEntity;
        if (radarUnit != null) {
            radarUnit.setLockedEntity(aimLockEntity);
        }
        if (vehicle.level().isClientSide()) {
            // 红外锁定提示
            if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR) {
                if (irTrackAlarmSound != null && aimLockEntity == null) {
                    irTrackAlarmSound.stop();
                    irTrackAlarmSound = null;
                }
                if (irTrackAlarmSound == null && aimLockEntity != null) {
                    irTrackAlarmSound = new VehicleSound(AllSounds.IR_TRACK_ALARM.get(), 1f, 1f, 1f, true, 50, false, false, vehicle.getId());
                    irTrackAlarmSound.play();
                }
            }
            // 将客户端锁定目标通知服务端
            ClientVehicleAction action = new ClientVehicleAction();
            action.vehicleEntityId = vehicle.getId();
            action.lockEntity = true;
            action.lockedEntityId = aimLockEntity == null ? -1 : aimLockEntity.getId();
            Channel.CHANNEL.sendToServer(action);
        } else {
            // 通知雷达锁定与脱锁给目标载具乘客
            if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.RF) {
                if (lastAimLockEntity != null) {
                    ServerVehicleWarn packet = new ServerVehicleWarn(vehicle.getId(), lastAimLockEntity.getId(), WarnType.RADAR_LOCK, false);
                    Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle), packet);
                }
                if (aimLockEntity != null) {
                    ServerVehicleWarn packet = new ServerVehicleWarn(vehicle.getId(), aimLockEntity.getId(), WarnType.RADAR_LOCK, true);
                    Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle), packet);
                }
            }
        }
    }

    @Override
    public LivingEntity getOwner() {
        if (parentWeaponUnit != null) {
            return parentWeaponUnit.getOwner();
        }
        return super.getOwner();
    }

    public boolean isOperatorOnWeaponUnit() {
        return operatorOnWeaponUnit;
    }

    public void setOperatorOnWeaponUnit(boolean operatorOnWeaponUnit) {
        this.operatorOnWeaponUnit = operatorOnWeaponUnit;
    }

    public WeaponUnit getParentWeaponUnit() {
        return parentWeaponUnit;
    }

    public void setParentWeaponUnit(WeaponUnit parentWeaponUnit) {
        this.parentWeaponUnit = parentWeaponUnit;
    }

    public void addSubWeaponUnit(WeaponUnit subWeaponUnit) {
        this.subWeaponUnits.add(subWeaponUnit);
    }

    public List<WeaponUnit> getSubWeaponUnits() {
        return subWeaponUnits;
    }

    public WeaponUnitData.FireControlSensorType getFireControlSensorType() {
        Optional<AbstractVehicleWeapon<?>> weaponOptional = getCurrentWeapon();
        if (weaponOptional.isPresent()) {
            WeaponUnit weaponUnit = weaponOptional.get().getWeaponUnit();
            if (weaponUnit == this) {
                return fireControlSensorType;
            }
            return weaponOptional.get().getWeaponUnit().getFireControlSensorType();
        }
        return fireControlSensorType;
    }

    public WeaponUnitData.FireControlLockType getFireControlLockType() {
        Optional<AbstractVehicleWeapon<?>> weaponOptional = getCurrentWeapon();
        if (weaponOptional.isPresent()) {
            WeaponUnit weaponUnit = weaponOptional.get().getWeaponUnit();
            if (weaponUnit == this) {
                return fireControlLockType;
            }
            return weaponUnit.getFireControlLockType();
        }
        return fireControlLockType;
    }

    public boolean isParentWeaponUnitAim() {
        return parentWeaponUnitAim;
    }

    public void setParentWeaponUnitAim(boolean parentWeaponUnitAim) {
        this.parentWeaponUnitAim = parentWeaponUnitAim;
    }

    public boolean isIrSensorOn() {
        return irSensorOn;
    }

    public int getIrCoolingTick() {
        return irCoolingTick;
    }

    public Optional<AbstractVehicleWeapon<?>> getCurrentWeapon() {
        if (currentWeaponIndex >= 0 && currentWeaponIndex < weapons.size()) {
            return Optional.of(weapons.get(currentWeaponIndex));
        }
        return Optional.empty();
    }

    public void setCurrentWeaponIndex(int index) {
        this.currentWeaponIndex = index;
    }

    public int getCurrentWeaponIndex() {
        return currentWeaponIndex;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putInt("CurrentWeaponIndex", currentWeaponIndex);
        CompoundTag weaponTag = new CompoundTag();
        this.weapons.forEach(weapon -> {
            CompoundTag tag1 = weapon.serializeNBT();
            if (tag1.isEmpty()) {
                return;
            }
            weaponTag.put(weapon.getSerializeId(), tag1);
        });
        tag.put("WeaponTag", weaponTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        this.initWeapon(nbt.getInt("CurrentWeaponIndex"));
        CompoundTag weaponTag = nbt.getCompound("WeaponTag");
        this.weapons.forEach(weapon -> {
            if (weaponTag.contains(weapon.getSerializeId(), Tag.TAG_COMPOUND)) {
                weapon.deserializeNBT(weaponTag.getCompound(weapon.getSerializeId()));
            }
        });
    }

    @Deprecated
    public WeaponUnit(String id, int index, AbstractVehicle vehicle,
                      Vec3 pivotOffset, float barrelLength,
                      Vec3 opticalSightOffset, Vec3 operatorViewOffset, Vec3 seatOffset, WeaponUnit baseWeaponUnit) {
        super(id, index, vehicle);

        this.zoomMin = 1;
        this.zoomMax = 8;
        this.zoom = this.zoomMin;
        this.opticalSightType = WeaponUnitData.OpticalSightType.CRT;
        this.firingMode = WeaponUnitData.FiringMode.RIPPLE;

        if (pivotOffset != null) {
            this.pivotOffset = pivotOffset;
        }
        this.bolts.add(new Bolt(Vec3.ZERO, barrelLength, 0f, 0f));
        this.opticalSightOffset = opticalSightOffset;
        this.operatorViewOffset = operatorViewOffset;
        this.seatOffset = seatOffset;
        this.baseRotatableUnit = baseWeaponUnit;

        this.fireControlSensorType = WeaponUnitData.FireControlSensorType.NONE;
        this.fireControlLockType = WeaponUnitData.FireControlLockType.NONE;

        currentWeaponIndex = 0;
    }

    @Deprecated
    @Override
    public void initStructureModel(String name) {
        BedrockModel model = CommonAssetsManager.structureModelManager().getStructureModel(vehicle.getStructureModel()).orElse(null);
        if (model == null) {
            return;
        }
        var yTurnBone = model.getBoneMap().get(name);
        var xTurnBone = model.getBoneMap().get(name + "_barrel");
        if (yTurnBone != null && xTurnBone != null) {
//            this.pivotOffset = new Vec3(yTurnBone.x / 16, xTurnBone.y / 16, yTurnBone.z / 16);
//            var cubes = xTurnBone.cubes.stream().map(c -> (BedrockCubePerFace) c).toList();
//            var barrelCube = cubes.stream()
//                    .max(Comparator.comparingDouble(c -> c.depth() * c.width() * c.height()))
//                    .orElse(null);
//            if (barrelCube != null) {
//                double barrelHalfLength = new Vec3(
//                        xTurnBone.x / 16 - yTurnBone.x / 16 + barrelCube.x() + barrelCube.width() / 2,
//                        barrelCube.y() + barrelCube.height() / 2,
//                        xTurnBone.z / 16 - yTurnBone.z / 16 + barrelCube.z() + barrelCube.depth() / 2
//                ).length();
//            }
        }
//        this.yTurnUnitOBBs = PartUnitData.collectOBBs(yTurnBone);
//        this.xTurnUnitOBBs = PartUnitData.collectOBBs(xTurnBone);
    }

    @Deprecated
    @Override
    protected void initOBBs() {
//        this.partCubeOBBs.addAll(xTurnUnitOBBs);
//        this.partCubeOBBs.addAll(yTurnUnitOBBs);
    }

}
