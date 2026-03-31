package org.ywzj.vehicle.entity.vehicle;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.Math;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllDamageTypes;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.entity.BoundingBoxChangeable;
import org.ywzj.vehicle.api.entity.ICustomVehicle;
import org.ywzj.vehicle.api.entity.OBBEntity;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.api.event.VehicleAttackEvent;
import org.ywzj.vehicle.api.event.VehicleCollectCollisionEvent;
import org.ywzj.vehicle.api.event.VehicleMoveEvent;
import org.ywzj.vehicle.capability.VehicleCapabilityProvider;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.part.data.PartUnitPojo;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.ContainerCraft;
import org.ywzj.vehicle.item.VehicleItem;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.*;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.DamageSystem;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PhysicsEngine;
import org.ywzj.vehicle.vehicle.control.ControlUnit;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
import org.ywzj.vehicle.vehicle.part.DoorUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.passenger.WarningReceiver;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.pojo.DefenseStats;
import org.ywzj.vehicle.vehicle.pojo.EnergyInfo;
import org.ywzj.vehicle.vehicle.pojo.ViewInfo;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;
import org.ywzj.vehicle.vehicle.structure.VehicleStructOBBs;

import java.util.*;

public abstract class AbstractVehicle extends ContainerCraft
        implements RemoteTickEntity, OBBEntity, ICustomVehicle, IEntityAdditionalSpawnData, BoundingBoxChangeable {

    public static final EntityDataAccessor<Float> X_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> Y_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> Z_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENERGY = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENGINE_SPEED = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> ENGINE_ON = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DESTROYED = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.BOOLEAN);
    private ResourceLocation vehicleId;
    private ResourceLocation displayId;
    private Component name;
    public final ControlUnit controlUnit;
    public List<Seat> seats;
    protected final List<PartUnit<?>> partUnits;
    protected Map<String, PartUnit<?>> partUnitMap;
    protected final Map<String, DecorationUnit> decorationUnits;
    protected ViewInfo viewInfo;
    protected boolean viewZoomed;
    public EnergyInfo energyInfo;
    public DefenseStats defenseStats;
    public Vec3 centerOffset;
    public float curbWeight;
    private float xRot;
    public float xRotO;
    private float lerpXRot;
    private float yRot;
    public float yRotO;
    private float lerpYRot;
    private float zRot;
    public float zRotO;
    private float lerpZRot;
    protected List<VehicleCubeOBB> vehicleCubeOBBs;
    protected VehicleCubeOBB mainCubeOBB;
    protected double structureLength;
    public WarningReceiver warningReceiver;
    public PhysicsEngine physicsEngine;
    private final HashMap<LivingEntity, Vec3> dismountLocations;
    protected boolean driverXYRotControl = false;
    public boolean uav;
    public boolean remote;
    public Team remoteTeam;
    public boolean protectPassenger;
    protected boolean dataInitialized;
    private long destroyedTime;
    protected int engineParticleTick;
    public long lastRenderTime;
    private boolean finalRotUpdate;

    protected AbstractVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.vehicleId = EntityType.getKey(pEntityType);
        this.seats = new ArrayList<>();
        this.controlUnit = new ControlUnit(this);
        this.partUnits = new ArrayList<>();
        this.partUnitMap = Map.of();
        this.decorationUnits = new HashMap<>();
        this.vehicleCubeOBBs = new ArrayList<>();
        this.curbWeight = 1;
        this.viewInfo = new ViewInfo();
        this.energyInfo = new EnergyInfo();
        this.setMaxUpStep(1.0f);
        this.physicsEngine = new PhysicsEngine(this);
        this.dismountLocations = new HashMap<>();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(X_ROT, 0f);
        this.entityData.define(Y_ROT, 0f);
        this.entityData.define(Z_ROT, 0f);
        this.entityData.define(ENERGY, 0f);
        this.entityData.define(POWER, 0f);
        this.entityData.define(ENGINE_SPEED, 0f);
        this.entityData.define(ENGINE_ON, false);
        this.entityData.define(DESTROYED, false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Engine", isEngineOn());
        compound.putFloat("Power", getPower());
        compound.putBoolean("Destroyed", isDestroyed());
        compound.putLong("DestroyedTime", destroyedTime);
        compound.putString(ICustomVehicle.TAG_VEHICLE_ID, this.getVehicleId().toString());
        compound.putString(ICustomVehicle.TAG_VEHICLE_DISPLAY_ID, this.getDisplayId().toString());
        compound.put("PartUnits", serializePartUnitsData());
        compound.put("DecorationUnits", serializeDecorationUnitsData());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Engine")) {
            toggleEngine(compound.getBoolean("Engine"));
        }
        if (compound.contains("Power")) {
            setPower(compound.getFloat("Power"));
        }
        if (compound.contains("Destroyed", Tag.TAG_ANY_NUMERIC)) {
            entityData.set(DESTROYED, compound.getBoolean("Destroyed"));
        }
        if (compound.contains("DestroyedTime", Tag.TAG_ANY_NUMERIC)) {
            destroyedTime = compound.getLong("DestroyedTime");
        }
        if (compound.contains(ICustomVehicle.TAG_VEHICLE_ID, Tag.TAG_STRING)) {
            ResourceLocation vehicleId = ResourceLocation.tryParse(compound.getString(ICustomVehicle.TAG_VEHICLE_ID));
            if (vehicleId != null) {
                this.vehicleId = vehicleId;
            }
        }
        if (compound.contains(ICustomVehicle.TAG_VEHICLE_DISPLAY_ID, Tag.TAG_STRING)) {
            ResourceLocation displayId = ResourceLocation.tryParse(compound.getString(ICustomVehicle.TAG_VEHICLE_DISPLAY_ID));
            if (displayId != null) {
                this.displayId = displayId;
            }
        }
        this.initData();
        if (compound.contains("PartUnits", Tag.TAG_COMPOUND)) {
            deserializePartUnitsData(compound.getCompound("PartUnits"));
        }
        if (compound.contains("DecorationUnits", Tag.TAG_COMPOUND)) {
            deserializeDecorationUnitsData(compound.getCompound("DecorationUnits"));
        }
    }

    @NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.getVehicleId());
        buffer.writeResourceLocation(this.getDisplayId());
        buffer.writeNbt(serializePartUnitsData());
        buffer.writeNbt(serializeDecorationUnitsData());
        buffer.writeInt(seats.size());
        for (Seat seat : seats) {
            buffer.writeInt(seat.passengerId);
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.vehicleId = buffer.readResourceLocation();
        this.displayId = buffer.readResourceLocation();
        this.initData();
        this.initDisplayData();
        deserializePartUnitsData(buffer.readNbt());
        deserializeDecorationUnitsData(buffer.readNbt());
        int[] passengerIdsBySeat = new int[buffer.readInt()];
        for(int index = 0; index < passengerIdsBySeat.length; index += 1) {
            passengerIdsBySeat[index] = buffer.readInt();
        }
        setSeats(passengerIdsBySeat);
    }

    public void writeData(CompoundTag data) {
        data.putString(ICustomVehicle.TAG_VEHICLE_ID, getVehicleId().toString());
        Entity driver = getDriver();
        if (driver != null) {
            data.putInt("driverId", driver.getId());
            Team team = driver.getTeam();
            if (team != null) {
                data.putString("teamName", team.getName());
            }
        }
        data.putBoolean("Destroyed", isDestroyed());
    }

    public void readData(CompoundTag data) {
        if (data.contains("driverId")) {
            controlUnit.setOperatorId(data.getInt("driverId"));
        }
        if (data.contains("teamName")) {
            remoteTeam = level().getScoreboard().getPlayerTeam(data.getString("teamName"));
        } else {
            remoteTeam = null;
        }
        if (data.contains("Destroyed")) {
            entityData.set(DESTROYED, data.getBoolean("Destroyed"));
        }
    }

    public void remoteTick() {}

    private CompoundTag serializePartUnitsData() {
        CompoundTag partUnitsTag = new CompoundTag();
        partUnits.forEach((partUnit -> {
            CompoundTag partTag = partUnit.serializeNBT();
            if (partTag.isEmpty()) {
                return;
            }
            partUnitsTag.put(partUnit.getId(), partTag);
        }));
        return partUnitsTag;
    }

    private void deserializePartUnitsData(CompoundTag partUnitsTag) {
        if (partUnitsTag != null) {
            partUnits.forEach(partUnit -> {
                if (partUnitsTag.contains(partUnit.getId(), Tag.TAG_COMPOUND)) {
                    CompoundTag partTag = partUnitsTag.getCompound(partUnit.getId());
                    partUnit.deserializeNBT(partTag);
                }
            });
        }
    }

    private CompoundTag serializeDecorationUnitsData() {
        CompoundTag decorationUnitsTag = new CompoundTag();
        decorationUnits.values().forEach((decorationUnit -> {
            CompoundTag partTag = decorationUnit.serializeNBT();
            if (partTag.isEmpty()) {
                return;
            }
            decorationUnitsTag.put(decorationUnit.getId(), partTag);
        }));
        return decorationUnitsTag;
    }

    private void deserializeDecorationUnitsData(CompoundTag decorationUnitsTag) {
        if (decorationUnitsTag == null) {
            return;
        }
        for (String id : decorationUnitsTag.getAllKeys()) {
            if (decorationUnitsTag.contains(id, Tag.TAG_COMPOUND)) {
                PartUnitPojo partUnitPojo = new PartUnitPojo();
                partUnitPojo.id = id;
                DecorationUnit decorationUnit = new DecorationUnit(id.hashCode(), this, new PartUnitData(partUnitPojo));
                CompoundTag partTag = decorationUnitsTag.getCompound(id);
                decorationUnit.deserializeNBT(partTag);
                decorationUnits.put(id, decorationUnit);
            }
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide()) {
            if (!dataInitialized) {
                initData();
            }
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        partUnits.forEach((PartUnit::onRemoved));
    }

    public void initDisplayData() {
        ClientAssetsManager.INSTANCE.getVehicleDisplay(this.getDisplayId()).ifPresent(this::initDisplayData);
    }

    public void initData() {
        CommonAssetsManager.vehicleDataManager().getVehicleData(this.getVehicleId()).ifPresent(this::initData);
    }

    public void initDisplayData(BaseDisplay display) {}

    public void initData(BaseVehicleData vehicleData) {
        if (vehicleData == null) {
            YwzjVehicle.LOGGER.error("No vehicle data found for {}", vehicleId);
            this.discard();
            return;
        }
        if (getHealth() < 0) {
            setMaxHealth(vehicleData.getMaxHealth());
            setHealth(vehicleData.getMaxHealth());
        }
        this.name = vehicleData.getName();
        this.viewInfo = vehicleData.getViewInfo();
        this.energyInfo = vehicleData.getEnergyInfo();
        this.physicsEngine.mass = vehicleData.getPhysicsInfo().mass;
        this.physicsEngine.canDestroyBlock = vehicleData.getPhysicsInfo().canDestroyBlock;
        this.physicsEngine.radarCrossSection = vehicleData.getPhysicsInfo().radarCrossSection;
        this.defenseStats = vehicleData.getDefenseStats();
        this.centerOffset = vehicleData.getCenterOffset();
        vehicleData.inject(this);
        VehicleStructOBBs vehicleStruct = vehicleData.getVehicleStructObbs();
        this.vehicleCubeOBBs = vehicleStruct.obbs();
        this.mainCubeOBB = vehicleStruct.mainCubeOBB();
        this.structureLength = vehicleData.getStructureLength();
        BaseVehicleData.PartUnitsAndSeats partUnitsAndSeats = vehicleData.createPartUnits(this);
        this.partUnits.addAll(partUnitsAndSeats.partUnitMap().values());
        this.seats.addAll(partUnitsAndSeats.seats());
        if (vehicleData.withWarningReceiver()) {
            this.warningReceiver = new WarningReceiver(this);
        }
        this.protectPassenger = vehicleData.isProtectPassenger();
        Map<String, PartUnit<?>> map = new HashMap<>();
        for (PartUnit<?> partUnit : partUnits) {
            map.put(partUnit.getId(), partUnit);
        }
        this.partUnitMap = map;
        updateOBBs();
        this.dataInitialized = true;
    }

    /**
     * 获取载具自定义配置ID，默认会是载具注册ID
     */
    @NotNull
    @Override
    public ResourceLocation getVehicleId() {
        return this.vehicleId;
    }

    @Override
    public void setVehicleId(@NotNull ResourceLocation vehicleId) {
        this.vehicleId = vehicleId;
    }

    @Override
    public ResourceLocation getDisplayId() {
        if (this.displayId == null) {
            return this.vehicleId;
        }
        return this.displayId;
    }

    @Override
    public void setDisplayId(ResourceLocation displayId) {
        this.displayId = displayId;
        if (!level().isClientSide()) {
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleChangeDisplay(this.getId(), displayId));
        }
    }

    @Override
    public Component getDisplayName() {
        return name;
    }

    @Override
    public void tick() {
        super.tick();
        tickPosAndRot();
        if (!this.isRemoved()) {
            aiStep();
        }
        tickParts();
        tickDecorations();
        updateOBBs();
        if (level().isClientSide()) {
            tickSound();
            tickParticle();
            if (warningReceiver != null) {
                warningReceiver.tick();
            }
        } else {
            if (tickCount == 1) {
                for (Entity passenger : new ArrayList<>(getPassengers())) {
                    passenger.stopRiding();
                }
            }
            if (isDestroyed() && System.currentTimeMillis() - destroyedTime > 60000) {
                this.discard();
            }
            tickEnergy();
            tickPower();
            tickEngineSpeed();
            tickPhysics(tickMove());
            if (MinecraftForge.EVENT_BUS.post(new VehicleMoveEvent(this))) {
                this.setDeltaMovement(Vec3.ZERO);
            }
            if (uav) {
                EntityUtil.keepChunkLoaded(this, position());
                EntityUtil.keepChunkLoaded(this, position().add(getLookAngle().normalize().scale(16)));
            }
        }
        afterVehicleRot();
    }

    protected void tickEnergy() {
        getCapability(VehicleCapabilityProvider.CAPABILITY).ifPresent(cap -> {
            float fuel = cap.getFuel();
            fuel = Math.max(0, fuel - energyInfo.energyConsumptionPerTick * getPower() / 100);
            physicsEngine.mass = curbWeight + fuel;
            entityData.set(ENERGY, fuel);
            setEnergy(fuel);
        });
    }

    protected void tickPower() {
        FluidState fluidState = level().getFluidState(BlockPos.containing(new Vec3(mainCubeOBB.obb().center())));
        if (!fluidState.isEmpty()) {
            setPower(0);
            return;
        }
        if (getEnergy() == 0) {
            setPower(0);
            return;
        }
        if (isDestroyed()) {
            setPower(Math.max(getPower() - 2, 0));
            return;
        }
        setPower(Mth.clamp(getPower() + (isEngineOn() ? 1 : -1), 0, 100));
    }

    protected void tickEngineSpeed() {
        float engineSpeed = getEngineSpeed();
        if (hasPower() && engineSpeed <= 60) {
            setEngineSpeed(Math.max(engineSpeed + (isEngineOn() ? 1 : -1), 0));
        }
    }

    protected void tickPhysics(Vec3 force) {
        Vector3f[] axes = mainCubeOBB.obb().getAxes();
        // 车体大OBB的表面采样点
        List<VehicleCubeOBB.CubePoint> surfacePoints = mainCubeOBB.cubePoints();
        // 接触方块的采样点
        List<VehicleCubeOBB.CubePoint> touchPoints = new ArrayList<>();

        for (VehicleCubeOBB.CubePoint point : surfacePoints) {
            Vec3 worldPos = new Vec3(point.worldPos(axes));
            BlockPos blockPos = BlockPos.containing(worldPos);

            // 调试
//            DebugUtil.particle(level(), new Vec3(worldPos), point.cubeFace());
//            DebugUtil.particle(level(), new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()));

            BlockState blockState = level().getBlockState(blockPos);
            if (blockState.isSolid()) {
                point.cubePointContext.setBlockPos(blockPos);
                point.cubePointContext.setBlockState(blockState);
                touchPoints.add(point);
            }
        }
        MinecraftForge.EVENT_BUS.post(new VehicleCollectCollisionEvent(this, touchPoints));

        // 调试
//        touchPoints.forEach(p -> DebugUtil.particle(level(), new Vec3(p.worldPos(axes)), p.cubeFace()));
//        touchPoints.forEach(p -> {
//            BlockPos blockPos = p.cubePointContext.blockPos();
//            DebugUtil.particle(level(), new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), p.cubeFace());
//        });

        // 碰撞
        Vec3 velocity = physicsEngine.motionByImpact(touchPoints, axes, getDeltaMovement());
        // 阻力
        velocity = physicsEngine.decelerationByFriction(touchPoints, velocity);
        // 重力与旋转
        velocity = physicsEngine.rotAndFallByGravity(touchPoints, new Vector3f(0, 0, 0), axes, force.toVector3f(), velocity.toVector3f());
        physicsEngine.velocityO = physicsEngine.velocity;

        setDeltaMovement(velocity);

//        if (this instanceof Ztz99a) {
//            DebugUtil.particle(level(), ((WeaponUnit)partUnits.get(0)).worldCurrentBoltPosition());
//            DebugUtil.particle(level(), ((WeaponUnit)partUnits.get(0)).aimContext().position);
//
//            DebugUtil.particle(level(), ((WeaponUnit)partUnits.get(0)).getSubWeaponUnits().get(2).worldCurrentBoltPosition());
//            DebugUtil.particle(level(), ((WeaponUnit)partUnits.get(0)).getSubWeaponUnits().get(2).aimContext().position);

//            DebugUtil.particle(level(), ((WeaponUnit)partUnits.get(0)).weapons.get(2).getWeaponUnit().worldCurrentBoltPosition());
//            DebugUtil.particle(level(), ((WeaponUnit)partUnits.get(0)).weapons.get(2).getWeaponUnit().aimContext().position);

//            DebugUtil.particle(level(), ((WeaponUnit)partUnits.get(0)).getCurrentWeapon().get().getWeaponUnit().worldCurrentBoltPosition());
//            DebugUtil.particle(level(), ((WeaponUnit)partUnits.get(0)).getCurrentWeapon().get().getWeaponUnit().aimContext().position);

//            DebugUtil.particle(level(), ((WeaponUnit)seats.get(0).partUnit).ammoSpawnPosition());
//            DebugUtil.particle(level(), ((WeaponUnit)seats.get(0).partUnit).worldOwnerViewPosition());
//            DebugUtil.particle(level(), ((WeaponUnit)seats.get(0).partUnit).worldOpticalSightPosition());
//            DebugUtil.particle(level(), seats.get(0).partUnit.worldSeatPosition());
//        }

    }

    @Override
    public void move(@NotNull MoverType pType, Vec3 pPos) {
        this.setPos(this.getX() + pPos.x, this.getY() + pPos.y, this.getZ() + pPos.z);
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return super.isInvulnerableTo(source) || source.is(DamageTypes.PLAYER_ATTACK);
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        if (MinecraftForge.EVENT_BUS.post(new VehicleAttackEvent(this, damageSource, amount))) {
            return false;
        }
        if (this.isInvulnerableTo(damageSource)) {
            return false;
        } else {
            if (!level().isClientSide()) {
                this.level().broadcastDamageEvent(this, damageSource);
            }
            DamageSystem.hurt(damageSource, amount, this);
            if (this.getHealth() <= 0) {
                if (isDestroyed()) {
                    this.discard();
                } else {
                    this.getPassengers().forEach(Entity::stopRiding);
                    entityData.set(DESTROYED, true);
                    this.setHealth(this.getMaxHealth());
                    destroyedTime = System.currentTimeMillis();
                    VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), damageSource.getEntity(), this, this.position(),
                            (float) mainCubeOBB.depth, AllConfigs.common.vehicleExplosionHurtPassengerDamage.get().floatValue(), false, false);
                    vehicleExplosion.explode(Collections.singletonList(this));
                }
            }
            this.markHurt();
            return true;
        }
    }

    public int passengerCapacity() {
        return seats.size();
    }

    public SoundEvent getEngineStartSound() {
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(getDisplayId());
        return displayOptional.map(display -> display.getSoundEvents().get("engine_start")).orElse(null);
    }

    public SoundEvent getEngineStopSound() {
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(getDisplayId());
        return displayOptional.map(display -> display.getSoundEvents().get("engine_stop")).orElse(null);
    }

    public SoundEvent getEngineIdleSound() {
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(getDisplayId());
        return displayOptional.map(display -> display.getSoundEvents().get("engine_idle")).orElse(null);
    }

    public SoundEvent getEngineRunSound() {
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(getDisplayId());
        return displayOptional.map(display -> display.getSoundEvents().get("engine_run")).orElse(null);
    }

    public SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
        return AllSounds.VEHICLE_HURT.get();
    }

    public void initPartUnits() {}

    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {
        if (isDestroyed() && tickCount % 20 == 0) {
            Level level = this.level();
            level.playLocalSound(
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.BLOCKS,
                    0.6F + level.random.nextFloat() * 0.4F,
                    0.8F + level.random.nextFloat() * 0.4F,
                    false
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void tickParticle() {
        if (isDestroyed() && tickCount % 20 == 0) {
            AABB aabb = getBoundingBox();
            Level level = this.level();
            if (level.isClientSide()) {
                for (int i = 0; i < 5; i++) {
                    double x = Mth.nextDouble(RandomSource.create(), aabb.minX, aabb.maxX);
                    double y = Mth.nextDouble(RandomSource.create(), aabb.minY, aabb.maxY);
                    double z = Mth.nextDouble(RandomSource.create(), aabb.minZ, aabb.maxZ);
                    double vx = (level.random.nextDouble() - 0.5D) * 0.02D;
                    double vy = level.random.nextDouble() * 0.05D + 0.02D;
                    double vz = (level.random.nextDouble() - 0.5D) * 0.02D;
                    level.addParticle(ParticleTypes.LARGE_SMOKE, true, x, y, z, vx, vy, vz);
                }
            }
        }
    }

    protected abstract Vec3 tickMove();

    protected void tickPosAndRot() {
        if (!level().isClientSide()) {
            if (this.xRotO == this.xRot && this.yRotO == this.yRot && !finalRotUpdate) {
                triggerRotUpdate();
                finalRotUpdate = true;
            } else {
                finalRotUpdate = false;
            }
        }
        this.xRotO = this.xRot;
        this.yRotO = this.yRot;
        this.zRotO = this.zRot;
        if (level().isClientSide()) {
            if (this.lerpSteps > 0) {
                double dX = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
                double dY = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
                double dZ = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
                float dXRot = Mth.wrapDegrees(lerpXRot - this.getXRot()) / this.lerpSteps;
                float dYRot = Mth.wrapDegrees(lerpYRot - this.getYRot()) / this.lerpSteps;
                float dZRot = Mth.wrapDegrees(lerpZRot - this.getZRot()) / this.lerpSteps;
                this.setXRot(this.getXRot() + dXRot);
                this.setYRot(this.getYRot() + dYRot);
                this.setZRot(this.getZRot() + dZRot);
                this.lerpSteps -= 1;
                this.setPos(dX, dY, dZ);
            }
        }
    }

    protected void tickParts() {
        partUnits.forEach(PartUnit::tick);
    }

    protected void tickDecorations() {
        decorationUnits.values().forEach(PartUnit::tick);
    }

    protected void afterVehicleRot() {
        float dXRot = xRot - xRotO;
        float dYRot = yRot - yRotO;
        float dZRot = zRot - zRotO;
        if (dXRot != 0 || dYRot != 0) {
            partUnits.forEach(partUnit -> partUnit.withVehicleRot(dXRot, dYRot, dZRot));
        }
        if (level().isClientSide()) {
            Player player = LocalVehiclePlayer.instance.getPlayer();
            if (player.getVehicle() == this && (!driverXYRotControl || player != getDriver())) {
                boolean rotTp = viewInfo.passengerViewRot.rotByVehicleInThirdPerson && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON;
                boolean rotOp = viewInfo.passengerViewRot.rotByVehicleInOperator && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR;
                if (rotTp || rotOp) {
                    if (rotOp) {
                        player.xRotO = player.xRotO + dXRot;
                        player.setXRot(player.getXRot() + dXRot);
                    }
                    player.yRotO = player.yRotO + dYRot;
                    player.setYRot(player.getYRot() + dYRot);
                    player.setYBodyRot(player.yBodyRot + dYRot);
                }
            }
        } else {
            getPassengers().stream()
                    .filter(passenger -> !(passenger instanceof Player))
                    .forEach(passenger -> {
                        passenger.yRotO = passenger.yRotO + dYRot;
                        passenger.setYRot(passenger.getYRot() + dYRot);
                    });
        }
    }

    @Override
    public List<OBB> getOBBs() {
        List<OBB> vehicleOBBs = new ArrayList<>(this.vehicleCubeOBBs.stream().map(VehicleCubeOBB::obb).toList());
        for (PartUnit<?> partUnit : partUnits) {
            vehicleOBBs.addAll(partUnit.getOBBs());
        }
        return vehicleOBBs;
    }

    @Override
    public void updateOBBs() {
        List<VehicleCubeOBB> allCubeOBBS = new ArrayList<>(this.vehicleCubeOBBs);
        for (PartUnit<?> partUnit : partUnits) {
            allCubeOBBS.addAll(partUnit.getPartCubeOBBs());
        }
        allCubeOBBS.forEach(cubeOBB -> cubeOBB.update(this));
        mainCubeOBB.update(this);
    }

    public AABB getAABB() {
        if (remote) {
            return AABB.ofSize(position(), 1, 1, 1);
        }
        List<OBB> vehicleOBBs = getOBBs();
        if (vehicleOBBs.isEmpty()) {
            return AABB.ofSize(position(), 1, 1, 1);
        }
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (OBB obb : vehicleOBBs) {
            Vector3f[] vertices = obb.getVertices();
            for (Vector3f v : vertices) {
                if (v.x < minX) minX = v.x;
                if (v.y < minY) minY = v.y;
                if (v.z < minZ) minZ = v.z;
                if (v.x > maxX) maxX = v.x;
                if (v.y > maxY) maxY = v.y;
                if (v.z > maxZ) maxZ = v.z;
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    protected void addPassenger(Entity pPassenger) {
        if (pPassenger instanceof LivingEntity livingEntity) {
            onEnterVehicle(livingEntity);
            super.addPassenger(pPassenger);
        }
    }

    @Override
    protected void removePassenger(Entity pPassenger) {
        if (pPassenger instanceof LivingEntity livingEntity) {
            Vec3 dismountLocation;
            DoorUnit doorUnit = getNearestDoorUnit(livingEntity);
            if (doorUnit != null) {
                dismountLocation = doorUnit.worldPosition(doorUnit.getPivotOffset()).subtract(0, pPassenger.getEyeHeight() / 2, 0);
            } else {
                PartUnit<?> partUnit = getOwnOperatorUnit(livingEntity);
                dismountLocation = relativeRotPos(position().add(mainCubeOBB.obb().extents().x + 1, 1, partUnit != null ? partUnit.getSeatOffset().z : 0), false);
            }
            dismountLocations.put(livingEntity, dismountLocation);
            onLeaveVehicle(livingEntity);
            super.removePassenger(pPassenger);
        }
    }

    public void onEnterVehicle(LivingEntity livingEntity) {
        if (!level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level();
            Seat targetSeat = null;
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                // 优先取交互到的门所对应的乘位
                Vec3 eyePosition = serverPlayer.getEyePosition();
                PartUnit<?> partUnit = VectorUtil.hitPartUnit(this, eyePosition, eyePosition.add(serverPlayer.getLookAngle().scale(4)));
                if (partUnit instanceof DoorUnit doorUnit && doorUnit.getSeatUnitOfDoor() != null) {
                    Optional<Seat> doorSeat = seats.stream().filter(seat -> seat.partUnit == doorUnit.getSeatUnitOfDoor()).findFirst();
                    if (doorSeat.isPresent()) {
                        targetSeat = doorSeat.get();
                        if (targetSeat.passengerId != -1) {
                            return;
                        }
                    }
                }
            }
            if (targetSeat == null) {
                Optional<Seat> emptySeatOptional = seats.stream().filter(seat -> seat.passengerId == -1).findFirst();
                if (emptySeatOptional.isEmpty()) {
                    return;
                }
                targetSeat = emptySeatOptional.get();
            }
            if (targetSeat.seatIndex == 0) {
                controlUnit.setOperator(livingEntity);
                toggleEngine(true);
                partUnits.forEach(partUnit -> {
                    if (partUnit instanceof DoorUnit doorUnit) {
                        doorUnit.setOn(false);
                    }
                });
            }
            targetSeat.partUnit.setOwner(livingEntity);
            targetSeat.passengerId = livingEntity.getId();
            for (ServerPlayer serverPlayer : serverLevel.players()) {
                Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ServerVehicleSeatsChange(this));
            }
        }
        livingEntity.setSprinting(false);
    }

    public void onLeaveVehicle(LivingEntity pPassenger) {
        if (!level().isClientSide()) {
            Optional<Seat> ownSeat = seats.stream().filter(seat -> seat.passengerId == pPassenger.getId()).findFirst();
            if (!ownSeat.isPresent()) {
                return;
            }
            Seat seat = ownSeat.get();
            if (seat.seatIndex == 0) {
                controlUnit.setOperator(null);
            }
            seat.partUnit.setOwner(null);
            seat.passengerId = -1;
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleSeatsChange(this));
        } else {
            if (warningReceiver != null) {
                warningReceiver.clear();
            }
        }
    }

    public boolean changeSeat(LivingEntity pPassenger, int toSeatIndex) {
        if (toSeatIndex <= seats.size() && seats.get(toSeatIndex).passengerId == -1) {
            Optional<Seat> ownSeat = seats.stream().filter(seat -> seat.passengerId == pPassenger.getId()).findFirst();
            if (ownSeat.isPresent()) {
                Seat seat = ownSeat.get();
                if (seat.seatIndex == 0) {
                    controlUnit.setOperator(null);
                }
                seat.partUnit.setOwner(null);
                seat.passengerId = -1;
            }
            Seat toSeat = seats.get(toSeatIndex);
            if (toSeat.seatIndex == 0) {
                controlUnit.setOperator(pPassenger);
                toggleEngine(true);
            }
            toSeat.partUnit.setOwner(pPassenger);
            toSeat.passengerId = pPassenger.getId();
            toSeat.partUnit.applySeatRot(pPassenger);
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleSeatsChange(this));
            return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private void setSeats(int[] ids) {
        Player player = LocalVehiclePlayer.instance.getPlayer();
        List<Integer> passengerIdsBySeat = new ArrayList<>();
        for (int id : ids) {
            passengerIdsBySeat.add(id);
        }
        if (seats.stream().anyMatch(seat -> seat.passengerId == player.getId())
                && !passengerIdsBySeat.contains(player.getId())) {
            LocalVehiclePlayer.instance.switchViewType(LocalVehiclePlayer.ViewType.THIRD_PERSON);
            LocalVehiclePlayer.instance.seat = null;
        }
        for (int index = 0; index < passengerIdsBySeat.size(); index += 1) {
            Seat seat = seats.get(index);
            Integer id = passengerIdsBySeat.get(index);
            if (id != -1) {
                if (index == 0) {
                    controlUnit.setOperatorId(id);
                }
                seat.partUnit.setOwnerId(id);
                seat.passengerId = id;
                if (seat.passengerId == player.getId()) {
                    seat.partUnit.applySeatRot(player);
                    LocalVehiclePlayer.instance.seat = seat;
                }
            } else {
                if (index == 0) {
                    controlUnit.setOperator(null);
                }
                seat.partUnit.setOwner(null);
                seat.passengerId = -1;
            }
        }
    }

    public void onClientVehicleChangeSeat(ClientVehicleChangeSeat message, Player player) {
        changeSeat(player, message.toSeat);
    }

    public void onClientVehicleAction(ClientVehicleAction message, Player player) {
        if (player != null && player.level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            if (message.leaveVehicle) {
                player.stopRiding();
            } else if (message.toggleEngine) {
                vehicle.toggleEngine(null);
            } else if (message.lockEntity) {
                PartUnit<?> partUnit = vehicle.getOwnOperatorUnit(player);
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    weaponUnit.setLockedEntity(player.level().getEntity(message.lockedEntityId));
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void onServerVehicleSeatsChange(ServerVehicleSeatsChange message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            vehicle.setSeats(message.passengerIdsBySeat);
        }
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity pPassenger) {
        return seats.stream().anyMatch(seat -> seat.passengerId == -1);
    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if (!this.level().isClientSide()) {
            if (isDestroyed()) {
                this.openCustomInventoryScreen(pPlayer);
                return InteractionResult.SUCCESS;
            }
            if (pHand == InteractionHand.MAIN_HAND) {
                ItemStack itemStack = pPlayer.getItemInHand(pHand);
                if (itemStack.getItem() instanceof VehicleItem) {
                    return InteractionResult.PASS;
                }
                if (pPlayer.startRiding(this)) {
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @OnlyIn(Dist.CLIENT)
    public Vec3 thirdPersonPosition(LivingEntity pPassenger, Float partialTick) {
        if (pPassenger != null) {
            Vec3 offset = isViewZoomed() ? getViewInfo().thirdPersonCenterOffsetZoomed : getViewInfo().thirdPersonCenterOffset;
            double distance = isViewZoomed() ? getViewInfo().thirdPersonDistanceZoomed : getViewInfo().thirdPersonDistance;
            Matrix3f axisRollMat = new Matrix3f();
            Quaternionf q = new Quaternionf();
            q.rotateY(Math.toRadians(-this.getYRot()));
            q.get(axisRollMat);
            Vector3f rotPos = axisRollMat.transform(offset.toVector3f());
            Vec3 p;
            float yRot;
            float xRot;
            if (partialTick == null) {
                p = this.position();
                yRot = pPassenger.yHeadRot;
                xRot = pPassenger.getXRot();
            } else {
                p = new Vec3(Mth.lerp(partialTick, xo, getX()),
                        Mth.lerp(partialTick, yo, getY()),
                        Mth.lerp(partialTick, zo, getZ()));
                yRot = Mth.lerp(partialTick, pPassenger.yHeadRotO, pPassenger.yHeadRot);
                xRot = Mth.lerp(partialTick, pPassenger.xRotO, pPassenger.getXRot());
            }
            Vec3 thirdPersonCenter = p.add(new Vec3(rotPos.x, rotPos.y, rotPos.z));
            axisRollMat = new Matrix3f();
            q = new Quaternionf();
            q.rotateY(Math.toRadians(-yRot));
            q.rotateX(Math.toRadians(xRot));
            q.get(axisRollMat);
            float d;
            if (isViewZoomed()) {
                d = (float) distance;
            } else {
                d = (float) Math.max(0, distance - pPassenger.getXRot() / 90 * offset.y);
            }
            Vector3f rotOffset = axisRollMat.transform(new Vector3f(0, 0, -d));
            Vec3 thirdPersonPos = thirdPersonCenter.add(rotOffset.x, rotOffset.y, rotOffset.z);
            int maxTry = 128;
            while (maxTry > 0) {
                maxTry -= 1;
                BlockHitResult blockHitResult = level().clip(new ClipContext(thirdPersonPos, thirdPersonCenter,
                        ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, pPassenger));
                if (blockHitResult.getType() != BlockHitResult.Type.MISS) {
                    thirdPersonPos = blockHitResult.getLocation();
                    Vec3 step = thirdPersonCenter.subtract(thirdPersonPos).normalize().scale(0.1);
                    thirdPersonPos = thirdPersonPos.add(step);
                } else {
                    Vec3 step = thirdPersonCenter.subtract(thirdPersonPos).normalize().scale(1);
                    thirdPersonPos = thirdPersonPos.add(step);
                    break;
                }
            }
            return thirdPersonPos;
        }
        return Vec3.ZERO;
    }

    @NotNull
    @Override
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity pPassenger) {
        return dismountLocations.getOrDefault(pPassenger, super.getDismountLocationForPassenger(pPassenger));
    }

    @Override
    protected void positionRider(@NotNull Entity pPassenger, Entity.MoveFunction pCallback) {
        if (!(pPassenger instanceof LivingEntity living)) {
            super.positionRider(pPassenger, pCallback);
            return;
        }
        PartUnit<?> partUnit = getOwnOperatorUnit(living);
        if (partUnit != null) {
            Vec3 pos = partUnit.worldSeatPosition();
            pCallback.accept(pPassenger, pos.x, pos.y, pos.z);
        } else {
            super.positionRider(pPassenger, pCallback);
        }
    }

    public LivingEntity getDriver() {
        return controlUnit.getOperator();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (level().isClientSide()) {
            return null;
        } else {
            return getDriver();
        }
    }

    public PartUnit<?> getOwnOperatorUnit(LivingEntity pPassenger) {
        if (pPassenger == null) {
            return null;
        }
        Optional<Seat> ownSeat = seats.stream().filter(seat -> seat.passengerId == pPassenger.getId()).findFirst();
        return ownSeat.map(seat -> seat.partUnit).orElse(null);
    }

    public void playVehicleSound(SoundEvent soundEvent, boolean on) {
        Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerSoundEvent(this.getId(), soundEvent.getLocation().getPath(), on));
    }

    public List<PartUnit<?>> getPartUnits() {
        return partUnits;
    }

    public Optional<PartUnit<?>> getPartUnit(int index) {
        if (index >= 0 && index < partUnits.size()) {
            return Optional.of(partUnits.get(index));
        }
        return Optional.empty();
    }

    public DoorUnit getNearestDoorUnit(LivingEntity livingEntity) {
        PartUnit<?> seatUnit = getOwnOperatorUnit(livingEntity);
        double minDistance = Double.MAX_VALUE;
        DoorUnit nearestDoorUnit = null;
        for (PartUnit<?> partUnit : getPartUnits()) {
            if (partUnit instanceof DoorUnit doorUnit) {
                if (doorUnit.getSeatUnitOfDoor() != null && doorUnit.getSeatUnitOfDoor() == seatUnit) {
                    return doorUnit;
                }
                double distance = partUnit.worldPosition(partUnit.getPivotOffset()).distanceTo(livingEntity.position());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestDoorUnit = doorUnit;
                }
            }
        }
        return nearestDoorUnit;
    }

    public Optional<PartUnit<?>> getPartUnit(String id) {
        return Optional.ofNullable(partUnitMap.get(id));
    }

    public Map<String, DecorationUnit> getDecorationUnits() {
        return decorationUnits;
    }

    public List<VehicleCubeOBB> getVehicleCubeOBBs() {
        return vehicleCubeOBBs;
    }

    public VehicleCubeOBB getMainCubeOBB() {
        return mainCubeOBB;
    }

    public double getStructureLength() {
        return structureLength;
    }

    public float getXRot() {
        return this.xRot;
    }

    public void setXRot(float rot) {
        if (!level().isClientSide()) {
            this.entityData.set(X_ROT, rot, true);
        }
        this.xRot = rot;
    }

    public float getYRot() {
        return this.yRot;
    }

    public void setYRot(float rot) {
        if (!level().isClientSide()) {
            this.entityData.set(Y_ROT, rot, true);
        }
        this.yRot = rot;
    }

    public float getZRot() {
        return this.zRot;
    }

    public void setZRot(float rot) {
        this.zRot = rot;
        if (!level().isClientSide()) {
            this.entityData.set(Z_ROT, this.zRot, true);
        }
    }

    public float getViewXRot(float pPartialTicks) {
        return pPartialTicks == 1.0F ? this.getXRot() : Mth.lerp(pPartialTicks, this.xRotO, this.getXRot());
    }

    public float getViewYRot(float pPartialTicks) {
        float dYRot = yRot - yRotO;
        if (Math.abs(dYRot) > 180) {
            yRotO += Math.signum(dYRot) * 360;
        }
        return pPartialTicks == 1.0F ? this.getYRot() : Mth.lerp(pPartialTicks, this.yRotO, this.getYRot());
    }

    public float getViewZRot(float pPartialTicks) {
        return pPartialTicks == 1.0F ? this.getZRot() : Mth.lerp(pPartialTicks, this.zRotO, this.getZRot());
    }

    public Quaternionf rotYXZ() {
        Quaternionf q = new Quaternionf();
        q.rotateY(Math.toRadians(-this.getYRot()))
                .rotateX(Math.toRadians(this.getXRot()))
                .rotateZ(Math.toRadians(this.getZRot()));
        return q;
    }

    /**
     * 某世界坐标随载具三轴旋转后或前的新坐标
     */
    public Vec3 relativeRotPos(Vec3 worldPos, boolean reverse) {
        return relativeRotDirection(worldPos.subtract(position()), reverse).add(position());
    }

    /**
     * 某世界坐标系下的向量随载具三轴旋转后或前的向量
     */
    public Vec3 relativeRotDirection(Vec3 worldDirection, boolean reverse) {
        Quaternionf q = rotYXZ();
        Matrix3f axisRollMat = new Matrix3f();
        q.get(axisRollMat);
        if (reverse) {
            axisRollMat = axisRollMat.transpose();
        }
        Vector3f relativePos = new Vector3f(
                (float) (worldDirection.x() - centerOffset.x),
                (float) (worldDirection.y() - centerOffset.y),
                (float) (worldDirection.z() - centerOffset.z)
        );
        axisRollMat.transform(relativePos);
        return new Vec3(
                relativePos.x + centerOffset.x,
                relativePos.y + centerOffset.y,
                relativePos.z + centerOffset.z
        );
    }

    public abstract void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator);

    public float getEnergy() {
        float amount = entityData.get(ENERGY);
        if (amount == 0 && AllConfigs.common.infiniteFuel.get()) {
            amount = Float.MIN_VALUE;
        }
        return amount;
    }

    public void setEnergy(float amount) {
        getCapability(VehicleCapabilityProvider.CAPABILITY).ifPresent(cap -> {
            cap.setFuel(amount);
            entityData.set(ENERGY, amount);
            physicsEngine.mass = curbWeight + amount;
        });
    }

    public float addEnergy(float amount) {
        float fuel = getEnergy();
        float space = energyInfo.energyCapacity - fuel;
        if (space > amount) {
            setEnergy(fuel + amount);
            return 0;
        } else {
            setEnergy(energyInfo.energyCapacity);
            return amount - space;
        }
    }

    public ViewInfo getViewInfo() {
        return viewInfo;
    }

    public boolean isViewZoomed() {
        return viewZoomed;
    }

    public void toggleViewZoom() {
        viewZoomed = !viewZoomed;
    }

    public float getPower() {
        return entityData.get(POWER);
    }

    public void setPower(float power) {
        entityData.set(POWER, power);
    }

    public float getEngineSpeed() {
        return entityData.get(ENGINE_SPEED);
    }

    public void setEngineSpeed(float engineSpeed) {
        entityData.set(ENGINE_SPEED, engineSpeed);
    }

    public boolean hasPower() {
        return getPower() > 20;
    }

    public void toggleEngine(Boolean on) {
        entityData.set(ENGINE_ON, on == null ? !isEngineOn() : on);
    }

    public boolean isEngineOn() {
        return entityData.get(ENGINE_ON);
    }

    public boolean isDestroyed() {
        return entityData.get(DESTROYED);
    }

    @Override
    public Team getTeam() {
        if (!remote) {
            Entity driver = getDriver();
            if (driver == null) {
                return null;
            }
            return driver.getTeam();
        } else {
            return remoteTeam;
        }
    }

    public Matrix4f getWheelsTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        return transform;
    }

    public Vector4f transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
    }

    @Override
    public void push(@NotNull Entity pEntity) {
        if (this.isPassengerOfSameVehicle(pEntity)) {
            return;
        }
        if (pEntity instanceof AbstractVehicle vehicle) {
            VehicleCubeOBB bodyCube = vehicle.getMainCubeOBB();
            if (!OBB.isColliding(bodyCube.obb(), this.getMainCubeOBB().obb())) {
                return;
            }
        } else {
            if (!getMainCubeOBB().obb().contains(pEntity.getEyePosition())) {
                return;
            }
        }
        impact(pEntity);
        if (pEntity instanceof AbstractVehicle vehicle) {
            double d0 = pEntity.getX() - this.getX();
            double d1 = pEntity.getZ() - this.getZ();
            double d2 = Mth.absMax(d0, d1);
            if (d2 >= (double)0.01F) {
                d2 = Math.sqrt(d2);
                d0 /= d2;
                d1 /= d2;
                double d3 = 1.0D / d2;
                if (d3 > 1.0D) {
                    d3 = 1.0D;
                }
                d0 *= d3;
                d1 *= d3;
                d0 *= 0.05F;
                d1 *= 0.05F;
                if (vehicle.isPushable()) {
                    vehicle.push(d0, 0.0D, d1);
                }
            }
        }
    }

    public void support(Entity pEntity) {
        if (pEntity.noPhysics || this.noPhysics) {
            return;
        }
        AABB entityAABB = pEntity.getBoundingBox();
        Vec3 movement = pEntity.getDeltaMovement();
        for (OBB obb : getOBBs()) {
            if (!OBB.isColliding(obb, entityAABB)) {
                continue;
            }
            Vec3 mtv = new Vec3(obb.calculateMTV(entityAABB));
            if (mtv.lengthSqr() > 0) {
                if (Math.abs(mtv.y) > Math.abs(mtv.x) && Math.abs(mtv.y) > Math.abs(mtv.z)) {
                    if (mtv.y > 0) {
                        pEntity.setOnGround(true);
                        pEntity.fallDistance = 0;
                    }
                    pEntity.setDeltaMovement(movement.x, this.getDeltaMovement().y, movement.z);
                }
                mtv = new Vec3(Math.abs(mtv.x) < 0.001 ? this.getDeltaMovement().x : mtv.x,
                        Math.abs(mtv.y) < 0.001 ? this.getDeltaMovement().y : mtv.y,
                        Math.abs(mtv.z) < 0.001 ? this.getDeltaMovement().z : mtv.z);
                Vec3 toPos = new Vec3(pEntity.getX() + mtv.x,
                        pEntity.getY() + mtv.y,
                        pEntity.getZ() + mtv.z);
                pEntity.setPos(toPos);
            }
        }
    }

    public void impact(Entity entity) {
        if (this.equals(entity.getVehicle())) {
            return;
        }
        LivingEntity driver = getDriver();
        if (entity instanceof TamableAnimal tamableAnimal) {
            if (tamableAnimal.getOwner() == driver) {
                return;
            }
        }
        double velocity = this.getDeltaMovement().length();
        double entityVelocity = entity.getDeltaMovement().dot(this.getDeltaMovement()) / velocity;
        double relVelocity = (velocity - entityVelocity) * 20;
        if (relVelocity > 1) {
            entity.hurt(AllDamageTypes.Sources.vehicleCollision(level().registryAccess(), this, this.getDriver(), null),
                    (float) relVelocity * curbWeight);
        }
    }

    public void triggerRotUpdate() {
        ClientboundMoveEntityPacket.Rot packet = new ClientboundMoveEntityPacket.Rot(this.getId(), (byte) 0, (byte) 0, this.onGround());
        ((ServerLevel) this.level()).getChunkSource().broadcast(this, packet);
    }

    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYaw, float pPitch, int pPosRotationIncrements, boolean pTeleport) {
        super.lerpTo(pX, pY, pZ, pYaw, pPitch, pPosRotationIncrements, pTeleport);
        this.lerpXRot = entityData.get(X_ROT);
        this.lerpYRot = entityData.get(Y_ROT);
        this.lerpZRot = entityData.get(Z_ROT);
    }

    public void aiStep() {
        Vec3 v = this.getDeltaMovement();
        double dx = v.x;
        double dy = v.y;
        double dz = v.z;
        if (Math.abs(v.x) < 0.001D) {
            dx = 0.0D;
        }
        if (Math.abs(v.y) < 0.001D) {
            dy = 0.0D;
        }
        if (Math.abs(v.z) < 0.001D) {
            dz = 0.0D;
        }
        this.setDeltaMovement(dx, dy, dz);

        this.level().getProfiler().push("travel");
        {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        this.level().getProfiler().pop();

        this.level().getProfiler().push("push");
        {
            this.pushEntities();
        }
        this.level().getProfiler().pop();
    }

    public static class Seat {

        public final Integer seatIndex;
        public final PartUnit<?> partUnit;
        public Integer passengerId;

        public Seat(Integer seatIndex, PartUnit<?> partUnit) {
            this.seatIndex = seatIndex;
            this.partUnit = partUnit;
            this.passengerId = -1;
        }

    }

//    @Deprecated
//    protected void initOBBs() {
//        BedrockModel model = CommonAssetsManager.structureModelManager().getStructureModel(this.getStructureModel()).orElse(null);
//        BedrockBone bone = model.getBoneMap().get("vehicle_body");
//        // 约定取体积最大的块计算物理
//        List<BedrockCube> cubes = new ArrayList<>(bone.cubes.stream().toList());
//        cubes.sort((cube1, cube2) -> (int) -(cube1.depth() * cube1.width() * cube1.height() - cube2.depth() * cube2.width() * cube2.height()));
//        mainCubeOBB = VehicleBedrockCubeOBB.init(bone, cubes.remove(0));
//        vehicleOBBs.add(mainCubeOBB);
//        for (BedrockCube cube : cubes) {
//            vehicleOBBs.add(VehicleBedrockCubeOBB.init(bone, cube));
//        }
//        for (BedrockBone child : bone.getChildren()) {
//            List<BedrockCube> childCubes = new ArrayList<>(child.cubes.stream().toList());
//            for (BedrockCube cube : childCubes) {
//                vehicleOBBs.add(VehicleBedrockCubeOBB.init(child, cube));
//            }
//        }
//    }

    @Deprecated
    @Nullable
    public ResourceLocation getStructureModel() {
        ResourceLocation id = this.getVehicleId();
        return YwzjVehicle.resourceLocation(id.getNamespace() + ":entity/" + id.getPath());
    }

}
