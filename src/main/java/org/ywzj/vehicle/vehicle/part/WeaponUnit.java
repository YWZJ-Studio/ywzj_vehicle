package org.ywzj.vehicle.vehicle.part;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.sync.SyncDataHolder;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.network.message.ServerVehicleFire;
import org.ywzj.vehicle.network.message.ServerVehicleWarn;
import org.ywzj.vehicle.util.CcipUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.pojo.Bolt;
import org.ywzj.vehicle.vehicle.pojo.WarnType;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.weapon.*;
import org.ywzj.vehicle.vehicle.weapon.seeker.ElectroOptical;
import org.ywzj.vehicle.vehicle.weapon.seeker.Infrared;
import org.ywzj.vehicle.vehicle.weapon.seeker.Radar;

import java.util.*;

/**
 * 默认武器站实现<br/>
 * 武器站是一种有方向机与高低机，可发射多类武器的载具可动部件<br/>
 * 其中方向机与高低机的结构模型可相互独立运动<br/>
 * 一个武器站可关联有多个子武器站，并在火控上联动<br/>
 * 多个武器站可纵向堆叠，并在方向机上联动旋转<br/>
 */
public class WeaponUnit extends RotatableUnit<WeaponUnitData> {

    // 武器站炮闩
    private final List<Bolt> bolts = new ArrayList<>();
    // 备弹数
    private int ammoCapacity;
    // 当前使用的炮闩
    private int currentBoltIndex;
    // 发射模式
    private WeaponUnitData.FiringMode firingMode;
    // 冷发射
    private final int coldLaunchTimeTick;
    private final Vec3 coldLaunchVelocity;
    // 武器站光瞄偏移，为开镜视角下玩家的摄像机相对于武器站枢轴的偏移
    private final Vec3 opticalSightOffset;
    // 武器站操作员镜头偏移，为操作员视角下玩家的摄像机相对于武器站枢轴的偏移
    private final Vec3 operatorViewOffset;
    // 操作员是否随武器站转动
    private boolean operatorOnWeaponUnit = true;
    // 开镜类型
    public WeaponUnitData.OpticalSightType opticalSightType;
    // 是否有双向稳定器
    private final boolean withStabilizer;
    // 是否有焦点锁定器
    private final boolean withFocusLocker;
    // 是否有热成像仪
    private final boolean withThermalImager;
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
    private Vec3 focusLockPos;
    private Entity lockedEntity;
    private int loseLockTick;
    private boolean parentWeaponUnitAim;
    private final List<RadarUnit> radarUnits = new ArrayList<>();
    private boolean seekerOn;
    private int lockCoolingTick;
    // 第三人称准心样式
    public WeaponUnitData.CrosshairStyle crosshairStyle;
    // 是否仅渲染选中武器的部件模型
    public boolean renderSelectedWeapon;
    // OBB结构
    private VehicleCubeGroup xTurnGroup;
    // 武器与选射
    public final List<AbstractVehicleWeapon<?>> weapons = new ArrayList<>();
    public final List<AbstractVehicleWeapon<?>> secondaryWeapons = new ArrayList<>();
    public final List<AbstractVehicleWeapon<?>> independentWeapons = new ArrayList<>();
    public final List<AbstractVehicleWeapon<?>> indexedWeapons = new ArrayList<>();
    // 弹仓
    public final Map<AbstractVehicleWeapon<?>, WeaponBayUnit> weaponBayUnits = new HashMap<>();
    private int currentWeaponIndex = -1;
    public SyncDataHolder<Integer> currentWeaponIndexHolder;
    private int currentSecondaryWeaponIndex = -1;
    public SyncDataHolder<Integer> currentSecondaryIndexHolder;
    public Vec3 weaponHitPos;
    public Vec3 weaponHitPosO;
    private VehicleSound irTrackAlarmSound;
    private int ignoreRemoteRotTick;

    public WeaponUnit(int index, AbstractVehicle vehicle, WeaponUnitData data) {
        super(index, vehicle, data);
        this.seatOffset = data.getSeatOffset();
        if (data.getBolts() != null) {
            this.bolts.addAll(data.getBolts());
        } else {
            this.bolts.add(new Bolt(Vec3.ZERO, 0, 0, 0));
        }
        this.ammoCapacity = data.getAmmoCapacity();
        this.firingMode = data.getFiringMode();
        this.coldLaunchTimeTick = data.getColdLaunchTimeTick();
        this.coldLaunchVelocity = data.getColdLaunchVelocity();
        this.parentWeaponUnitAim = data.isParentWeaponUnitAim();
        this.opticalSightOffset = data.getOpticalSightOffset();
        this.operatorViewOffset = data.getOperatorViewOffset();
        this.operatorOnWeaponUnit = data.isOperatorOnWeaponUnit();
        this.fireControlSensorType = data.getFireControlSensorType();
        this.opticalSightType = data.getOpticalSightType();
        this.withStabilizer = data.withStabilizer();
        this.withFocusLocker = data.withFocusLocker();
        this.withThermalImager = data.withThermalImager();
        this.zoomMin = data.getZoomMin();
        this.zoomMax = data.getZoomMax();
        this.zoom = this.zoomMin;
        this.crosshairStyle = data.getCrosshairStyle();
        this.renderSelectedWeapon = data.isRenderSelectedWeapon();
        this.currentWeaponIndexHolder = this.getSyncData().define(
                SyncDataSerializers.INT,
                this::setCurrentWeaponIndex,
                this::getCurrentWeaponIndex,
                currentWeaponIndex
        );
        this.currentSecondaryIndexHolder = this.getSyncData().define(
                SyncDataSerializers.INT,
                this::setCurrentSecondaryWeaponIndex,
                this::getCurrentSecondaryWeaponIndex,
                currentSecondaryWeaponIndex
        );
        currentWeaponIndex = 0;
        currentSecondaryWeaponIndex = 0;
    }

    @Override
    public void buildStructure(Map<VehicleCubeGroup, VehicleCubeGroup> vehicleCubeGroupCopy) {
        super.buildStructure(vehicleCubeGroupCopy);
        this.xTurnGroup = vehicleCubeGroupCopy.get(data.getRawXTurnGroup());
    }

    public void switchWeapon(boolean secondary, boolean next, boolean modding) {
        int size = secondary ? secondaryWeapons.size() : weapons.size();
        if (size < 2) {
            return;
        }
        int index = ((secondary ? currentSecondaryWeaponIndex : currentWeaponIndex) + (next ? 1 : size - 1)) % size;
        selectWeaponIndex(secondary, index, modding);
    }

    public void selectWeaponIndex(boolean secondary, int index, boolean modding) {
        if (secondary) {
            this.getCurrentSecondaryWeapon().ifPresent(AbstractVehicleWeapon::onSwitchFrom);
            setCurrentSecondaryWeaponIndex(index);
            this.getCurrentSecondaryWeapon().ifPresent(AbstractVehicleWeapon::onSwitchTo);
        } else {
            this.getCurrentWeapon().ifPresent(AbstractVehicleWeapon::onSwitchFrom);
            setCurrentWeaponIndex(index);
            this.getCurrentWeapon().ifPresent(AbstractVehicleWeapon::onSwitchTo);
        }
        if (modding) {
            SoundEvent reloadSound = getCurrentWeapon().get().getReloadSound();
            if (reloadSound != null) {
                vehicle.playVehicleSound(reloadSound, getPivotOffset(), 1f, 2f, 1f, 0, false, false, true);
            }
        } else {
            vehicle.playVehicleSound(AllSounds.SELECT_WEAPON.get(), true);
        }
        if (getOwner() instanceof Player player) {
            (secondary ? getCurrentSecondaryWeapon() : getCurrentWeapon()).ifPresent(weapon -> player.displayClientMessage(Component.translatable("tips.use_weapon", weapon.getDisplayName()), true));
        }
    }

    public void cycleMultiWeapon(boolean next) {
        this.getCurrentWeapon().ifPresent(weapon -> {
            if (weapon instanceof VehicleMultiWeapons multiWeapons) {
                multiWeapons.cycleSubWeapon(next);
                vehicle.playVehicleSound(AllSounds.SELECT_WEAPON.get(), true);
                if (getOwner() instanceof Player player) {
                    player.displayClientMessage(Component.translatable("tips.use_weapon", multiWeapons.getDisplayName()), true);
                }
            }
        });
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
        WeaponUnit parent = this;
        // 武器
        for (var weaponInfo : data.getWeapons()) {
            // 多弹种则构造VehicleMultiWeapons
            if (weaponInfo.ids != null && !weaponInfo.ids.isEmpty()) {
                List<AbstractVehicleWeapon<?>> multiWeapons = new ArrayList<>();
                for (ResourceLocation id : weaponInfo.ids) {
                    VehicleWeaponIndex<?, ?> subIndex = CommonAssetsManager.vehicleWeaponManager().getIndex(id).orElse(null);
                    if (subIndex != null) {
                        String subSaveId = weaponInfo.saveId + "_" + id.toString().replace(':', '_');
                        AbstractVehicleWeapon<?> sub = subIndex.create(vehicle, parent, multiWeapons.size(), subSaveId);
                        sub.defineSyncData(this.getSyncData());
                        multiWeapons.add(sub);
                    }
                }
                if (!multiWeapons.isEmpty()) {
                    VehicleMultiWeapons multi = new VehicleMultiWeapons(vehicle, parent, index, multiWeapons, weaponInfo.saveId);
                    multi.defineSyncData(this.getSyncData());
                    if (weaponInfo.secondary) {
                        secondaryWeapons.add(multi);
                    } else {
                        weapons.add(multi);
                    }
                    indexedWeapons.add(multi);
                    if (weaponInfo.weaponBayUnitId != null
                            && partUnitsView.get(weaponInfo.weaponBayUnitId) instanceof WeaponBayUnit weaponBayUnit) {
                        weaponBayUnits.put(multi, weaponBayUnit);
                    }
                    index += 1;
                }
            }
            // 武器站加入武器列表
            else if (weaponInfo.id == null) {
                if (weaponInfo.partUnitId != null
                        && partUnitsView.get(weaponInfo.partUnitId) instanceof WeaponUnit agentWeaponUnit
                        && agentWeaponUnit != parent) {
                    if (agentWeaponUnit.getParentWeaponUnit() == null) {
                        agentWeaponUnit.setParentWeaponUnit(parent);
                    }
                    parent.addSubWeaponUnit(agentWeaponUnit);
                    VehicleWeaponAgent weaponAgent = new VehicleWeaponAgent(vehicle, agentWeaponUnit, index);
                    weapons.add(weaponAgent);
                    indexedWeapons.add(weaponAgent);
                    if (weaponInfo.weaponBayUnitId != null
                            && partUnitsView.get(weaponInfo.weaponBayUnitId) instanceof WeaponBayUnit weaponBayUnit) {
                        weaponBayUnits.put(weaponAgent, weaponBayUnit);
                    }
                    index += 1;
                }
            }
            // 武器实现加入武器列表
            else {
                VehicleWeaponIndex<?, ?> vehicleWeaponIndex = CommonAssetsManager.vehicleWeaponManager().getIndex(weaponInfo.id).orElse(null);
                if (vehicleWeaponIndex != null) {
                    AbstractVehicleWeapon<?> weapon;
                    if (weaponInfo.partUnitId != null
                            && partUnitsView.get(weaponInfo.partUnitId) instanceof WeaponUnit subWeaponUnit
                            && subWeaponUnit != parent) {
                        if (subWeaponUnit.getParentWeaponUnit() == null) {
                            subWeaponUnit.setParentWeaponUnit(parent);
                        }
                        parent.addSubWeaponUnit(subWeaponUnit);
                        weapon = vehicleWeaponIndex.create(vehicle, subWeaponUnit, index, weaponInfo.saveId);
                    } else {
                        weapon = vehicleWeaponIndex.create(vehicle, parent, index, weaponInfo.saveId);
                    }
                    if (vehicleWeaponIndex.data().independent) {
                        independentWeapons.add(weapon);
                    } else if (weaponInfo.secondary) {
                        secondaryWeapons.add(weapon);
                    } else {
                        weapons.add(weapon);
                    }
                    weapon.defineSyncData(this.getSyncData());
                    indexedWeapons.add(weapon);
                    if (weaponInfo.weaponBayUnitId != null
                            && partUnitsView.get(weaponInfo.weaponBayUnitId) instanceof WeaponBayUnit weaponBayUnit) {
                        weaponBayUnits.put(weapon, weaponBayUnit);
                    }
                    index += 1;
                }
            }
        }
        // 雷达
        for (String subPartUnitId : data.getSubPartUnitIds()) {
            if (partUnitsView.get(subPartUnitId) instanceof RadarUnit radarUnit) {
                radarUnits.add(radarUnit);
                addSubPartUnit(radarUnit);
                radarUnit.setParentPartUnit(this);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        weapons.forEach(weapon -> {
            AbstractVehicleWeapon<?> proxyWeapon = proxyWeapon(weapon);
            ClientAssetsManager.INSTANCE.getWeaponDisplay(proxyWeapon.getData().getWeaponId()).ifPresent(weaponDisplay -> {
                VehicleBedrockModel weaponModel = weaponDisplay.getModel();
                if (weaponModel == null) {
                    return;
                }
                if (!weaponModel.hasBakedModel()) {
                    return;
                }
                ResourceLocation texture = weaponDisplay.getTexture();
                if (texture == null) {
                    return;
                }
                WeaponUnit weaponUnit = proxyWeapon.getWeaponUnit();
                if (weaponUnit == null) {
                    return;
                }
                if (!weaponUnit.renderModel) {
                    return;
                }
                if (weaponUnit.parentWeaponUnit != null && weaponUnit.parentWeaponUnit.renderSelectedWeapon) {
                    if (weaponUnit.parentWeaponUnit.getCurrentWeapon().get().getWeaponUnit() != this) {
                        return;
                    }
                }
                VehicleCubeGroup xTurnGroup = weaponUnit.xTurnGroup;
                if (xTurnGroup == null) {
                    return;
                }
                int boltsSize = weaponUnit.getBolts().size();
                int remainAmmo = Math.min(proxyWeapon.getRemainAmmo(), boltsSize);
                for (int index = boltsSize - 1; index > boltsSize - 1 - remainAmmo; index -= 1) {
                    Bolt bolt = weaponUnit.getBolts().get(index);
                    pPoseStack.pushPose();
                    {
                        Vec3 offset = xTurnGroup.pivotOffset.add(bolt.offset);
                        pPoseStack.translate(offset.x(), offset.y(), offset.z() + bolt.barrelLength / 2);
                        weaponModel.renderToBufferBaked(pPoseStack, bufferSource, texture, vehicle.isDestroyed() ? 64 : pPackedLight);
                        weaponModel.renderSpecialBonesBaked(pPoseStack, bufferSource, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY);
                    }
                    pPoseStack.popPose();
                }
            });
        });
    }

    @Override
    public void tick() {
        if (vehicle.level().isClientSide()) {
            tickFireControl();
        } else {
            // 通知雷达锁定给目标载具乘客
            if (vehicle.tickCount % 2 == 0 && getFireControlSensorType() == WeaponUnitData.FireControlSensorType.RF && !radarUnits.isEmpty()) {
                RadarUnit mainRadarUnit = getMainRadarUnit();
                Entity radarLockedEntity = mainRadarUnit.getLockedEntity();
                if (radarLockedEntity != null) {
                    ServerVehicleWarn packet = new ServerVehicleWarn(vehicle.getId(), radarLockedEntity.getId(), WarnType.RADAR_LOCK, mainRadarUnit.getRadarType());
                    Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> radarLockedEntity), packet);
                }
            }
        }
        super.tick();
        indexedWeapons.forEach(AbstractVehicleWeapon::tick);
    }

    @Override
    protected void tickRemoteRot() {
        if (vehicle.level().isClientSide() && ignoreRemoteRotTick > 0) {
            if (Math.abs(xRemoteAimRot - xAimRot) < 5 && Math.abs(yRemoteAimRot - yAimRot) < 5) {
                ignoreRemoteRotTick -= 1;
                return;
            } else {
                ignoreRemoteRotTick = 0;
            }
        }
        super.tickRemoteRot();
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
    public void tickFireControl() {
        if (vehicle.getPassengers().isEmpty()) {
            return;
        }
        if (getOwner() != LocalVehiclePlayer.instance.getPlayer()) {
            lockedEntity = null;
            return;
        }
        if (withFocusLocker && focusLockPos != null) {
            aim(focusLockPos);
            return;
        }
        if (lockedEntity != null) {
            Vec3 center = lockedEntity.getBoundingBox().getCenter();
            // 武器站火控自动瞄准锁定目标
            aim(center);
            Entity checkEntity = null;
            // 武器站火控是否仍能锁定目标
            switch (getFireControlSensorType()) {
                case IR -> checkEntity = Infrared.checkTarget(this, lockedEntity);
                case EO -> checkEntity = ElectroOptical.checkTarget(this, lockedEntity);
                case RF -> checkEntity = Radar.checkTarget(vehicle, getRadarDetectedEntities().stream()
                        .map(detectedObject -> detectedObject.entity).toList(), lockedEntity);
                default -> {}
            }
            if (checkEntity != lockedEntity) {
                setLockedEntity(checkEntity);
            }
            // 雷达发生远程与本地实体切换
            if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.RF && radarUnits != null && lockedEntity != null) {
                RadarUnit.DetectedObject detectedObject = null;
                for (RadarUnit radarUnit : radarUnits) {
                    detectedObject = radarUnit.getDetectedEntities().get(lockedEntity.getId());
                    if (detectedObject != null) {
                        break;
                    }
                }
                if (detectedObject != null && detectedObject.entity != lockedEntity) {
                    setLockedEntity(detectedObject.entity);
                }
            }
            // 锁定实体是否已消失
            if (lockedEntity != null) {
                if (!lockedEntity.isAlive()) {
                    if (loseLockTick > 20) {
                        setLockedEntity(null);
                    } else {
                        loseLockTick += 1;
                    }
                }
            }
        }
        // 导引头开启并冷却后搜索并锁定目标
        else if (isSeekerOn() && lockedEntity == null) {
            lockCoolingTick += 1;
            if (lockCoolingTick > 20) {
                // 导弹
                Optional<AbstractVehicleWeapon<?>> weaponOptional = getCurrentWeapon();
                if (weaponOptional.isPresent() && weaponOptional.get() instanceof VehicleMissile missile) {
                    Entity entity = null;
                    // 红外
                    if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR) {
                        entity = Infrared.findTarget(missile.getWeaponUnit(), missile.getData().getSeekerFov());
                    }
                    // 雷达
                    else if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.RF) {
                        // 主动雷达弹需雷达扫描目标
                        if (missile.getData().getHomingMode() == VehicleMissileWeaponData.HomingMode.ACTIVE_RADAR) {
                            entity = Radar.findTarget(getMainRadarUnit(), missile.getData().getSeekerFov(), this);
                        }
                        // 半主动雷达弹需雷达锁定目标
                        else if (missile.getData().getHomingMode() == VehicleMissileWeaponData.HomingMode.SEMI_ACTIVE_RADAR) {
                            entity = getMainRadarUnit().getLockedEntity();
                            if (entity != null) {
                                Vec3 vLock = entity.getBoundingBox().getCenter().subtract(worldPivotPosition());
                                Vec3 vAim = worldVec();
                                double degree = Math.toDegrees(VectorUtil.angleBetween(vLock, vAim));
                                if (degree > missile.getData().getSeekerFov()) {
                                    entity = null;
                                }
                            }
                        }
                    }
                    if (entity != null) {
                        setLockedEntity(entity);
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickHit() {
        weaponHitPosO = weaponHitPos;
        weaponHitPos = currentWeaponHitPosition();
    }

    @Override
    public boolean onInteract(Player player, InteractionHand hand) {
        if (!vehicle.level().isClientSide() && hand == InteractionHand.MAIN_HAND && isInteractive() && player.isShiftKeyDown()) {
            if (player.getItemInHand(hand).getItem() == AllItems.MODDING_TOOL.get()) {
                switchWeapon(false, true, true);
                AbstractVehicleWeapon<?> weapon = getCurrentWeapon().get();
                player.displayClientMessage(Component.translatable("tips.use_weapon", weapon.getDisplayName()), true);
            }
            return false;
        }
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void fireControlLock() {
        if (!radarUnits.isEmpty()) {
            RadarUnit mainRadarUnit = getMainRadarUnit();
            if (mainRadarUnit.getLockedEntity() != null) {
                mainRadarUnit.setLockedEntity(null);
                return;
            }
        }
        if (lockedEntity != null) {
            setLockedEntity(null);
            return;
        }
        WeaponUnitData.FireControlSensorType sensorType = getFireControlSensorType();
        // 光电锁定
        if (sensorType == WeaponUnitData.FireControlSensorType.EO) {
            Vec3 lookAtPos;
            if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON
                    || LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                lookAtPos = LocalVehiclePlayer.instance.freeAimPos();
            } else if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                lookAtPos = LocalVehiclePlayer.instance.scopeAimPos();
            } else {
                return;
            }
            Entity lockedEntity = ElectroOptical.findTarget(this, lookAtPos);
            setLockedEntity(lockedEntity);
            if (lockedEntity != null) {
                setFocusLockPos(null);
                return;
            }
        }
        // 雷达锁定
        if (sensorType == WeaponUnitData.FireControlSensorType.RF) {
            RadarUnit mainRadarUnit = getMainRadarUnit();
            Entity lockedEntity = Radar.findTarget(mainRadarUnit, mainRadarUnit.getScanSectorAngle(), this);
            if (lockedEntity != null) {
                mainRadarUnit.setLockedEntity(lockedEntity);
                return;
            }
        }
        // 焦点锁定
        if (withFocusLocker && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
            if (focusLockPos == null) {
                setFocusLockPos(LocalVehiclePlayer.instance.scopeAimPos());
            } else {
                setFocusLockPos(null);
            }
        } else if (focusLockPos != null) {
            setFocusLockPos(null);
        }
    }

    /**
     * 导引头开关机
     */
    @OnlyIn(Dist.CLIENT)
    public void toggleSeeker(Boolean on) {
        Optional<AbstractVehicleWeapon<?>> weaponOptional = getCurrentWeapon();
        if (weaponOptional.isPresent()) {
            AbstractVehicleWeapon<?> weapon = weaponOptional.get();
            if (weapon.withSeeker()) {
                if (weapon.getRemainAmmo() == 0) {
                    on = false;
                }
                if (on == null) {
                    seekerOn = !seekerOn;
                } else {
                    seekerOn = on;
                }
                lockCoolingTick = 0;
                if (lockedEntity != null) {
                    setLockedEntity(null);
                }
            }
        }
    }

    public void shoot(int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (weaponIndex < indexedWeapons.size()) {
            AbstractVehicleWeapon<?> weapon = indexedWeapons.get(weaponIndex);
            WeaponBayUnit weaponBayUnit = weaponBayUnits.get(weapon);
            if (weaponBayUnit != null && !weaponBayUnit.isOn()) {
                weaponBayUnit.setOn(true);
                return;
            }
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

    @OnlyIn(Dist.CLIENT)
    public void toggleCurrentWeaponBay() {
        getCurrentWeapon().ifPresent(weapon -> {
            WeaponBayUnit weaponBayUnit = weaponBayUnits.get(weapon);
            if (weaponBayUnit != null) {
                ClientVehicleAction action = new ClientVehicleAction();
                action.vehicleEntityId = vehicle.getId();
                action.partUnitIndex = weaponBayUnit.getIndex();
                action.togglePartUnitState = true;
                Channel.CHANNEL.sendToServer(action);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    public void onClientFire() {
        Optional<AbstractVehicleWeapon<?>> weaponOptional = getCurrentWeapon();
        WeaponUnit rootParentWeaponUnit = getRootParentWeaponUnit();
        if (weaponOptional.isEmpty()) {
            if (rootParentWeaponUnit != null) {
                weaponOptional = rootParentWeaponUnit.getCurrentWeapon();
            }
        }
        if (weaponOptional.isPresent()) {
            if (weaponOptional.get() instanceof VehicleMissile vehicleMissile
                    && vehicleMissile.getData().getGuidance() == VehicleMissileWeaponData.Guidance.HOMING) {
                rootParentWeaponUnit.toggleSeeker(false);
            }
        }
    }

    /**
     * 武器站双向稳定系统
     */
    @Override
    public void withVehicleRot(float dVehicleXRot, float dVehicleYRot, float dVehicleZRot) {
        if (Math.abs(xAimRot - xRot) > 5 || Math.abs(yAimRot - yRot) > 5) {
            return;
        }
        if (Math.abs(dVehicleXRot) > 0.01 || Math.abs(dVehicleYRot) > 0.01 || Math.abs(dVehicleZRot) > 0.01) {
            if (getOwner() != null && (!needPower || vehicle.hasPower()) && withStabilizer) {
                Quaternionf rotationO = new Quaternionf();
                rotationO.rotateY(org.joml.Math.toRadians(-(vehicle.getYRot() - dVehicleYRot)))
                        .rotateX(org.joml.Math.toRadians(vehicle.getXRot() - dVehicleXRot))
                        .rotateZ(org.joml.Math.toRadians(vehicle.getZRot() - dVehicleZRot));
                if (structureGroup != null) {
                    rotationO.mul(structureGroup.baseRotation);
                    if (structureGroup.parent != null) {
                        rotationO.mul(structureGroup.parent.globalTransform().rotation());
                    }
                }
                Vector3f localVec = VectorUtil.rotToVec(xRot, yRot).toVector3f();
                Quaternionf relativeRot = baseRot().invert().mul(rotationO);
                Vector3f targetVec = new Quaternionf(relativeRot).transform(localVec);
                Vec2 targetRot = VectorUtil.vecToRot(new Vec3(targetVec));
                setXRot(Math.max(Math.min(targetRot.x, xRotMax), xRotMin));
                if (org.joml.Math.abs(xRot - xRotO) > 180) {
                    xRotO += org.joml.Math.signum(xRot - xRotO) * 360;
                }
                setYRot(Math.max(Math.min(targetRot.y, yRotMax), yRotMin));
                if (org.joml.Math.abs(yRot - yRotO) > 180) {
                    yRotO += org.joml.Math.signum(yRot - yRotO) * 360;
                }
                updateRot();
                if (vehicle.level().isClientSide()) {
                    setXAimRot(targetRot.x);
                    setYAimRot(targetRot.y);
                    ignoreRemoteRotTick = 1;
                }
            }
        }
    }

    public void aim(Vec3 worldPos) {
        Vec2 rot = aimRot(worldPos);
        Bolt bolt = getCurrentBolt();
        rot = new Vec2(rot.x - bolt.xRot, rot.y + bolt.yRot);
        if (xAimRot != rot.x || yAimRot != rot.y) {
            if (vehicle.level().isClientSide()) {
                ClientVehicleAction control = new ClientVehicleAction();
                control.vehicleEntityId = vehicle.getId();
                control.partUnitIndex = index;
                control.xAimRot = rot.x;
                control.yAimRot = rot.y;
                Channel.CHANNEL.sendToServer(control);
            }
            xAimRot = rot.x;
            yAimRot = rot.y;
        }
        subWeaponUnits.forEach(weaponUnit -> weaponUnit.aim(worldPos));
    }

    @Override
    public Vec2 aimRot(Vec3 worldPosition) {
        if (xTurnGroup == null) {
            return new Vec2(0, 0);
        }
        Vec3 fromWorldPosition = vehicle.relativeRotPos(vehicle.position().add(xTurnGroup.globalTransform().offset()), false);
        Vec3 worldAim = new Vec3(worldPosition.x - fromWorldPosition.x, worldPosition.y - fromWorldPosition.y, worldPosition.z - fromWorldPosition.z);
        return worldVecToLocalRot(worldAim);
    }

    private Vec3 currentWeaponHitPosition() {
        Optional<AbstractVehicleWeapon<?>> vehicleWeaponOptional = getCurrentWeapon();
        if (vehicleWeaponOptional.isPresent()) {
            AbstractVehicleWeapon<?> vehicleWeapon = vehicleWeaponOptional.get();
            WeaponUnit currentWeaponUnit = vehicleWeapon.getWeaponUnit();
            if (currentWeaponUnit.isParentWeaponUnitAim()) {
                currentWeaponUnit = currentWeaponUnit.getRootParentWeaponUnit();
            }
            Vec3 aimHitPosition = null;
            if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.CCIP) {
                if (vehicleWeapon instanceof VehicleMultiWeapons vehicleMultiWeapons) {
                    vehicleWeapon = vehicleMultiWeapons.getSelectedWeapon();
                }
                if (vehicleWeapon instanceof VehicleAerialBomb bomb) {
                    Vec3 releasePos = currentWeaponUnit.worldPivotPosition().add(0, 0, 0);
                    float dragCoefficient = bomb.getData().getDragCoefficient();
                    aimHitPosition = CcipUtil.computeCcip(vehicle.level(), releasePos, vehicle.getDeltaMovement(), dragCoefficient);
                } else if (vehicleWeapon instanceof VehicleRocket rocket) {
                    Vec3 releasePos = currentWeaponUnit.worldPivotPosition();
                    var data = rocket.getData();
                    Vec2 rot = currentWeaponUnit.worldRot();
                    Vec3 aimDir = VectorUtil.rotToVec(rot.x, rot.y).normalize();
                    Vec3 startVelocity = aimDir.scale(data.getVelocity()).add(vehicle.getDeltaMovement());
                    aimHitPosition = CcipUtil.computeCcipRocket(vehicle.level(), releasePos, startVelocity,
                            data.getThrust(), data.getMass(), data.getMotorBurnTime(), data.getDragCoefficient());
                } else if (vehicleWeapon instanceof VehicleCannon cannon) {
                    AimContext aimContext = currentWeaponUnit.aimContext();
                    var data = cannon.getData();
                    Vec3 aimDir = VectorUtil.rotToVec(aimContext.direction.x, aimContext.direction.y).normalize();
                    Vec3 startVelocity = aimDir.scale(data.getVelocity()).add(vehicle.getDeltaMovement());
                    aimHitPosition = CcipUtil.computeCcipCannon(vehicle.level(), vehicle, aimContext.from, startVelocity,
                            data.getFriction(), data.getLife());
                }
            }
            if (aimHitPosition == null) {
                aimHitPosition = currentWeaponUnit.aimHitPosition();
            }
            return aimHitPosition;
        } else {
            return aimHitPosition();
        }
    }

    public Vec3 aimHitPosition() {
        List<Vec3> positions = aimContexts().stream().map(context -> context.from).toList();
        double x = positions.stream().mapToDouble(pos -> pos.x).average().orElse(0);
        double y = positions.stream().mapToDouble(pos -> pos.y).average().orElse(0);
        double z = positions.stream().mapToDouble(pos -> pos.z).average().orElse(0);
        AimContext aimContext = aimContext();
        Vec3 start = new Vec3(x, y, z);
        Vec3 end = start.add(VectorUtil.rotToVec(aimContext.direction.x, aimContext.direction.y).normalize().scale(1024));
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
            aimContext.from = this.vehicle.position();
            aimContext.direction = new Vec2(this.vehicle.getXRot(), this.vehicle.getYRot());
            return aimContext;
        }
        VehicleCubeGroup.GlobalTransform globalTransform = xTurnGroup.globalTransform();
        Quaternionf rotation = globalTransform.rotation();
        Vec3 boltPosition = worldBoltPosition(bolt, globalTransform);
        Vector3f worldRot = new Vector3f();
        vehicle.rotYXZ().mul(rotation).getEulerAnglesYXZ(worldRot);
        aimContext.direction = new Vec2((float) Math.toDegrees(worldRot.x), (float) Math.toDegrees(-worldRot.y));
        Vec3 direction = VectorUtil.rotToVec(aimContext.direction.x, aimContext.direction.y);
        aimContext.from = boltPosition.add(direction.scale(bolt.barrelLength));
        return aimContext;
    }

    public Vec3 worldCurrentBoltPosition() {
        Bolt bolt = getCurrentBolt();
        VehicleCubeGroup.GlobalTransform globalTransform = xTurnGroup.globalTransform();
        return worldBoltPosition(bolt, globalTransform);
    }

    public Vec3 worldBoltPosition(Bolt bolt, VehicleCubeGroup.GlobalTransform globalTransform) {
        Quaternionf rotation = globalTransform.rotation();
        Vector3f boltOffset = new Vector3f((float) bolt.offset.x, (float) bolt.offset.y, (float) bolt.offset.z);
        rotation.transform(boltOffset);
        Vec3 pivot = globalTransform.offset();
        Vector3f rotatedBoltOffset = vehicle.rotYXZ().transform(pivot
                .add(boltOffset.x, boltOffset.y, boltOffset.z).toVector3f()
                .sub(vehicle.centerOffset.toVector3f()));
        return vehicle.position().add(vehicle.centerOffset).add(rotatedBoltOffset.x, rotatedBoltOffset.y, rotatedBoltOffset.z);
    }

    public Vec3 worldPivotPosition() {
        return worldPositionWithBaseRot(pivotOffset);
    }

    public Vec3 worldOpticalSightPosition() {
        if (opticalSightOffset == null) {
            return worldOwnerViewPosition();
        }
        Vec3 offsetFromVehicle = pivotOffset.add(opticalSightOffset);
        if (getOpticalSightType() == WeaponUnitData.OpticalSightType.OPERATOR) {
            return worldPositionWithGroupRot(offsetFromVehicle, xTurnGroup);
        }
        return worldPosition(offsetFromVehicle);
    }

    @Override
    public Vec3 worldOwnerViewPosition() {
        if (operatorViewOffset == null) {
            float eyeHeight = owner == null ? 2 : owner.getEyeHeight();
            return worldPosition(pivotOffset.add(new Vec3(0, eyeHeight, 0)));
        }
        Vec3 offsetFromVehicle = pivotOffset.add(operatorViewOffset);
        if (!operatorOnWeaponUnit && LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
            return worldPositionWithBaseRot(offsetFromVehicle);
        }
        if (getOpticalSightType() == WeaponUnitData.OpticalSightType.OPERATOR) {
            return worldPositionWithGroupRot(offsetFromVehicle, xTurnGroup);
        }
        return worldPosition(offsetFromVehicle);
    }

    @Override
    public Vec3 worldSeatPosition() {
        float eyeHeight = getOwner() == null ? 2 : owner.getEyeHeight();
        if (!operatorOnWeaponUnit) {
            return worldPositionWithBaseRot(new Vec3(seatOffset.x, seatOffset.y  - eyeHeight, seatOffset.z));
        }
        return worldPositionWithSelfRot(new Vec3(seatOffset.x, seatOffset.y  - eyeHeight, seatOffset.z));
    }

    @Override
    public Quaternionf baseRot() {
        Quaternionf rotation = vehicle.rotYXZ();
        VehicleCubeGroup group = structureGroup;
        if (group == null) {
            group = xTurnGroup;
        }
        if (group != null) {
            rotation.mul(group.baseRotation);
            if (group.parent != null) {
                rotation.mul(group.parent.globalTransform().rotation());
            }
        }
        return rotation;
    }

    public int getAmmoCapacity() {
        return ammoCapacity;
    }

    public Bolt getCurrentBolt() {
        return bolts.get(currentBoltIndex);
    }

    public List<Bolt> getBolts() {
        return bolts;
    }

    public WeaponUnitData.FiringMode getFiringMode() {
        return firingMode;
    }

    public int getColdLaunchTimeTick() {
        return coldLaunchTimeTick;
    }

    public Vec3 getColdLaunchVelocity() {
        return coldLaunchVelocity;
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

    public boolean withStabilizer() {
        return withStabilizer;
    }

    public boolean withThermalImager() {
        return withThermalImager;
    }

    public boolean withFocusLocker() {
        return withFocusLocker;
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

    public Collection<RadarUnit.DetectedObject> getRadarDetectedEntities() {
        Set<RadarUnit.DetectedObject> radarDetectedEntities = new HashSet<>();
        for (RadarUnit radarUnit : radarUnits) {
            radarDetectedEntities.addAll(radarUnit.getDetectedEntities().values());
        }
        return radarDetectedEntities;
    }

    public Vec3 getFocusLockPos() {
        return focusLockPos;
    }

    public void setFocusLockPos(Vec3 focusLockPos) {
        this.focusLockPos = focusLockPos;
    }

    public Entity getLockedEntity() {
        return lockedEntity;
    }

    /**
     * 武器站锁定目标
     */
    public void setLockedEntity(Entity lockedEntity) {
        this.lockedEntity = lockedEntity;
        loseLockTick = 0;
        if (vehicle.level().isClientSide()) {
            if (irTrackAlarmSound != null && lockedEntity == null) {
                irTrackAlarmSound.stop();
                irTrackAlarmSound = null;
            }
            // 红外锁定提示
            if (getFireControlSensorType() == WeaponUnitData.FireControlSensorType.IR) {
                if (irTrackAlarmSound == null && lockedEntity != null) {
                    irTrackAlarmSound = new VehicleSound(AllSounds.IR_TRACK_ALARM.get(), 1f, 1f, 1f, true, 50, false, false, vehicle.getId());
                    irTrackAlarmSound.play();
                }
            }
            // 将客户端锁定目标通知服务端
            ClientVehicleAction action = new ClientVehicleAction();
            action.vehicleEntityId = vehicle.getId();
            action.lockEntity = true;
            action.lockedEntityId = lockedEntity == null ? -1 : lockedEntity.getId();
            Channel.CHANNEL.sendToServer(action);
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

    public WeaponUnit getRootParentWeaponUnit() {
        WeaponUnit rootParentWeaponUnit = parentWeaponUnit;
        if (rootParentWeaponUnit == null) {
            return this;
        }
        while (rootParentWeaponUnit.getParentWeaponUnit() != null) {
            rootParentWeaponUnit = rootParentWeaponUnit.getParentWeaponUnit();
        }
        return rootParentWeaponUnit;
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

    public boolean isInteractive() {
        return parentWeaponUnit != null;
    }

    public boolean isParentWeaponUnitAim() {
        return parentWeaponUnitAim;
    }

    public void setParentWeaponUnitAim(boolean parentWeaponUnitAim) {
        this.parentWeaponUnitAim = parentWeaponUnitAim;
    }

    public List<RadarUnit> getRadarUnits() {
        return radarUnits;
    }

    public RadarUnit getMainRadarUnit() {
        if (radarUnits.isEmpty()) {
            return null;
        }
        return radarUnits.get(0);
    }

    public boolean isSeekerOn() {
        return seekerOn;
    }

    public float getSeekerFov() {
        Optional<AbstractVehicleWeapon<?>> weaponOptional = getCurrentWeapon();
        if (weaponOptional.isPresent() && weaponOptional.get() instanceof VehicleMissile missile) {
            return missile.getData().getSeekerFov();
        }
        return 30;
    }

    public int getLockCoolingTick() {
        return lockCoolingTick;
    }

    public AbstractVehicleWeapon<?> proxyWeapon(AbstractVehicleWeapon<?> weapon) {
        while (weapon instanceof VehicleWeaponAgent weaponAgent) {
            Optional<AbstractVehicleWeapon<?>> weaponOptional = weaponAgent.getWeaponUnit().getCurrentWeapon();
            if (weaponOptional.isPresent()) {
                weapon = weaponOptional.get();
            } else {
                break;
            }
        }
        return weapon;
    }

    public Optional<AbstractVehicleWeapon<?>> getCurrentWeapon() {
        if (currentWeaponIndex >= 0 && currentWeaponIndex < weapons.size()) {
            AbstractVehicleWeapon<?> currentWeapon = proxyWeapon(weapons.get(currentWeaponIndex));
            return Optional.of(currentWeapon);
        }
        return Optional.empty();
    }

    public int getCurrentWeaponIndex() {
        return currentWeaponIndex;
    }

    public void setCurrentWeaponIndex(int index) {
        this.currentWeaponIndex = index;
        setLockedEntity(null);
    }

    public Optional<AbstractVehicleWeapon<?>> getCurrentSecondaryWeapon() {
        if (currentSecondaryWeaponIndex >= 0 && currentSecondaryWeaponIndex < secondaryWeapons.size()) {
            return Optional.of(secondaryWeapons.get(currentSecondaryWeaponIndex));
        }
        return Optional.empty();
    }

    public int getCurrentSecondaryWeaponIndex() {
        return currentSecondaryWeaponIndex;
    }

    public void setCurrentSecondaryWeaponIndex(int currentSecondaryWeaponIndex) {
        this.currentSecondaryWeaponIndex = currentSecondaryWeaponIndex;
        setLockedEntity(null);
    }

    public List<AbstractVehicleWeapon<?>> getIndexedWeapons() {
        return indexedWeapons;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putInt("CurrentWeaponIndex", currentWeaponIndex);
        CompoundTag weaponTag = new CompoundTag();
        this.indexedWeapons.forEach(weapon -> {
            String serializeId = weapon.getSerializeId();
            if (serializeId != null) {
                CompoundTag weaponData = weapon.serializeNBT();
                if (weaponData.isEmpty()) {
                    return;
                }
                weaponTag.put(serializeId, weaponData);
            }
        });
        tag.put("WeaponTag", weaponTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        this.initWeapon(nbt.getInt("CurrentWeaponIndex"));
        CompoundTag weaponTag = nbt.getCompound("WeaponTag");
        this.indexedWeapons.forEach(weapon -> {
            if (weaponTag.contains(weapon.getSerializeId(), Tag.TAG_COMPOUND)) {
                String serializeId = weapon.getSerializeId();
                if (serializeId != null) {
                    weapon.deserializeNBT(weaponTag.getCompound(serializeId));
                }
            }
        });
    }

    @Deprecated
    public WeaponUnit(String id, int index, AbstractVehicle vehicle,
                      Vec3 pivotOffset, float barrelLength,
                      Vec3 opticalSightOffset, Vec3 operatorViewOffset, Vec3 seatOffset, WeaponUnit baseWeaponUnit) {
        super(id, index, vehicle);
        this.withStabilizer = false;
        this.withThermalImager = false;
        this.withFocusLocker = false;
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

        this.fireControlSensorType = WeaponUnitData.FireControlSensorType.NONE;
        this.coldLaunchTimeTick = 0;
        this.coldLaunchVelocity = new Vec3(0, -1, 0);

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
