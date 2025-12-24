package org.ywzj.vehicle.vehicle.parts;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
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
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector4d;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.api.entity.RadarObstruction;
import org.ywzj.vehicle.api.entity.SightObstruction;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.client.gui.VehicleCrossHairOverlay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
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
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;
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
    // 雷达
    private RadarUnit radarUnit;
    // 火控
    public WeaponUnitData.FireControlSensorType fireControlSensorType = WeaponUnitData.FireControlSensorType.NONE;
    public WeaponUnitData.FireControlLockType fireControlLockType;
    private boolean stabilizer;
    private Vec3 aimLockPosition;
    private Entity aimLockEntity;
    private boolean parentWeaponUnitAim;
    // 第三人称准心样式
    public WeaponUnitData.CrosshairStyle crosshairStyle = WeaponUnitData.CrosshairStyle.CIRCLE;
    // OBB结构
    private List<VehicleBedrockCubeOBB> yTurnUnitOBBs;
    private List<VehicleBedrockCubeOBB> xTurnUnitOBBs;
    // 武器与选射
    public final List<AbstractVehicleWeapon<?>> weapons = new ArrayList<>();
    public final List<AbstractVehicleWeapon<?>> independentWeapons = new ArrayList<>();
    public final List<AbstractVehicleWeapon<?>> indexedWeapons = new ArrayList<>();
    private int currentWeaponIndex = -1;
    public SyncDataHolder<Integer> currentWeaponIndexHolder;

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

        this.yTurnUnitOBBs = data.getYTurnUnitOBBs();
        this.xTurnUnitOBBs = data.getXTurnUnitOBBs();
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

        currentWeaponIndex = 0;
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
    public void updateOBBs() {
        updateOBBs(yTurnUnitOBBs, false);
        updateOBBs(xTurnUnitOBBs, true);
    }

    @OnlyIn(Dist.CLIENT)
    public void tickStabilizer() {
        if (stabilizer && aimLockPosition != null) {
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
                    setAimLockEntity(null, WeaponUnitData.FireControlSensorType.NONE);
                    return;
                }
            }
            EntityHitResult entityHit = VectorUtil.hitEntity(vehicle, checkStart, checkEnd);
            if (entityHit != null) {
                Entity entity = entityHit.getEntity();
                // 锁定实体是否被视觉遮挡
                if (entity instanceof SightObstruction && fireControlSensorType == WeaponUnitData.FireControlSensorType.IR) {
                    setAimLockEntity(null, WeaponUnitData.FireControlSensorType.NONE);
                    stabilizer = false;
                    return;
                }
                // 锁定实体是否被干扰
                if (entity instanceof RadarObstruction && fireControlSensorType == WeaponUnitData.FireControlSensorType.RF) {
                    setAimLockEntity(entity, WeaponUnitData.FireControlSensorType.RF);
                }
            }
            // 锁定实体是否已消失
            if (aimLockEntity != null) {
                if (!aimLockEntity.isAlive()) {
                    setAimLockEntity(null, WeaponUnitData.FireControlSensorType.NONE);
                    stabilizer = false;
                }
            }
        }
    }

    @Override
    public List<VehicleBedrockCubeOBB> getUnitBedrockCubeOBBs() {
        List<VehicleBedrockCubeOBB> unitBedrockCubeOBBs = new ArrayList<>(yTurnUnitOBBs.size() + xTurnUnitOBBs.size());
        unitBedrockCubeOBBs.addAll(yTurnUnitOBBs);
        unitBedrockCubeOBBs.addAll(xTurnUnitOBBs);
        return unitBedrockCubeOBBs;
    }

    @Override
    public List<OBB> getOBBs() {
        List<OBB> unitOBBs = new ArrayList<>(yTurnUnitOBBs.size() + xTurnUnitOBBs.size());
        yTurnUnitOBBs.forEach(unitOBB -> unitOBBs.add(unitOBB.obb()));
        xTurnUnitOBBs.forEach(unitOBB -> unitOBBs.add(unitOBB.obb()));
        return unitOBBs;
    }

    public void updateOBBs(List<VehicleBedrockCubeOBB> unitOBBs, boolean isBarrel) {
        for (VehicleBedrockCubeOBB unitBedrockCubeOBB : unitOBBs) {
            OBB obb = unitBedrockCubeOBB.obb();
            Quaternionf rot = new Quaternionf();
            rot.rotateY(Math.toRadians(-combineYRot()));
            if (isBarrel) {
                rot.rotateX(Math.toRadians(xRot));
                Vec3 barrelCenterOffset = rotatedOffsetWithSelfRot(unitBedrockCubeOBB.offset());
                Vec3 boltOffset = new Vec3(unitBedrockCubeOBB.offset().x, unitBedrockCubeOBB.offset().y, unitBedrockCubeOBB.boneZ / 16);
                Vec3 barrelPivotOffset = rotatedOffsetWithSelfRot(boltOffset);
                Vec3 rel = barrelCenterOffset.subtract(barrelPivotOffset);
                double len = rel.length();
                float xRotR = Math.toRadians(xRot);
                double cos = Math.cos(xRotR);
                double sin = Math.sin(xRotR);
                Vec3 v = rel.scale(cos);
                double x = v.x;
                double y = -len * sin;
                double z = v.z;
                obb.setCenter(vehicle.relativeRotPos(vehicle.position().add(new Vec3(x, y, z).add(barrelPivotOffset)), false).toVector3f());
            } else {
                obb.setCenter(worldPosition(unitBedrockCubeOBB.offset()).toVector3f());
            }
            Quaternionf selfRot = new Quaternionf(unitBedrockCubeOBB.selfRot());
            obb.setRotation(vehicle.rotYXZ().mul(rot).mul(selfRot));
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

    public Vec2 aimRot(Vec3 worldPos) {
        Vec3 fromWorldPos = worldCurrentBoltPosition();
        Vec3 worldAim = new Vec3(worldPos.x - fromWorldPos.x, worldPos.y - fromWorldPos.y, worldPos.z - fromWorldPos.z);
        return vecToRot(worldAim);
    }

    public Vec3 aimHitPosition() {
        Vec3 start = worldPivotPosition();
        Vec3 direction = worldVec().normalize();
        Vec3 end = start.add(direction.scale(256));
        return VectorUtil.hitPosition(vehicle, start, end);
    }

    public List<AimContext> aimContexts() {
        List<AimContext> positions = new ArrayList<>();
        for (int boltIndex = 0; boltIndex < bolts.size(); boltIndex += 1) {
            positions.add(aimContext(boltIndex));
        }
        return positions;
    }

    public AimContext aimContext() {
        return aimContext(currentBoltIndex);
    }

    public AimContext aimContext(int boltIndex) {
        Bolt bolt = bolts.get(boltIndex < 0 || boltIndex >= bolts.size() ? currentBoltIndex : boltIndex);
        Vec2 direction = worldRot(xRot + bolt.xRot(), yRot - bolt.yRot());
        Vec3 worldVec = VectorUtil.calculateViewVector(direction.x, direction.y);
        Vec3 barrelOffset = worldVec.normalize().scale(bolt.barrelLength());
        Vec3 position = worldPosition(pivotOffset.add(bolt.offset())).add(barrelOffset);
        AimContext aimContext = new AimContext();
        aimContext.position = position;
        aimContext.direction = direction;
        return aimContext;
    }

    public Vec3 worldCurrentBoltPosition() {
        Bolt bolt = bolts.get(currentBoltIndex);
        return worldPosition(pivotOffset.add(bolt.offset()));
    }

    public Vec3 worldPivotPosition() {
        Vector4d offset = rotatedOffsetWithBaseRot(this, this.pivotOffset.x, this.pivotOffset.z);
        Vec3 boltPosition = vehicle.position().add(new Vec3(offset.x, pivotOffset.y, offset.y));
        return vehicle.relativeRotPos(boltPosition, false);
    }

    public Vec3 worldOpticalSightPosition() {
        if (opticalSightOffset == null) {
            return worldOwnerViewPosition();
        }
        return worldPosition(pivotOffset.add(opticalSightOffset));
    }

    @Override
    public Vec3 worldOwnerViewPosition() {
        if (!operatorOnWeaponUnit) {
            Vec3 offsetFromVehicle = pivotOffset.add(operatorViewOffset);
            Vector4d offsetWithBaseRot = rotatedOffsetWithBaseRot(this, offsetFromVehicle.x, offsetFromVehicle.z);
            return vehicle.relativeRotPos(vehicle.position().add(new Vec3(offsetWithBaseRot.z, offsetFromVehicle.y, offsetWithBaseRot.w)), false);
        }
        if (operatorViewOffset == null) {
            float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
            return worldPosition(pivotOffset.add(new Vec3(0, eyeHeight, 0)));
        }
        return worldPosition(pivotOffset.add(operatorViewOffset));
    }

    @Override
    public Vec3 worldSeatPosition() {
        float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
        if (!operatorOnWeaponUnit) {
            Vector4d offset = rotatedOffsetWithBaseRot(this, seatOffset.x, seatOffset.z);
            return vehicle.relativeRotPos(vehicle.position().add(offset.z, seatOffset.y - eyeHeight, offset.w), false);
        }
        Vec3 offset = rotatedOffsetWithSelfRot(this.seatOffset);
        return vehicle.relativeRotPos(vehicle.position().add(offset.x, seatOffset.y - eyeHeight, offset.z), false);
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
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void fireControlLock() {
        getCurrentWeapon().ifPresent(vehicleWeapon -> {
            WeaponUnit currentWeaponUnit = vehicleWeapon.getWeaponUnit();
            if (aimLockEntity != null) {
                setAimLockEntity(null, WeaponUnitData.FireControlSensorType.NONE);
                stabilizer = false;
                return;
            }
            if (currentWeaponUnit.fireControlLockType == WeaponUnitData.FireControlLockType.AIM_HIT
                    || currentWeaponUnit.fireControlLockType == WeaponUnitData.FireControlLockType.NONE) {
                // 稳定器
                if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON
                        || LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                    aimLockPosition = LocalVehiclePlayer.instance.cameraAimHit(-LocalVehiclePlayer.CAMERA_UPWARD_ANGLE, 0);
                } else if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                    aimLockPosition = LocalVehiclePlayer.instance.cameraAimHit(0, 0);
                }
                // 瞄准锁定
                if (currentWeaponUnit.fireControlLockType == WeaponUnitData.FireControlLockType.AIM_HIT) {
                    EntityHitResult entityHit = VectorUtil.hitEntity(vehicle, worldPivotPosition(), aimLockPosition);
                    if (entityHit != null) {
                        Entity entity = entityHit.getEntity();
                        if (entity instanceof SightObstruction) {
                            setAimLockEntity(null, WeaponUnitData.FireControlSensorType.NONE);
                        } else {
                            setAimLockEntity(entity, WeaponUnitData.FireControlSensorType.IR);
                            stabilizer = true;
                        }
                    } else {
                        setAimLockEntity(null, WeaponUnitData.FireControlSensorType.NONE);
                    }
                }
            }
            // 范围锁定
            else if (currentWeaponUnit.fireControlLockType == WeaponUnitData.FireControlLockType.AIM_FRUSTUM) {
                if (radarUnit != null) {
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
                            setAimLockEntity(aimDetectedEntities.get(0), WeaponUnitData.FireControlSensorType.RF);
                        }
                    }
                }
            }
        });
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

    public void setAimLockEntity(Entity aimLockEntity, WeaponUnitData.FireControlSensorType fireControlSensorType) {
        Entity lastAimLockEntity = this.aimLockEntity;
        WeaponUnitData.FireControlSensorType lastFireControlSensorType = this.fireControlSensorType;
        this.aimLockEntity = aimLockEntity;
        this.fireControlSensorType = fireControlSensorType;
        if (aimLockEntity == null) {
            this.fireControlSensorType = WeaponUnitData.FireControlSensorType.NONE;
        }
        if (radarUnit != null) {
            radarUnit.lockedEntity = aimLockEntity;
        }
        if (vehicle.level().isClientSide()) {
            ClientVehicleAction action = new ClientVehicleAction();
            action.vehicleEntityId = vehicle.getId();
            action.lockEntity = true;
            action.lockedEntityId = aimLockEntity == null ? -1 : aimLockEntity.getId();
            action.sensorType = fireControlSensorType;
            Channel.CHANNEL.sendToServer(action);
        } else {
            // 通知雷达锁定与脱锁给目标载具乘客
            if (lastFireControlSensorType == WeaponUnitData.FireControlSensorType.RF) {
                if (lastAimLockEntity != null) {
                    ServerVehicleWarn packet = new ServerVehicleWarn(vehicle.getId(), lastAimLockEntity.getId(), WarnType.RADAR_LOCK, false);
                    Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> vehicle), packet);
                }
            }
            if (this.fireControlSensorType == WeaponUnitData.FireControlSensorType.RF) {
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

    public boolean isParentWeaponUnitAim() {
        return parentWeaponUnitAim;
    }

    public void setParentWeaponUnitAim(boolean parentWeaponUnitAim) {
        this.parentWeaponUnitAim = parentWeaponUnitAim;
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
        this.yTurnUnitOBBs = PartUnitData.collectOBBs(yTurnBone);
        this.xTurnUnitOBBs = PartUnitData.collectOBBs(xTurnBone);
    }

    @Deprecated
    @Override
    protected void initOBBs() {
        this.unitBedrockCubeOBBs.addAll(xTurnUnitOBBs);
        this.unitBedrockCubeOBBs.addAll(yTurnUnitOBBs);
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

}
