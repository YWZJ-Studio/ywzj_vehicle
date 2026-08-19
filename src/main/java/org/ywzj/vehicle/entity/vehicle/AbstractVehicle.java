package org.ywzj.vehicle.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllDamageTypes;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.entity.DetachedBodyVehicle;
import org.ywzj.vehicle.api.entity.ICustomVehicle;
import org.ywzj.vehicle.api.entity.OBBEntity;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.api.collision.CollisionProvider;
import org.ywzj.vehicle.api.collision.CollisionProviders;
import org.ywzj.vehicle.api.event.VehicleAttackEvent;
import org.ywzj.vehicle.api.event.VehicleCollectCollisionEvent;
import org.ywzj.vehicle.api.event.VehicleMoveEvent;
import org.ywzj.vehicle.client.particle.BulletHoleParticle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.part.data.PartUnitPojo;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.ContainerCraft;
import org.ywzj.vehicle.item.VehicleItem;
import org.ywzj.vehicle.network.message.*;
import org.ywzj.vehicle.util.*;
import org.ywzj.vehicle.vehicle.DamageSystem;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PhysicsEngine;
import org.ywzj.vehicle.vehicle.PhysicsRig;
import org.ywzj.vehicle.vehicle.PhysicsTrace;
import org.ywzj.vehicle.vehicle.VehicleRighting;
import org.ywzj.vehicle.vehicle.schedule.VehiclePhysicsJob;
import org.ywzj.vehicle.vehicle.schedule.VehiclePhysicsScheduler;
import org.ywzj.vehicle.vehicle.collision.BoxBuffer;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;
import org.ywzj.vehicle.vehicle.collision.ContactSynthesis;
import org.ywzj.vehicle.vehicle.collision.SweptHull;
import org.ywzj.vehicle.vehicle.control.ControlUnit;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
import org.ywzj.vehicle.vehicle.part.DoorUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.parenting.CarrierDecks;
import org.ywzj.vehicle.vehicle.parenting.CarrierLink;
import org.ywzj.vehicle.vehicle.parenting.DeckAttachment;
import org.ywzj.vehicle.vehicle.parenting.DeckSnapshot;
import org.ywzj.vehicle.vehicle.parenting.VehicleHarness;
import org.ywzj.vehicle.vehicle.parenting.VehicleParenting;
import org.ywzj.vehicle.vehicle.passenger.WarningReceiver;
import org.ywzj.vehicle.vehicle.pojo.AimContext;
import org.ywzj.vehicle.vehicle.pojo.DefenseStats;
import org.ywzj.vehicle.vehicle.pojo.EnergyInfo;
import org.ywzj.vehicle.vehicle.pojo.ViewInfo;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;
import org.ywzj.vehicle.vehicle.structure.VehicleStructOBBs;

import java.util.*;

public abstract class AbstractVehicle extends ContainerCraft
        implements RemoteTickEntity, OBBEntity, ICustomVehicle, IEntityWithComplexSpawn, DetachedBodyVehicle {

    public static final EntityDataAccessor<Float> X_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> Y_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> Z_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENERGY = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENGINE_SPEED = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> ENGINE_ON = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DESTROYED = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<CompoundTag> DETACHED_ANCHORS = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.COMPOUND_TAG);
    /**
     * Entity id of the carrier this vehicle is asleep on, or -1. Synced to prevent the client
     * interpolating a parked aircraft and its ship separately, causing them to drift apart visually.
     */
    public static final EntityDataAccessor<Integer> HARNESS_CARRIER = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.INT);
    private static final String DETACHED_ANCHORS_TAG = "DetachedAnchors";
    protected ResourceLocation vehicleId;
    protected ResourceLocation displayId;
    private BakedModelInstance modelInstance;
    private Component name;
    public final ControlUnit controlUnit;
    public List<Seat> seats;
    protected final List<PartUnit<?>> partUnits;
    protected Map<String, PartUnit<?>> partUnitMap;
    protected final Map<String, DecorationUnit> decorationUnits;
    protected final HashSet<BulletHoleParticle> bulletHoleParticles;
    protected ViewInfo viewInfo;
    protected boolean viewZoomed;
    public EnergyInfo energyInfo;
    public DefenseStats defenseStats;
    public Vec3 centerOffset;
    public float curbWeight;
    public Vec3 deltaMovementO;
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
    private List<OBB> cachedOBBs = List.of();
    private List<OBB> activeOBBs = List.of();
    private ChunkCollisionCache.Cursor collisionCursor;
    private final BoxBuffer scratchBoxes = new BoxBuffer();
    // Providers hand back real AABBs, cached and reused across ticks rather than rebuilt.
    private final List<AABB> providerBoxes = new ArrayList<>();
    // Hull swept against world, rewritten per substep; must not live on shared cube groups.
    private final OBB sweepHull = new OBB(new Vector3f(), new Vector3f(), new Quaternionf());
    private final SweptHull.Broadphase sweptBroadphase = new SweptHull.Broadphase();
    private final OBB.SatFrame sweepFrame = new OBB.SatFrame();
    @Nullable
    private AABB preparedBounds;
    private boolean tickAnySolidNearby;
    private final OBB.SatFrame supportFrame = new OBB.SatFrame();
    private final Vector3f supportMtv = new Vector3f();
    private final Quaternionf cachedRotYXZ = new Quaternionf();
    private float cachedRotYXZYaw = Float.NaN;
    private float cachedRotYXZPitch = Float.NaN;
    private float cachedRotYXZRoll = Float.NaN;
    // Margin in blocks when collecting collision sections; 2.0 covers sample overshoot and block rounding.
    private static final double SAMPLE_GATE_MARGIN = 2.0;
    // Flattened body and part geometry, indexed once at startup; cube lists are static after initData.
    private List<VehicleCubeOBB> allCubeOBBs;
    private int[] cubeGroupIndex = new int[0];
    /** Owning part unit per entry of allCubeOBBs, or -1 for body cubes. */
    private int[] cubePartIndex = new int[0];
    private boolean[] cubeDetached = new boolean[0];
    // Group forest in topological order; transforms must not live on shared cube groups.
    private VehicleCubeGroup[] groupOrder = new VehicleCubeGroup[0];
    private int[] groupParent = new int[0];
    private Vector3f[] groupOffset = new Vector3f[0];
    private Quaternionf[] groupRotation = new Quaternionf[0];
    // Vehicle-local bounds of all OBBs, used to compute bounding box by rotating corners.
    private double localMinX, localMinY, localMinZ, localMaxX, localMaxY, localMaxZ;
    private boolean localBoundsValid;
    private final Matrix3f scratchLocalRot = new Matrix3f();
    /**
     * Every structure cube's vehicle-local axis-aligned bound, six floats each. Mesh entities
     * standing on this vehicle collide against these. Filled by updateOBBs at no extra cost.
     * mainCubeOBB is deliberately not in here; it is the hull's bounding volume, which for
     * anything wing-shaped is too large to stand on. Double-buffered and published by reference:
     * updateOBBs fills the buffer that is not currently published, then stores it into deckSnapshot.
     */
    private final DeckSnapshot[] deckSnapshotBuffers = {new DeckSnapshot(), new DeckSnapshot()};
    private int deckSnapshotSlot;
    private volatile DeckSnapshot deckSnapshot = DeckSnapshot.EMPTY;
    /** Count of allCubeOBBs declared as landing surface. */
    private int deckCubes;
    /**
     * This tick's frozen view of the carrier this vehicle is flying over, driving on or landing on.
     * Per vehicle and rewritten on the tick thread, so the solve reads numbers nobody else is touching.
     */
    private final CarrierLink carrierLink = new CarrierLink();
    /** Set while this vehicle's pose is derived from a carrier's rather than solved. */
    @Nullable
    private VehicleHarness harness;
    /** Consecutive ticks this vehicle has been parked on a deck, counting towards harness. */
    private int harnessDwell;
    /** Vehicles asleep on this one. */
    private final List<AbstractVehicle> harnessedVehicles = new ArrayList<>(0);
    /** Whether this vehicle is currently in its level's carrier list. */
    private boolean carrierRegistered;
    /** Entities parented to this vehicle. */
    private final List<Entity> deckRiders = new ArrayList<>();
    // Resolve passes over the OBB set in support(). Overlapping part boxes can each push the
    // entity, so one pass leaves it displaced by their sum; re-testing until nothing overlaps
    // converges instead. Bounded so pathological geometry cannot spin here.
    private static final int SUPPORT_RESOLVE_PASSES = 4;
    // Upper bound on movement substeps, so a fast vehicle cannot multiply per-tick cost without limit
    private static final int MAX_COLLISION_SUBSTEPS = 16;
    // Maximum displacement per collision step, in blocks; half block width hides between samples.
    private static final double SAFE_STEP = 0.5;
    // Clearance above max step height in sweep hull; prevents clipping on boundary-height steps.
    private static final double SWEEP_STEP_MARGIN = 0.05;
    protected double structureLength;
    public WarningReceiver warningReceiver;
    public PhysicsEngine physicsEngine;
    /** The working pose the physics pipeline runs against, sync or async. */
    private final PhysicsRig physicsRig = new PhysicsRig();
    /** World view frozen for this vehicle's async solve. */
    private final ChunkCollisionCache.PinnedSections pinnedSections =
            new ChunkCollisionCache.PinnedSections();
    /** Engine state as of submit, restored if the solve is discarded. */
    private final PhysicsEngine.State engineStateSnapshot = new PhysicsEngine.State();
    /** The solve in flight between this vehicle's tick and the level barrier, or null. */
    @Nullable
    private VehiclePhysicsJob physicsJob;
    /** Recoils fired while a solve owned the engine state; run at the barrier. */
    private final List<Runnable> pendingRecoils = new ArrayList<>(0);
    /** Physics trace recorder if one was requested; null otherwise. */
    @Nullable
    private PhysicsTrace physicsTrace;
    /** Consecutive ticks players have been shoving this hull back onto its wheels. */
    public int rightingHold;
    private final HashMap<LivingEntity, Vec3> dismountLocations;
    protected boolean driverXYRotControl = false;
    public boolean uav = false;
    public boolean collision = true;
    public boolean remote = false;
    public PlayerTeam remoteTeam;
    public boolean protectPassenger;
    protected boolean dataInitialized;
    protected int destroyedTick;
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
        this.bulletHoleParticles = new HashSet<>();
        this.vehicleCubeOBBs = new ArrayList<>();
        this.curbWeight = 1;
        this.viewInfo = new ViewInfo();
        this.energyInfo = new EnergyInfo();
        this.physicsEngine = new PhysicsEngine(this);
        this.dismountLocations = new HashMap<>();
        // A remotely operated vehicle reaches the client's entity list before its chunk is streamed,
        // so the HUD can draw it before it has ever ticked client-side.
        this.deltaMovementO = Vec3.ZERO;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(X_ROT, 0f);
        builder.define(Y_ROT, 0f);
        builder.define(Z_ROT, 0f);
        builder.define(ENERGY, 0f);
        builder.define(POWER, 0f);
        builder.define(ENGINE_SPEED, 0f);
        builder.define(ENGINE_ON, false);
        builder.define(DESTROYED, false);
        builder.define(DETACHED_ANCHORS, new CompoundTag());
        builder.define(HARNESS_CARRIER, -1);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Engine", isEngineOn());
        compound.putFloat("Energy", getEnergy());
        compound.putFloat("Power", getPower());
        compound.putBoolean("Destroyed", isDestroyed());
        compound.putInt("DestroyedTick", destroyedTick);
        compound.putString(ICustomVehicle.TAG_VEHICLE_ID, this.getVehicleId().toString());
        compound.putString(ICustomVehicle.TAG_VEHICLE_DISPLAY_ID, this.getDisplayId().toString());
        compound.put("PartUnits", serializePartUnitsData());
        compound.put("DecorationUnits", serializeDecorationUnitsData());
        CompoundTag anchors = entityData.get(DETACHED_ANCHORS);
        if (!anchors.isEmpty()) {
            compound.put(DETACHED_ANCHORS_TAG, anchors.copy());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Engine")) {
            toggleEngine(compound.getBoolean("Engine"));
        }
        if (compound.contains("Energy")) {
            setEnergy(compound.getFloat("Energy"));
        }
        if (compound.contains("Power")) {
            setPower(compound.getFloat("Power"));
        }
        if (compound.contains("Destroyed", Tag.TAG_ANY_NUMERIC)) {
            entityData.set(DESTROYED, compound.getBoolean("Destroyed"));
        }
        if (compound.contains("DestroyedTick", Tag.TAG_ANY_NUMERIC)) {
            destroyedTick = compound.getInt("DestroyedTick");
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
        if (compound.contains(DETACHED_ANCHORS_TAG, Tag.TAG_COMPOUND)) {
            entityData.set(DETACHED_ANCHORS, compound.getCompound(DETACHED_ANCHORS_TAG).copy());
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.getVehicleId());
        buffer.writeResourceLocation(this.getDisplayId());
        buffer.writeInt(destroyedTick);
        buffer.writeNbt(serializePartUnitsData());
        buffer.writeNbt(serializeDecorationUnitsData());
        buffer.writeInt(seats.size());
        for (Seat seat : seats) {
            buffer.writeInt(seat.passengerId);
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.vehicleId = buffer.readResourceLocation();
        this.displayId = buffer.readResourceLocation();
        destroyedTick = buffer.readInt();
        initData();
        deserializePartUnitsData(buffer.readNbt());
        deserializeDecorationUnitsData(buffer.readNbt());
        initDisplayData();
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
            PlayerTeam team = driver.getTeam();
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
            CompoundTag partTag = partUnit.serializeNBT(registryAccess());
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
                    partUnit.deserializeNBT(registryAccess(), partTag);
                }
            });
        }
    }

    private CompoundTag serializeDecorationUnitsData() {
        CompoundTag decorationUnitsTag = new CompoundTag();
        decorationUnits.values().forEach((decorationUnit -> {
            CompoundTag partTag = decorationUnit.serializeNBT(registryAccess());
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
                decorationUnit.deserializeNBT(registryAccess(), partTag);
                decorationUnits.put(id, decorationUnit);
            }
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide()) {
            if (!dataInitialized) {
                initData();
            }
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        VehicleParenting.releaseAll(this, true);
        CarrierDecks.unregister(this);
        carrierRegistered = false;
        VehicleHarness.releaseAll(this, true);
        VehicleHarness.detach(this, true);
        partUnits.forEach((PartUnit::onRemoved));
        if (!level().isClientSide()) {
            for (Entity operator : new ArrayList<>(getDetachedOperators())) {
                operator.stopRiding();
            }
        }
    }

    public void initDisplayData() {
        ClientAssetsManager.INSTANCE.getVehicleDisplay(this.getDisplayId()).ifPresent(this::initDisplayData);
    }

    public void initData() {
        CommonAssetsManager.vehicleDataManager()
                .getVehicleData(this.getVehicleId())
                .ifPresentOrElse(this::initData,
                    () -> {
                        YwzjVehicle.LOGGER.error("No vehicle data found for {}", vehicleId);
                        this.discard();
                    }
                );
    }

    public void initDisplayData(BaseDisplay display) {
        VehicleBedrockModel model = display.getModel();
        if (model != null && model.hasBakedModel()) {
            modelInstance = model.createBakedInstance();
        }
    }

    private void initData(BaseVehicleData vehicleData) {
        if (getHealth() < 0) {
            setMaxHealth(vehicleData.getMaxHealth());
            setHealth(vehicleData.getMaxHealth());
        }
        this.name = vehicleData.getName();
        this.viewInfo = vehicleData.getViewInfo();
        this.energyInfo = vehicleData.getEnergyInfo();
        this.physicsEngine.mass = vehicleData.getPhysicsInfo().mass;
        this.physicsEngine.friction = vehicleData.getPhysicsInfo().friction;
        this.physicsEngine.center = vehicleData.getPhysicsInfo().center;
        this.physicsEngine.canDestroyBlock = vehicleData.getPhysicsInfo().canDestroyBlock;
        this.physicsEngine.canTumble = vehicleData.getPhysicsInfo().canTumble;
        this.physicsEngine.radarCrossSection = vehicleData.getPhysicsInfo().radarCrossSection;
        this.physicsEngine.destroyExplosionVelocity = vehicleData.getPhysicsInfo().destroyExplosionVelocity;
        this.defenseStats = vehicleData.getDefenseStats();
        this.centerOffset = vehicleData.getCenterOffset();
        VehicleStructOBBs vehicleStruct = vehicleData.getVehicleStructObbs();
        this.vehicleCubeOBBs.addAll(vehicleStruct.obbs());
        this.mainCubeOBB = vehicleStruct.mainCubeOBB();
        this.structureLength = vehicleData.getStructureLength();
        BaseVehicleData.PartUnitsAndSeats partUnitsAndSeats = vehicleData.createPartUnits(this);
        this.partUnits.addAll(partUnitsAndSeats.partUnitMap().values());
        this.seats.addAll(partUnitsAndSeats.seats());
        this.uav = vehicleData.isUav();
        if (vehicleData.withWarningReceiver()) {
            this.warningReceiver = new WarningReceiver(this);
        }
        this.protectPassenger = vehicleData.isProtectPassenger();
        Map<String, PartUnit<?>> map = new HashMap<>();
        for (PartUnit<?> partUnit : partUnits) {
            map.put(partUnit.getId(), partUnit);
        }
        this.partUnitMap = map;
        vehicleData.inject(this);
        // Re-index from scratch; initData runs more than once, on the save and spawn data paths,
        // and parts finish attaching cubes here.
        this.allCubeOBBs = null;
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
            PacketDistributor.sendToPlayersTrackingEntity(this, new ServerVehicleChangeDisplay(this.getId(), displayId));
        }
    }

    @Override
    public Component getDisplayName() {
        return name;
    }

    @Override
    public void tick() {
        PhysicsTrace trace = physicsTrace;
        if (trace != null) {
            trace.beginTick(this);
        }
        VehicleParenting.capture(this);
        super.tick();
        deltaMovementO = getDeltaMovement();
        tickPosAndRot();
        if (!level().isClientSide() && !this.isRemoved()) {
            if (tickCount == 1) {
                for (Entity passenger : new ArrayList<>(getPassengers())) {
                    passenger.stopRiding();
                }
                clearDetachedBodyAnchors();
            }
            if (isDestroyed()) {
                destroyedTick += 1;
                if (destroyedTick > 20 * 60) {
                    this.discard();
                }
            }
        }
        boolean async = false;
        boolean harnessed = VehicleHarness.isHarnessed(this);
        if (!this.isRemoved() && !harnessed) {
            if (!level().isClientSide()) {
                async = tryLaunchAsyncPhysics();
            }
            if (!async) {
                aiStep();
            }
        }
        if (level().isClientSide()) {
            tickSound();
            tickParticle();
            if (warningReceiver != null) {
                warningReceiver.tick();
            }
            if (isDestroyed()) {
                destroyedTick += 1;
            }
        } else if (!async && !harnessed && !this.isRemoved()) {
            tickEnergy();
            tickPower();
            tickEngineSpeed();
            this.level().getProfiler().push("vehicle_physics");
            tickPhysics(tickMove());
            this.level().getProfiler().pop();
            postMoveEvent();
        }
        if (!async) {
            finishTick(trace);
        }
    }

    private void finishTick(@Nullable PhysicsTrace trace) {
        if (!level().isClientSide()) {
            VehicleRighting.tick(this);
        }
        tickParts();
        tickDecorations();
        afterVehicleRot();
        this.level().getProfiler().push("vehicle_obb");
        updateOBBs();
        this.level().getProfiler().pop();
        this.level().getProfiler().push("vehicle_riders");
        VehicleParenting.tick(this);
        VehicleHarness.applyChildren(this);
        this.level().getProfiler().pop();
        VehicleHarness.tick(this);
        if (trace != null) {
            trace.endTick(this);
        }
    }

    private void postMoveEvent() {
        VehicleMoveEvent __event = new VehicleMoveEvent(this);
        NeoForge.EVENT_BUS.post(__event);
        if (__event.isCanceled()) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    protected void tickEnergy() {
        float energy = getEnergy();
        energy = Math.max(0, energy - energyInfo.energyConsumptionPerTick * getPower() / 100);
        setEnergy(energy);
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
        physicsRig.capturePose(this);
        physicsRig.bindWorld(ChunkCollisionCache.of(this.level()));
        collisionCursor = physicsRig.cursor;
        solveContacts(physicsRig, force, true);
        flushContacts(physicsRig);
    }


    void solveContacts(PhysicsRig rig, Vec3 force, boolean allowProviders) {
        Vector3f[] axes = rig.axes();
        // 接触方块的采样点
        List<VehicleCubeOBB.CubePoint> touchPoints = new ArrayList<>();

        AABB hullBounds = rig.bounds().inflate(SAMPLE_GATE_MARGIN);
        boolean anySolidNearby;
        if (covers(preparedBounds, hullBounds)) {
            anySolidNearby = tickAnySolidNearby;
        } else if (rig.sections instanceof ChunkCollisionCache liveCache) {
            anySolidNearby = liveCache.prepare(this.level(), hullBounds);
        } else {
            rig.needsLiveWorld = true;
            return;
        }

        List<CollisionProvider.Session> providers =
                allowProviders ? openProviderSessions(hullBounds) : List.of();
        boolean inverted = AllConfigs.Cached.invertedCollisionQuery;
        ChunkCollisionCache.Cursor cursor = rig.cursor;
        carrierLink.collect(mainCubeOBB, rig.hull, axes, touchPoints);
        List<CollisionProvider.Session> gridSessions = providers;
        if (anySolidNearby || !providers.isEmpty() || carrierLink.active()) {
            cursor.reset();
            ContactSynthesis.ContactResolver resolver = ContactSynthesis.blocks(cursor);

            if (inverted) {
                if (anySolidNearby) {
                    scratchBoxes.clear();
                    ChunkCollisionCache.collectBoxes(rig.sections, hullBounds, scratchBoxes);
                    ContactSynthesis.collect(mainCubeOBB, rig.hull, axes, scratchBoxes, resolver, touchPoints);
                }

                gridSessions = List.of();
                for (int i = 0, size = providers.size(); i < size; i++) {
                    CollisionProvider.Session session = providers.get(i);
                    providerBoxes.clear();
                    if (session.collectBoxes(hullBounds, providerBoxes)) {
                        ContactSynthesis.collect(mainCubeOBB, rig.hull, axes, providerBoxes,
                                ContactSynthesis.provider(session), touchPoints);
                    } else {
                        if (gridSessions.isEmpty()) {
                            gridSessions = new ArrayList<>(size - i);
                        }
                        gridSessions.add(session);
                    }
                }
            }

            // Landing gear and wheels attach sample points below the hull, so they must be probed
            // as points; the box passes skip them.
            List<VehicleCubeOBB.CubePoint> attachedPoints = mainCubeOBB.attachedPoints();
            if (inverted && !attachedPoints.isEmpty()) {
                for (int i = 0, size = attachedPoints.size(); i < size; i++) {
                    VehicleCubeOBB.CubePoint point = attachedPoints.get(i);
                    Vector3f worldPos = point.worldPos(rig.hull, axes);
                    if (carrierLink.contactAt(point, worldPos)) {
                        touchPoints.add(point);
                        continue;
                    }
                    if (anySolidNearby && ContactSynthesis.resolveColumn(
                            cursor, point.cubePointContext, worldPos)) {
                        touchPoints.add(point);
                        continue;
                    }
                    for (int p = 0, count = providers.size(); p < count; p++) {
                        CollisionProvider.Contact contact = providers.get(p).contactAt(point, worldPos);
                        if (contact != null) {
                            point.cubePointContext.setProviderCell(contact.blockPos());
                            point.cubePointContext.setBlockState(contact.state());
                            point.cubePointContext.setSurfaceY(Double.NaN);
                            touchPoints.add(point);
                            break;
                        }
                    }
                }
            }

            boolean gridForBlocks = anySolidNearby && !inverted;
            if (gridForBlocks || !gridSessions.isEmpty()) {
                for (VehicleCubeOBB.CubePoint point : mainCubeOBB.cubePoints()) {
                    if (inverted && attachedPoints.contains(point)) {
                        continue;
                    }
                    Vector3f worldPos = point.worldPos(rig.hull, axes);

                    if (gridForBlocks
                            && ContactSynthesis.resolveColumn(
                                    cursor, point.cubePointContext, worldPos)) {
                        touchPoints.add(point);
                        continue;
                    }
                    for (int i = 0, size = gridSessions.size(); i < size; i++) {
                        CollisionProvider.Contact contact = gridSessions.get(i).contactAt(point, worldPos);
                        if (contact != null) {
                            point.cubePointContext.setProviderCell(contact.blockPos());
                            point.cubePointContext.setBlockState(contact.state());
                            // Sample points are reused every tick, so a stale surface height from
                            // an earlier block contact would otherwise survive into this one.
                            point.cubePointContext.setSurfaceY(Double.NaN);
                            touchPoints.add(point);
                            break;
                        }
                    }
                }
            }
            for (int i = 0, size = providers.size(); i < size; i++) {
                providers.get(i).end(touchPoints);
            }
        }
        // Runs on a worker thread when the scheduler is on, so listeners must be thread safe.
        NeoForge.EVENT_BUS.post(new VehicleCollectCollisionEvent(this, touchPoints));

        // 碰撞
        Vec3 velocity = rig.getDeltaMovement();
        if (collision) {
            velocity = physicsEngine.motionByImpact(rig, touchPoints, axes, velocity);
        }
        // 阻力
        velocity = physicsEngine.decelerationByFriction(touchPoints, velocity);
        // 重力与旋转
        velocity = physicsEngine.rotAndFallByGravity(rig, touchPoints, axes, force.toVector3f(), velocity.toVector3f());
        physicsEngine.velocityO = physicsEngine.velocity;

        rig.setDeltaMovement(velocity);
    }

    /**
     * Applies a finished contact solve to the live entity.
     * Must be called from world thread
     */
    void flushContacts(PhysicsRig rig) {
        this.setPos(rig.x, rig.y, rig.z);
        if (rig.getXRot() != this.getXRot()) {
            this.setXRot(rig.getXRot());
        }
        if (rig.getYRot() != this.getYRot()) {
            this.setYRot(rig.getYRot());
        }
        if (rig.getZRot() != this.getZRot()) {
            this.setZRot(rig.getZRot());
        }
        this.setDeltaMovement(rig.getDeltaMovement());
        this.setOnGround(rig.onGround());
        for (int i = 0; i < rig.posRotUpdates; i++) {
            triggerPosRotUpdate();
        }
        if (rig.impactVelocityDiff > 0.5) {
            DamageSystem.impactHurt(rig.impactVelocityDiff, this);
        }

        physicsEngine.applyPendingBreaks();
    }

    /**
     * Freezes this tick's physics inputs and hands the solve to the scheduler, or reports that
     * this vehicle must solve synchronously this tick.
     */
    private boolean tryLaunchAsyncPhysics() {
        if (mainCubeOBB == null || physicsTrace != null || level().isDebug()
                || !CollisionProviders.providers().isEmpty()
                || !VehiclePhysicsScheduler.available()) {
            return false;
        }
        tickEnergy();
        tickPower();
        tickEngineSpeed();
        applyVelocityDeadband();
        physicsRig.capturePose(this);
        physicsRig.bindWorld(pinnedSections);
        planTravel(physicsRig, true);
        collisionCursor = physicsRig.cursor;
        Vec3 force = tickMove();
        physicsRig.capturePose(this);
        physicsEngine.captureState(engineStateSnapshot);
        VehiclePhysicsJob job = new VehiclePhysicsJob(this, force);
        physicsJob = job;
        VehiclePhysicsScheduler.submit(job);
        return true;
    }

    //Async worker startup
    public void runPhysicsSolve(VehiclePhysicsJob job) {
        solveTravel(physicsRig);
        if (!physicsRig.needsLiveWorld) {
            solveContacts(physicsRig, job.force, false);
        }
    }

    public void completePhysicsJob(VehiclePhysicsJob job) {
        if (physicsJob != job) {
            return;
        }
        physicsJob = null;
        if (this.isRemoved()) {
            pendingRecoils.clear();
            return;
        }
        if (job.failed || physicsRig.needsLiveWorld || job.interfered()) {
            physicsEngine.restoreState(engineStateSnapshot);
            physicsRig.capturePose(this);
            physicsRig.bindWorld(ChunkCollisionCache.of(this.level()));
            collisionCursor = physicsRig.cursor;
            planTravel(physicsRig, false);
            solveTravel(physicsRig);
            flushTravel(physicsRig);
            this.pushEntities();
            physicsRig.capturePose(this);
            solveContacts(physicsRig, job.force, true);
            flushContacts(physicsRig);
        } else {
            flushTravel(physicsRig);
            Vec3 beforePush = this.getDeltaMovement();
            this.pushEntities();
            Vec3 afterPush = this.getDeltaMovement();
            if (afterPush.x != beforePush.x || afterPush.y != beforePush.y
                    || afterPush.z != beforePush.z) {
                physicsEngine.restoreState(engineStateSnapshot);
                physicsRig.capturePose(this);
                physicsRig.bindWorld(ChunkCollisionCache.of(this.level()));
                collisionCursor = physicsRig.cursor;
                solveContacts(physicsRig, job.force, true);
            }
            flushContacts(physicsRig);
        }
        postMoveEvent();
        finishTick(null);
        drainPendingRecoils();
    }

    //Drops an in-flight solve without applying it
    public void abandonPhysicsJob(VehiclePhysicsJob job) {
        if (physicsJob != job) {
            return;
        }
        physicsJob = null;
        physicsEngine.restoreState(engineStateSnapshot);
        drainPendingRecoils();
    }

    /**
     * Fires weapon recoil into the physics state, now or at the barrier.
     */
    public void queueRecoil(WeaponUnit weaponUnit, float recoil) {
        if (physicsJob != null) {
            pendingRecoils.add(() -> physicsEngine.recoil(weaponUnit, recoil));
        } else {
            physicsEngine.recoil(weaponUnit, recoil);
        }
    }

    private void drainPendingRecoils() {
        if (pendingRecoils.isEmpty()) {
            return;
        }
        for (int i = 0, size = pendingRecoils.size(); i < size; i++) {
            pendingRecoils.get(i).run();
        }
        pendingRecoils.clear();
    }

    /**
     * The snapshot cursor this tick's contacts were resolved through, or null before the first
     * tick that found anything nearby.
     */
    @Nullable
    public ChunkCollisionCache.Cursor collisionCursor() {
        return collisionCursor;
    }

    /**
     * The substep broadphase, already pointed at this level's snapshot cache. The tick's prepare
     * covers the climb band, so the headroom check can query it instead of walking sections.
     */
    public SweptHull.Broadphase sweptBroadphase() {
        return sweptBroadphase;
    }

    /** Whether the outer box fully contains the inner one. False when nothing was prepared. */
    private static boolean covers(@Nullable AABB outer, AABB inner) {
        return outer != null
                && outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
                && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    /**
     * Opens a session per registered provider that wants this vehicle, so the sampling loop can
     * consult them all in one pass.
     */
    private List<CollisionProvider.Session> openProviderSessions(AABB hullBounds) {
        List<CollisionProvider> registered = CollisionProviders.providers();
        if (registered.isEmpty()) {
            return List.of();
        }
        List<CollisionProvider.Session> sessions = new ArrayList<>(registered.size());
        for (int i = 0, size = registered.size(); i < size; i++) {
            CollisionProvider.Session session = registered.get(i).begin(this, hullBounds);
            if (session != null) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    @Override
    public void move(@NotNull MoverType pType, Vec3 pPos) {
        this.setPos(this.getX() + pPos.x, this.getY() + pPos.y, this.getZ() + pPos.z);
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return super.isInvulnerableTo(source)
                || (!AllConfigs.common.allowMeleeDamageVehicle.get() && source.is(DamageTypes.PLAYER_ATTACK));
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        VehicleAttackEvent __VehicleAttackEvent = new VehicleAttackEvent(this, damageSource, amount);
        NeoForge.EVENT_BUS.post(__VehicleAttackEvent);
        if (__VehicleAttackEvent.isCanceled()) {
            return false;
        }
        if (this.isInvulnerableTo(damageSource)) {
            return false;
        } else {
            if (!level().isClientSide()) {
                // Being shot is the clearest possible sign that a parked vehicle's situation has
                // changed. Wake it before the damage lands, so knockback and a wreck's collapse
                // are solved rather than held in place by the harness.
                VehicleHarness.wake(this);
                this.level().broadcastDamageEvent(this, damageSource);
                DamageSystem.hurt(damageSource, amount, this);
                if (this.getHealth() <= 0) {
                    if (isDestroyed()) {
                        this.discard();
                    } else {
                        this.getPassengers().forEach(Entity::stopRiding);
                        setDestroyed();
                        VehicleExplosion vehicleExplosion = new VehicleExplosion(level(), damageSource.getEntity(), this, this.position(),
                                (float) mainCubeOBB.depth / 2, AllConfigs.common.vehicleExplosionHurtPassengerDamage.get().floatValue(), false, false);
                        vehicleExplosion.explode(Collections.singletonList(this));
                        VehiclePartSpawner.spawnDestroyedParts(this);
                    }
                }
            }
            this.markHurt();
            return true;
        }
    }

    public int passengerCapacity() {
        return seats.size();
    }

    public BakedModelInstance getModelInstance() {
        return modelInstance;
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
        if (isDestroyed()) {
            Level level = level();
            if (!level.isClientSide()) {
                return;
            }
            if (destroyedTick < 20 * 30 && destroyedTick % 5 == 0) {
                ParticleUtil.spawnDestroyedVehicleCloud(level,
                        new Vec3(mainCubeOBB.obb().center()),
                        (float) Math.max(mainCubeOBB.width, mainCubeOBB.depth),
                        mainCubeOBB.height);
            }
            if (tickCount % 20 == 0) {
                ParticleUtil.spawnWreckageSmoke(level, getBoundingBox(), 5);
            }
        }
    }

    protected abstract Vec3 tickMove();

    protected void tickPosAndRot() {
        if (!level().isClientSide()) {
            if (this.xRotO == this.xRot && this.yRotO == this.yRot && !finalRotUpdate) {
                triggerPosRotUpdate();
                finalRotUpdate = true;
            } else {
                finalRotUpdate = false;
            }
        }
        this.xRotO = this.xRot;
        this.yRotO = this.yRot;
        this.zRotO = this.zRot;
        if (level().isClientSide()) {
            if (VehicleHarness.clientFollow(this)) {
                return;
            }
            if (this.lerpSteps > 0) {
                double dX = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
                double dY = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
                double dZ = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
                float dXRot = Mth.wrapDegrees(lerpXRot - this.getXRot());
                float dYRot = Mth.wrapDegrees(lerpYRot - this.getYRot());
                float dZRot = Mth.wrapDegrees(lerpZRot - this.getZRot());
                if (Math.abs(dYRot) > 90 && Math.abs(dZRot) > 90) {
                    this.xRot = Mth.wrapDegrees(180 - this.xRot);
                    this.xRotO = Mth.wrapDegrees(180 - this.xRotO);
                    this.yRot += Math.signum(dYRot) * 180;
                    this.yRotO += Math.signum(dYRot) * 180;
                    this.zRot += Math.signum(dZRot) * 180;
                    this.zRotO += Math.signum(dZRot) * 180;
                    dXRot = Mth.wrapDegrees(lerpXRot - this.getXRot());
                    dYRot = Mth.wrapDegrees(lerpYRot - this.getYRot());
                    dZRot = Mth.wrapDegrees(lerpZRot - this.getZRot());
                }
                this.setXRot(this.getXRot() + dXRot / this.lerpSteps);
                this.setYRot(this.getYRot() + dYRot / this.lerpSteps);
                this.setZRot(this.getZRot() + dZRot / this.lerpSteps);
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
        return activeOBBs;
    }

    /**
     * Rebuilds the collision list when a part detaches or reattaches, so detached
     * geometry stops colliding with the body it came off.
     */
    private void refreshActiveOBBs() {
        boolean changed = false;
        boolean anyDetached = false;
        for (int i = 0; i < cubeDetached.length; i++) {
            int part = cubePartIndex[i];
            boolean detached = part >= 0 && partUnits.get(part).isDetached();
            anyDetached |= detached;
            if (cubeDetached[i] != detached) {
                cubeDetached[i] = detached;
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        if (!anyDetached) {
            activeOBBs = cachedOBBs;
            return;
        }
        List<OBB> obbs = new ArrayList<>(cachedOBBs.size());
        for (VehicleCubeOBB cubeOBB : vehicleCubeOBBs) {
            obbs.add(cubeOBB.obb());
        }
        for (PartUnit<?> partUnit : partUnits) {
            if (!partUnit.isDetached()) {
                obbs.addAll(partUnit.getOBBs());
            }
        }
        activeOBBs = Collections.unmodifiableList(obbs);
    }

    /**
     * This vehicle's frame and walkable geometry as of its last bounding box update. Never null,
     * and a vehicle with nothing walkable reads as empty. Take it once per operation, do not hold
     * it, since it is replaced rather than mutated.
     */
    public DeckSnapshot deckSnapshot() {
        return deckSnapshot;
    }

    /** What a rider's feet find when they land on this hull. */
    public SoundType deckSoundType() {
        return SoundType.METAL;
    }

    /** Entities parented to this vehicle. */
    public List<Entity> deckRiders() {
        return deckRiders;
    }

    @Override
    public void updateOBBs() {
        if (mainCubeOBB == null) {
            return;
        }
        if (allCubeOBBs == null) {
            buildOBBIndex();
        }
        refreshActiveOBBs();
        refreshGroupTransforms();

        Quaternionf vehicleRotation = rotYXZShared();
        int structureCubes = allCubeOBBs.size();
        DeckSnapshot deck = deckSnapshotBuffers[deckSnapshotSlot];
        float[] deckBoxes = deck.boxBuffer(structureCubes);
        float[] landingBoxes = deckCubes > 0 ? deck.deckBoxBuffer(deckCubes) : deck.deckBoxes();
        int landingCount = 0;
        int boxCount = 0;
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0, size = structureCubes; i <= size; i++) {
            VehicleCubeOBB cubeOBB = i == size ? mainCubeOBB : allCubeOBBs.get(i);
            int group = cubeGroupIndex[i];
            cubeOBB.update(this, vehicleRotation, groupOffset[group], groupRotation[group]);

            // A detached part rides its own entity now. Keep posing it so nothing reads a stale
            // pose, but leave it out of the hull's bounds and deck surfaces.
            if (i < size && cubeDetached[i]) {
                continue;
            }

            Vector3f center = cubeOBB.localCenter();
            Vector3f extents = cubeOBB.obb().extents();
            cubeOBB.localRotation().get(scratchLocalRot);
            float rx = Math.abs(scratchLocalRot.m00) * extents.x + Math.abs(scratchLocalRot.m10) * extents.y + Math.abs(scratchLocalRot.m20) * extents.z;
            float ry = Math.abs(scratchLocalRot.m01) * extents.x + Math.abs(scratchLocalRot.m11) * extents.y + Math.abs(scratchLocalRot.m21) * extents.z;
            float rz = Math.abs(scratchLocalRot.m02) * extents.x + Math.abs(scratchLocalRot.m12) * extents.y + Math.abs(scratchLocalRot.m22) * extents.z;
            if (i < size) {
                int o = boxCount * 6;
                deckBoxes[o] = center.x - rx;
                deckBoxes[o + 1] = center.y - ry;
                deckBoxes[o + 2] = center.z - rz;
                deckBoxes[o + 3] = center.x + rx;
                deckBoxes[o + 4] = center.y + ry;
                deckBoxes[o + 5] = center.z + rz;
                if (cubeOBB.isDeck() && landingCount < deckCubes) {
                    System.arraycopy(deckBoxes, o, landingBoxes, landingCount * 6, 6);
                    landingCount++;
                }
                boxCount++;
            }
            if (center.x - rx < minX) minX = center.x - rx;
            if (center.y - ry < minY) minY = center.y - ry;
            if (center.z - rz < minZ) minZ = center.z - rz;
            if (center.x + rx > maxX) maxX = center.x + rx;
            if (center.y + ry > maxY) maxY = center.y + ry;
            if (center.z + rz > maxZ) maxZ = center.z + rz;
        }
        localMinX = minX; localMinY = minY; localMinZ = minZ;
        localMaxX = maxX; localMaxY = maxY; localMaxZ = maxZ;
        localBoundsValid = minX <= maxX;

        setBoundingBox(makeBoundingBox());

        deck.set(vehicleRotation, getYRot(),
                getX() + centerOffset.x, getY() + centerOffset.y, getZ() + centerOffset.z,
                boxCount, landingCount);
        deckSnapshot = deck;
        deckSnapshotSlot ^= 1;
        boolean isCarrier = landingCount > 0;
        if (isCarrier != carrierRegistered) {
            CarrierDecks.setRegistered(this, isCarrier);
            carrierRegistered = isCarrier;
        }
    }

    /** Refreshes every cube's world pose after a pure translation. Used by the substep loop. */
    private void translateOBBs(double dx, double dy, double dz) {
        if (mainCubeOBB == null) {
            return;
        }
        if (allCubeOBBs == null) {
            updateOBBs();
            return;
        }
        float fx = (float) dx;
        float fy = (float) dy;
        float fz = (float) dz;
        for (int i = 0, size = allCubeOBBs.size(); i <= size; i++) {
            VehicleCubeOBB cubeOBB = i == size ? mainCubeOBB : allCubeOBBs.get(i);
            cubeOBB.translate(fx, fy, fz, dx, dy, dz);
        }
    }

    /**
     * Flattens body and part geometry once, and lays out the group forest in topological order.
     */
    private void buildOBBIndex() {
        List<VehicleCubeOBB> cubes = new ArrayList<>(vehicleCubeOBBs);
        for (PartUnit<?> partUnit : partUnits) {
            cubes.addAll(partUnit.getPartCubeOBBs());
        }
        allCubeOBBs = List.copyOf(cubes);
        cubePartIndex = new int[cubes.size()];
        cubeDetached = new boolean[cubes.size()];
        Arrays.fill(cubePartIndex, -1);
        int cubeCursor = vehicleCubeOBBs.size();
        for (int p = 0, parts = partUnits.size(); p < parts; p++) {
            for (int c = partUnits.get(p).getPartCubeOBBs().size(); c > 0; c--) {
                cubePartIndex[cubeCursor++] = p;
            }
        }
        int declaredDeck = 0;
        for (int i = 0, size = cubes.size(); i < size; i++) {
            if (cubes.get(i).isDeck()) {
                declaredDeck++;
            }
        }
        deckCubes = declaredDeck;

        List<OBB> obbs = new ArrayList<>(cubes.size());
        for (VehicleCubeOBB cubeOBB : cubes) {
            obbs.add(cubeOBB.obb());
        }
        cachedOBBs = Collections.unmodifiableList(obbs);
        activeOBBs = cachedOBBs;

        List<VehicleCubeGroup> order = new ArrayList<>();
        Map<VehicleCubeGroup, Integer> indexOf = new IdentityHashMap<>();
        order.add(null);
        for (int i = 0, size = cubes.size(); i <= size; i++) {
            VehicleCubeOBB cubeOBB = i == size ? mainCubeOBB : cubes.get(i);
            indexGroupChain(cubeOBB.group, order, indexOf);
        }

        groupOrder = order.toArray(new VehicleCubeGroup[0]);
        groupParent = new int[groupOrder.length];
        groupOffset = new Vector3f[groupOrder.length];
        groupRotation = new Quaternionf[groupOrder.length];
        for (int i = 0; i < groupOrder.length; i++) {
            VehicleCubeGroup group = groupOrder[i];
            groupParent[i] = group == null || group.parent == null ? -1 : indexOf.get(group.parent);
            groupOffset[i] = new Vector3f();
            groupRotation[i] = new Quaternionf();
        }

        cubeGroupIndex = new int[cubes.size() + 1];
        for (int i = 0, size = cubes.size(); i <= size; i++) {
            VehicleCubeOBB cubeOBB = i == size ? mainCubeOBB : cubes.get(i);
            cubeGroupIndex[i] = cubeOBB.group == null ? 0 : indexOf.get(cubeOBB.group);
        }
    }

    /** Registers a group and every ancestor of it, parents before children. */
    private static void indexGroupChain(VehicleCubeGroup group, List<VehicleCubeGroup> order, Map<VehicleCubeGroup, Integer> indexOf) {
        if (group == null || indexOf.containsKey(group)) {
            return;
        }
        indexGroupChain(group.parent, order, indexOf);
        indexOf.put(group, order.size());
        order.add(group);
    }

    /** Resolves every group's transform relative to the vehicle pivot in one forward sweep. */
    private void refreshGroupTransforms() {
        for (int i = 0; i < groupOrder.length; i++) {
            VehicleCubeGroup group = groupOrder[i];
            if (group == null) {
                continue;
            }
            Vector3f offset = groupOffset[i];
            Quaternionf rotation = groupRotation[i];
            offset.set((float) group.pivot.x, (float) group.pivot.y, (float) group.pivot.z);
            int parent = groupParent[i];
            if (parent < 0) {
                rotation.set(group.rotation);
            } else {
                groupRotation[parent].transform(offset);
                offset.add(groupOffset[parent]);
                groupRotation[parent].mul(group.rotation, rotation);
            }
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        if (remote || !dataInitialized || !localBoundsValid) {
            return AABB.ofSize(position(), 1, 1, 1);
        }
        Quaternionf rotation = rotYXZShared();
        Vector3f corner = new Vector3f();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 8; i++) {
            corner.set(
                    (float) ((i & 1) == 0 ? localMinX : localMaxX),
                    (float) ((i & 2) == 0 ? localMinY : localMaxY),
                    (float) ((i & 4) == 0 ? localMinZ : localMaxZ));
            rotation.transform(corner);
            if (corner.x < minX) minX = corner.x;
            if (corner.y < minY) minY = corner.y;
            if (corner.z < minZ) minZ = corner.z;
            if (corner.x > maxX) maxX = corner.x;
            if (corner.y > maxY) maxY = corner.y;
            if (corner.z > maxZ) maxZ = corner.z;
        }
        double originX = getX() + centerOffset.x;
        double originY = getY() + centerOffset.y;
        double originZ = getZ() + centerOffset.z;
        return new AABB(
                minX + originX, minY + originY, minZ + originZ,
                maxX + originX, maxY + originY, maxZ + originZ);
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
            Vec3 dismountLocation = null;
            PartUnit<?> partUnit = getOwnOperatorUnit(livingEntity);
            if (partUnit != null) {
                dismountLocation = partUnit.worldDismountPosition();
            }
            if (dismountLocation == null) {
                DoorUnit doorUnit = getNearestDoorUnit(livingEntity);
                if (doorUnit != null) {
                    dismountLocation = doorUnit.worldPosition(doorUnit.getPivotOffset()).subtract(0, pPassenger.getEyeHeight() / 2, 0);
                }
            }
            if (dismountLocation == null) {
                dismountLocation = relativeRotPos(position().add(mainCubeOBB.obb().extents().x + 1, 1, partUnit != null ? partUnit.getSeatOffset().z : 0), false);
            }
            dismountLocations.put(livingEntity, dismountLocation);
            super.removePassenger(pPassenger);
            onLeaveVehicle(livingEntity);
        }
    }

    public void onEnterVehicle(LivingEntity livingEntity) {
        if (!level().isClientSide()) {
            if (uav && livingEntity instanceof ServerPlayer serverPlayer && tickCount != 0) {
                setDetachedBodyAnchor(serverPlayer, serverPlayer.position());
            }
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
                PacketDistributor.sendToPlayer(serverPlayer, new ServerVehicleSeatsChange(this));
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
            PacketDistributor.sendToPlayersTrackingEntity(this, new ServerVehicleSeatsChange(this));
        } else {
            onClientLeaveVehicle(pPassenger);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void onClientLeaveVehicle(LivingEntity pPassenger) {
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        if (pPassenger == instance.getPlayer() && instance.toLeave) {
            instance.toSeat(null, this);
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
            PacketDistributor.sendToPlayersTrackingEntity(this, new ServerVehicleSeatsChange(this));
            return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private void setSeats(int[] ids) {
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        Player player = instance.getPlayer();
        List<Integer> passengerIdsBySeat = new ArrayList<>();
        for (int id : ids) {
            passengerIdsBySeat.add(id);
        }
        if (seats.stream().anyMatch(seat -> seat.passengerId == player.getId())
                && !passengerIdsBySeat.contains(player.getId())) {
            instance.toLeave = true;
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
                    instance.toSeat(seat, this);
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
        if (uav) {
            return InteractionResult.PASS;
        }
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
    public Vec3 thirdPersonPosition(float partialTick) {
        Vec3 offset = isViewZoomed() ? getViewInfo().thirdPersonCenterOffsetZoomed : getViewInfo().thirdPersonCenterOffset;
        Matrix3f axisRollMat = new Matrix3f();
        Quaternionf q = new Quaternionf();
        float vehicleYRot = partialTick == 1.0F ? this.getYRot() : this.getViewYRot(partialTick);
        q.rotateY(Math.toRadians(-vehicleYRot));
        q.get(axisRollMat);
        Vector3f rotPos = axisRollMat.transform(offset.toVector3f());
        return position(partialTick).add(new Vec3(rotPos.x, rotPos.y, rotPos.z));
    }

    @OnlyIn(Dist.CLIENT)
    public double thirdPersonDistance(float cameraXRot) {
        double distance = isViewZoomed()
                ? getViewInfo().thirdPersonDistanceZoomed
                : getViewInfo().thirdPersonDistance;
        if (!isViewZoomed()) {
            distance = Math.max(0, distance - cameraXRot / 90 * getViewInfo().thirdPersonCenterOffset.y);
        }
        return distance;
    }

    @NotNull
    @Override
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity pPassenger) {
        Vec3 anchor = getDetachedBodyAnchor(pPassenger);
        if (anchor != null) {
            setDetachedBodyAnchor(pPassenger, null);
            return anchor;
        }
        return dismountLocations.getOrDefault(pPassenger, super.getDismountLocationForPassenger(pPassenger));
    }

    @Override
    public boolean isDetachedBodyActive() {
        return this.uav && !entityData.get(DETACHED_ANCHORS).isEmpty();
    }

    @Nullable
    @Override
    public Vec3 getDetachedBodyAnchor(Entity operator) {
        if (operator == null) {
            return null;
        }
        ListTag anchor = entityData.get(DETACHED_ANCHORS).getList(operator.getStringUUID(), Tag.TAG_DOUBLE);
        if (anchor.size() != 3) {
            return null;
        }
        return new Vec3(anchor.getDouble(0), anchor.getDouble(1), anchor.getDouble(2));
    }

    @Override
    public void setDetachedBodyAnchor(Entity operator, @Nullable Vec3 anchor) {
        if (operator == null || level().isClientSide()) {
            return;
        }
        CompoundTag anchors = entityData.get(DETACHED_ANCHORS).copy();
        if (anchor == null) {
            anchors.remove(operator.getStringUUID());
        } else {
            ListTag list = new ListTag();
            list.add(DoubleTag.valueOf(anchor.x));
            list.add(DoubleTag.valueOf(anchor.y));
            list.add(DoubleTag.valueOf(anchor.z));
            anchors.put(operator.getStringUUID(), list);
        }
        entityData.set(DETACHED_ANCHORS, anchors);
    }

    @Override
    public void clearDetachedBodyAnchors() {
        if (!level().isClientSide()) {
            entityData.set(DETACHED_ANCHORS, new CompoundTag());
        }
    }

    @Override
    public Collection<Entity> getDetachedOperators() {
        CompoundTag anchors = entityData.get(DETACHED_ANCHORS);
        if (anchors.isEmpty()) {
            return List.of();
        }
        List<Entity> operators = new ArrayList<>();
        for (Entity passenger : getPassengers()) {
            if (anchors.contains(passenger.getStringUUID(), Tag.TAG_LIST)) {
                operators.add(passenger);
            }
        }
        return operators;
    }

    @Override
    protected void positionRider(@NotNull Entity pPassenger, Entity.MoveFunction pCallback) {
        Vec3 anchor = getDetachedBodyAnchor(pPassenger);
        if (anchor != null) {
            pCallback.accept(pPassenger, anchor.x, anchor.y, anchor.z);
            return;
        }
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
        playVehicleSound(soundEvent, Vec3.ZERO, 1f, 1f, 1f, 0, false, false, on);
    }

    public void playVehicleSound(SoundEvent soundEvent, Vec3 offset,
                                 float volume, float distance, float pitch,
                                 int fadeTicks, boolean fadeIn, boolean fadeOut, boolean on) {
        PacketDistributor.sendToPlayersTrackingEntity(this,
                new ServerSoundEvent(this.getId(), soundEvent.getLocation().getPath(), offset,
                        volume, distance, pitch,
                        fadeTicks, fadeIn, fadeOut, on));
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

    public HashSet<BulletHoleParticle> getBulletHoleParticles() {
        return bulletHoleParticles;
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

    public Quaternionf rotYXZ(float partialTick) {
        if (partialTick == 1.0F) {
            return rotYXZ();
        }
        Quaternionf previousRotation = new Quaternionf()
                .rotateY(Math.toRadians(-yRotO))
                .rotateX(Math.toRadians(xRotO))
                .rotateZ(Math.toRadians(zRotO));
        return previousRotation.slerp(rotYXZ(), partialTick);
    }

    public Quaternionf rotYXZ() {
        return new Quaternionf(rotYXZShared());
    }

    /** The composed rotation without the defensive copy. Read-only; shared until a rotation field changes. */
    private Quaternionf rotYXZShared() {
        if (yRot != cachedRotYXZYaw || xRot != cachedRotYXZPitch || zRot != cachedRotYXZRoll) {
            cachedRotYXZ.identity()
                    .rotateY(Math.toRadians(-yRot))
                    .rotateX(Math.toRadians(xRot))
                    .rotateZ(Math.toRadians(zRot));
            cachedRotYXZYaw = yRot;
            cachedRotYXZPitch = xRot;
            cachedRotYXZRoll = zRot;
        }
        return cachedRotYXZ;
    }

    public Vec3 position(float partialTick) {
        if (partialTick == 1.0F) {
            return position();
        }
        return new Vec3(Mth.lerp(partialTick, xo, getX()),
                Mth.lerp(partialTick, yo, getY()),
                Mth.lerp(partialTick, zo, getZ()));
    }

    public Vector3f[] axes() {
        Vector3f[] axes = new Vector3f[]{
                new Vector3f(1, 0, 0),
                new Vector3f(0, 1, 0),
                new Vector3f(0, 0, 1)};
        Quaternionf rotation = rotYXZ();
        rotation.transform(axes[0]);
        rotation.transform(axes[1]);
        rotation.transform(axes[2]);
        return axes;
    }

    /**
     * 某世界坐标随载具三轴旋转后或前的新坐标
     */
    public Vec3 relativeRotPos(Vec3 worldPos, boolean reverse) {
        Vec3 center = position().add(centerOffset);
        return relativeRotDirection(worldPos.subtract(center), reverse).add(center);
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
        return new Vec3(axisRollMat.transform(worldDirection.toVector3f()));
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
        entityData.set(ENERGY, amount);
        physicsEngine.mass = curbWeight + amount;
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

    public void setDestroyed() {
        this.entityData.set(DESTROYED, true);
        this.setHealth(this.getMaxHealth());
        this.destroyedTick = 0;
    }

    @Override
    public Vec3 getLightProbePosition(float pPartialTicks) {
        if (mainCubeOBB == null) {
            return position();
        }
        return new Vec3(mainCubeOBB.obb().center());
    }

    @Override
    public PlayerTeam getTeam() {
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

    @Override
    public void push(@NotNull Entity pEntity) {
        if (!collision) {
            return;
        }
        if (this.isPassengerOfSameVehicle(pEntity)) {
            return;
        }
        if (pEntity instanceof AbstractVehicle vehicle) {
            if (carriedBy(this, vehicle) || carriedBy(vehicle, this)) {
                return;
            }
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
            if (this.getId() > vehicle.getId() || !vehicle.isPushable()) {
                return;
            }
            Vec3 separation = new Vec3(vehicle.getX() - this.getX(), 0, vehicle.getZ() - this.getZ());
            if (separation.horizontalDistanceSqr() < 1.0E-4) {
                return;
            }
            Vec3 normal = separation.normalize();
            Vec3 thisVelocity = this.getDeltaMovement();
            Vec3 otherVelocity = vehicle.getDeltaMovement();
            double relativeNormalSpeed = otherVelocity.subtract(thisVelocity).dot(normal);
            double separationSpeed = 0.01;
            if (relativeNormalSpeed < separationSpeed) {
                double thisMass = Math.max(this.physicsEngine.mass, 1.0E-3);
                double otherMass = Math.max(vehicle.physicsEngine.mass, 1.0E-3);
                double velocityChange = separationSpeed - relativeNormalSpeed;
                double totalMass = thisMass + otherMass;
                Vec3 thisPush = normal.scale(-velocityChange * otherMass / totalMass);
                Vec3 otherPush = normal.scale(velocityChange * thisMass / totalMass);
                this.push(thisPush.x, 0, thisPush.z);
                vehicle.push(otherPush.x, 0, otherPush.z);
            }
        }
    }

    /** Whether rider is being held up by carrier's deck right now. */
    private static boolean carriedBy(AbstractVehicle rider, AbstractVehicle carrier) {
        if (VehicleHarness.carrierOf(rider) == carrier) {
            return true;
        }
        CarrierLink link = rider.carrierLink();
        return link.carrier() == carrier && link.supported();
    }

    public void support(Entity pEntity) {
        if (pEntity.noPhysics || this.noPhysics || !collision) {
            return;
        }
        DeckAttachment attachment = VehicleParenting.attachmentOf(pEntity);
        if (attachment != null && attachment.vehicle() == this && attachment.gripped()) {
            return;
        }
        boolean carried = false;
        List<OBB> obbs = getOBBs();
        for (int pass = 0; pass < SUPPORT_RESOLVE_PASSES; pass++) {
            boolean resolvedAny = false;
            for (int i = 0, size = obbs.size(); i < size; i++) {
                OBB obb = obbs.get(i);
                AABB entityAABB = pEntity.getBoundingBox();
                Vector3f cubeCentre = obb.center();
                double offX = (entityAABB.minX + entityAABB.maxX) * 0.5 - cubeCentre.x;
                double offY = (entityAABB.minY + entityAABB.maxY) * 0.5 - cubeCentre.y;
                double offZ = (entityAABB.minZ + entityAABB.maxZ) * 0.5 - cubeCentre.z;
                double entityHalfX = (entityAABB.maxX - entityAABB.minX) * 0.5;
                double entityHalfY = (entityAABB.maxY - entityAABB.minY) * 0.5;
                double entityHalfZ = (entityAABB.maxZ - entityAABB.minZ) * 0.5;
                double reach = obb.extents().length() + Math.sqrt(entityHalfX * entityHalfX
                        + entityHalfY * entityHalfY + entityHalfZ * entityHalfZ);
                if (offX * offX + offY * offY + offZ * offZ > reach * reach) {
                    continue;
                }
                float depth = OBB.mtv(supportFrame.set(obb.rotation()),
                        cubeCentre.x, cubeCentre.y, cubeCentre.z, obb.extents(),
                        entityAABB.minX, entityAABB.minY, entityAABB.minZ,
                        entityAABB.maxX, entityAABB.maxY, entityAABB.maxZ, supportMtv);
                if (depth <= 0) {
                    continue;
                }
                Vec3 mtv = new Vec3(supportMtv.x, supportMtv.y, supportMtv.z);
                if (mtv.y < 0) {
                    Vec3 direction = pEntity.position().subtract(this.position()).normalize();
                    direction = direction.scale(0.2f);
                    mtv = new Vec3(direction.x, 0, direction.z);
                } else if (mtv.y > 0) {
                    Vec3 movement = pEntity.getDeltaMovement();
                    pEntity.setOnGround(true);
                    pEntity.fallDistance = 0;
                    pEntity.setDeltaMovement(movement.x, Math.max(0, movement.y), movement.z);
                    carried = true;
                }

                pEntity.setPos(pEntity.getX() + mtv.x, pEntity.getY() + mtv.y, pEntity.getZ() + mtv.z);
                resolvedAny = true;
            }
            if (!resolvedAny) {
                break;
            }
        }
        if (carried) {
            Vec3 carry = carriedDisplacement(pEntity.position());
            pEntity.setPos(pEntity.getX() + carry.x, pEntity.getY() + carry.y, pEntity.getZ() + carry.z);
        }
    }


    private Vec3 carriedDisplacement(Vec3 worldPos) {
        Vec3 prevPos = mainCubeOBB.positionO;
        Quaternionf prevRot = mainCubeOBB.rotationO;
        Vec3 currPos = mainCubeOBB.position;
        Quaternionf currRot = mainCubeOBB.rotation;
        if (prevPos == null || prevRot == null || currPos == null || currRot == null) {
            return this.getDeltaMovement();
        }
        Vector3f offset = new Vector3f(
                (float) (worldPos.x - prevPos.x),
                (float) (worldPos.y - prevPos.y),
                (float) (worldPos.z - prevPos.z));
        new Quaternionf(prevRot).conjugate().transform(offset);
        currRot.transform(offset);
        return new Vec3(currPos.x + offset.x - worldPos.x,
                currPos.y + offset.y - worldPos.y,
                currPos.z + offset.z - worldPos.z);
    }

    /** Carries the given candidates through one substep. */
    private void supportEntities(List<Entity> candidates) {
        boolean clientSide = this.level().isClientSide();
        for (int i = 0, size = candidates.size(); i < size; i++) {
            Entity entity = candidates.get(i);
            if (entity instanceof AbstractVehicle || entity.isPassengerOfSameVehicle(this)) {
                continue;
            }
            if (clientSide && !(entity instanceof Player)) {
                continue;
            }
            support(entity);
        }
    }


    /** How finely to slice this tick's movement. */
    private int collisionSubsteps(Vec3 movement) {
        Vector3f extents = mainCubeOBB.obb().extents();
        float radius = extents.length();
        double tipDisplacement = movement.length() + Math.abs(physicsEngine.rotV) * radius;
        return Mth.clamp(Mth.ceil(tipDisplacement / SAFE_STEP), 1, MAX_COLLISION_SUBSTEPS);
    }

    private final BoxBuffer turnBoxes = new BoxBuffer();
    private final OBB turnTrialHull = new OBB(new Vector3f(), new Vector3f(), new Quaternionf());
    private final Quaternionf turnSpin = new Quaternionf();

    /**
     * Turns the hull by the given degrees, but only as far as the world allows; rams whatever refuses.
     * @return the rotation actually applied, in degrees
     */
    public float turnBy(float degrees) {
        if (degrees == 0) {
            return 0;
        }
        if (!collision || mainCubeOBB == null) {
            this.setYRot(this.getYRot() + degrees);
            return degrees;
        }
        OBB obb = mainCubeOBB.obb();
        double radius = obb.extents().length();
        double tipSwing = Math.toRadians(Math.abs(degrees)) * radius;
        turnBoxes.clear();
        AABB bounds = getBoundingBox().inflate(Math.max(1.0, tipSwing + 0.5));
        ChunkCollisionCache cache = ChunkCollisionCache.of(this.level());
        boolean deckNear = carrierLink.active();
        if (!deckNear && !covers(preparedBounds, bounds) && !cache.prepare(this.level(), bounds)) {
            this.setYRot(this.getYRot() + degrees);
            return degrees;
        }
        cache.collectBoxes(bounds, turnBoxes);
        if (turnBoxes.isEmpty() && !deckNear) {
            this.setYRot(this.getYRot() + degrees);
            return degrees;
        }

        turnSpin.rotationY((float) Math.toRadians(-degrees));
        turnTrialHull.extents().set(obb.extents());
        turnSpin.mul(obb.rotation(), turnTrialHull.rotation());
        turnTrialHull.center().set(obb.center())
                .sub((float) getX(), (float) getY(), (float) getZ());
        turnSpin.transform(turnTrialHull.center())
                .add((float) getX(), (float) getY(), (float) getZ());
        OBB trial = SweptHull.climbHull(turnTrialHull, sweepSkirt(), sweepHull);
        int hit = SweptHull.firstOverlappingBox(trial, turnBoxes);
        if (hit < 0) {
            if (deckNear && carrierLink.overlaps(trial)) {
                return 0;
            }
            this.setYRot(this.getYRot() + degrees);
            return degrees;
        }
        double tipSpeed = Math.toRadians(Math.abs(degrees)) * radius;
        physicsEngine.ramByRotation(turnBoxes.get(hit), tipSpeed);
        return 0;
    }

    private static boolean horizontal(Vec3 step) {
        return step.x != 0 || step.z != 0;
    }

    /** One sweep cast against world boxes and carrier decks. */
    private double castToi(OBB hull, BoxBuffer near, double moveX, double moveY, double moveZ,
                           @Nullable SweptHull.Probe probe, OBB.SatFrame frame) {
        double toi = SweptHull.timeOfImpact(hull, near, moveX, moveY, moveZ, probe, frame);
        if (carrierLink.active()) {
            toi = Math.min(toi, carrierLink.timeOfImpact(hull, moveX, moveY, moveZ));
        }
        return toi;
    }

    /** Time of impact for climb headroom check. */
    public double climbToi(OBB hull, BoxBuffer near, double lift, OBB.SatFrame frame) {
        return castToi(hull, near, 0, lift, 0, null, frame);
    }

    /** This tick's carrier link. */
    public CarrierLink carrierLink() {
        return carrierLink;
    }

    // ---------------------------------------------------------------- harness state

    @Nullable
    public VehicleHarness harness() {
        return harness;
    }

    public void setHarness(@Nullable VehicleHarness harness) {
        this.harness = harness;
    }

    public int harnessDwell() {
        return harnessDwell;
    }

    public void setHarnessDwell(int ticks) {
        this.harnessDwell = ticks;
    }

    /** Vehicles asleep on this one. */
    public List<AbstractVehicle> harnessedVehicles() {
        return harnessedVehicles;
    }

    /** Entity id of the carrier this vehicle is asleep on, or -1. */
    public int harnessCarrierId() {
        return entityData.get(HARNESS_CARRIER);
    }

    public void setHarnessCarrierId(int id) {
        entityData.set(HARNESS_CARRIER, id);
    }

    private double sweptLegX, sweptLegZ;

    /** Casts horizontal movement as two per-axis legs. */
    private void sweepHorizontal(OBB hull, BoxBuffer near, double moveX, double moveZ,
                                 @Nullable SweptHull.Probe probe) {
        if (Math.abs(moveX) >= Math.abs(moveZ)) {
            sweepLegX(hull, near, moveX, probe);
            sweepLegZ(hull, near, moveZ, null);
        } else {
            sweepLegZ(hull, near, moveZ, probe);
            sweepLegX(hull, near, moveX, null);
        }
    }

    private void sweepLegX(OBB hull, BoxBuffer near, double moveX,
                           @Nullable SweptHull.Probe probe) {
        sweptLegX = 0;
        if (moveX != 0) {
            sweptLegX = moveX * castToi(hull, near, moveX, 0, 0, probe, sweepFrame);
            hull.center().x += (float) sweptLegX;
        }
    }

    private void sweepLegZ(OBB hull, BoxBuffer near, double moveZ,
                           @Nullable SweptHull.Probe probe) {
        sweptLegZ = 0;
        if (moveZ != 0) {
            sweptLegZ = moveZ * castToi(hull, near, 0, 0, moveZ, probe, sweepFrame);
            hull.center().z += (float) sweptLegZ;
        }
    }

    private static final int STEP_UP_ITERATIONS = 4;

    /** Retries a clipped horizontal step with the hull raised, returning the smallest lift that helps. */
    private double stepUp(OBB hull, BoxBuffer near, float baseX, float baseY, float baseZ,
                          double moveX, double moveZ, double baseGain) {
        double stepLen = Math.sqrt(moveX * moveX + moveZ * moveZ);
        double budget = Math.min(maxUpStep(), stepLen * physicsEngine.climbGradient);
        if (budget <= 1.0e-4) {
            return 0;
        }
        hull.center().set(baseX, baseY, baseZ);
        budget *= castToi(hull, near, 0, budget, 0, null, sweepFrame);
        if (budget <= 1.0e-4) {
            return 0;
        }
        hull.center().set(baseX, (float) (baseY + budget), baseZ);
        sweepHorizontal(hull, near, moveX, moveZ, null);
        double raisedGain = Math.abs(sweptLegX) + Math.abs(sweptLegZ);
        if (raisedGain <= baseGain + 1.0e-7) {
            return 0;
        }
        double bestX = sweptLegX;
        double bestZ = sweptLegZ;
        double lo = 0;
        double hi = budget;
        for (int i = 0; i < STEP_UP_ITERATIONS; i++) {
            double mid = (lo + hi) * 0.5;
            hull.center().set(baseX, (float) (baseY + mid), baseZ);
            sweepHorizontal(hull, near, moveX, moveZ, null);
            if (Math.abs(sweptLegX) + Math.abs(sweptLegZ) >= raisedGain - 1.0e-7) {
                hi = mid;
                bestX = sweptLegX;
                bestZ = sweptLegZ;
            } else {
                lo = mid;
            }
        }
        sweptLegX = bestX;
        sweptLegZ = bestZ;
        return hi;
    }

    private static final double MAX_TICK_DISPLACEMENT = MAX_COLLISION_SUBSTEPS * SAFE_STEP;

    private static final int OVERSPEED_LOG_INTERVAL = 100;

    private int overspeedLogTick = -OVERSPEED_LOG_INTERVAL;

    /** Caps this tick's movement at what the substep loop can actually slice safely. */
    private Vec3 clampToSweepBudget(Vec3 movement) {
        double length = movement.length();
        if (length <= MAX_TICK_DISPLACEMENT || length <= 1.0e-9) {
            return movement;
        }
        if (tickCount - overspeedLogTick >= OVERSPEED_LOG_INTERVAL) {
            overspeedLogTick = tickCount;
            YwzjVehicle.LOGGER.warn(
                    "{} #{} asked to move {} blocks in one tick; clamped to {} so collision"
                            + " substeps stay under {} blocks. Velocity is {} — this usually means"
                            + " nothing is cancelling its fall.",
                    getType().toShortString(), getId(), String.format(Locale.ROOT, "%.2f", length),
                    String.format(Locale.ROOT, "%.2f", MAX_TICK_DISPLACEMENT), SAFE_STEP,
                    getDeltaMovement());
        }
        return movement.scale(MAX_TICK_DISPLACEMENT / length);
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
        if (entity instanceof AbstractVehicle vehicle) {
            if (vehicle.hurtTick > 0) {
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

    public void triggerPosRotUpdate() {
        ClientboundMoveEntityPacket.PosRot packet = new ClientboundMoveEntityPacket.PosRot(this.getId(), (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, this.onGround());
        ((ServerLevel) this.level()).getChunkSource().broadcast(this, packet);
    }

    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYaw, float pPitch, int pPosRotationIncrements) {
        super.lerpTo(pX, pY, pZ, pYaw, pPitch, pPosRotationIncrements);
        this.lerpXRot = entityData.get(X_ROT);
        this.lerpYRot = entityData.get(Y_ROT);
        this.lerpZRot = entityData.get(Z_ROT);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    /** Hull-local Y of the underside swept against the world. */
    public double sweepSkirt() {
        return java.lang.Math.min(mainCubeOBB.climbSkirt(),
                -mainCubeOBB.obb().extents().y + maxUpStep() + SWEEP_STEP_MARGIN);
    }

    @Nullable
    public PhysicsTrace physicsTrace() {
        return physicsTrace;
    }

    public void setPhysicsTrace(@Nullable PhysicsTrace physicsTrace) {
        this.physicsTrace = physicsTrace;
    }

    public void aiStep() {
        applyVelocityDeadband();
        this.level().getProfiler().push("travel");
        physicsRig.capturePose(this);
        physicsRig.bindWorld(ChunkCollisionCache.of(this.level()));
        planTravel(physicsRig, false);
        solveTravel(physicsRig);
        flushTravel(physicsRig);
        this.level().getProfiler().pop();

        this.level().getProfiler().push("push");
        {
            this.pushEntities();
        }
        this.level().getProfiler().pop();
    }

    /** Apply velocity deadband before physics. */
    private void applyVelocityDeadband() {
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
    }

    /** Prepare sections and decide how finely to slice movement. */
    private void planTravel(PhysicsRig rig, boolean pin) {
        Vec3 movement = this.getDeltaMovement();
        ChunkCollisionCache cache = ChunkCollisionCache.of(this.level());
        boolean moving = movement.lengthSqr() > 1.0e-8;
        boolean prepare = !this.level().isClientSide() || (collision && moving);
        AABB gather = getBoundingBox().expandTowards(movement)
                .expandTowards(0, maxUpStep(), 0).inflate(SAMPLE_GATE_MARGIN);
        boolean anySolidNear = false;
        if (prepare) {
            anySolidNear = pin
                    ? cache.prepareAndPin(this.level(), gather, pinnedSections)
                    : cache.prepare(this.level(), gather);
            preparedBounds = gather;
        } else {
            preparedBounds = null;
            if (pin) {
                pinnedSections.clear();
            }
        }
        tickAnySolidNearby = anySolidNear;
        boolean deckNear = carrierLink.refresh(this, gather);
        sweptBroadphase.init(rig.sections);
        boolean swept = collision && moving
                && (deckNear || (anySolidNear && ChunkCollisionCache.anyBoxIn(rig.sections,
                        getBoundingBox().expandTowards(movement).inflate(1.0))));
        if (swept) {
            movement = clampToSweepBudget(movement);
            sweepFrame.set(mainCubeOBB.obb().rotation());
        }
        rig.travelMovement = movement;
        rig.swept = swept;
        rig.substeps = swept ? collisionSubsteps(movement) : 1;
        rig.carried = rig.substeps > 1
                ? this.level().getEntities(this, getBoundingBox().expandTowards(movement),
                        EntitySelector.pushableBy(this))
                : List.of();
    }

    /** The substep sweep. Worker-safe; reads prepared snapshots and records each slice. */
    void solveTravel(PhysicsRig rig) {
        boolean swept = rig.swept;
        int substeps = rig.substeps;
        Vec3 stepMovement = substeps > 1 ? rig.travelMovement.scale(1.0 / substeps) : rig.travelMovement;
        PhysicsTrace trace = physicsTrace != null && physicsTrace.isRecording()
                ? physicsTrace : null;
        SweptHull.Probe probe = trace != null ? trace.probe() : null;
        for (int step = 0; step < substeps; step++) {
            Vec3 clipped = stepMovement;
            if (swept) {
                OBB hull = SweptHull.climbHull(rig.hull, sweepSkirt(), sweepHull);
                boolean sideways = horizontal(stepMovement);
                BoxBuffer near = sweptBroadphase.near(hull, stepMovement.x,
                        stepMovement.y + (sideways ? maxUpStep() : 0), stepMovement.z);
                double stepY = stepMovement.y;
                if (stepY != 0) {
                    stepY *= castToi(hull, near, 0, stepY, 0,
                            sideways ? null : probe, sweepFrame);
                }
                double movedX = 0;
                double movedZ = 0;
                double stepUpLift = 0;
                if (sideways) {
                    hull.center().y += (float) stepY;
                    float baseX = hull.center().x;
                    float baseY = hull.center().y;
                    float baseZ = hull.center().z;
                    sweepHorizontal(hull, near, stepMovement.x, stepMovement.z, probe);
                    movedX = sweptLegX;
                    movedZ = sweptLegZ;
                    double got = Math.abs(movedX) + Math.abs(movedZ);
                    double wanted = Math.abs(stepMovement.x) + Math.abs(stepMovement.z);
                    if (got < wanted - 1.0e-7 && rig.onGround()) {
                        stepUpLift = stepUp(hull, near, baseX, baseY, baseZ,
                                stepMovement.x, stepMovement.z, got);
                        if (stepUpLift > 0) {
                            movedX = sweptLegX;
                            movedZ = sweptLegZ;
                            if (trace != null) {
                                trace.add(PhysicsTrace.Source.STEP_UP, stepUpLift);
                            }
                        }
                    }
                }
                clipped = new Vec3(movedX, stepY + stepUpLift, movedZ);
            } else if (probe != null) {
                probe.reset();
            }
            rig.move(clipped.x, clipped.y, clipped.z);
            if (trace != null) {
                if (swept) {
                    OBB ended = SweptHull.climbHull(rig.hull, sweepSkirt(), sweepHull);
                    SweptHull.measurePenetration(ended,
                            sweptBroadphase.near(ended, 0, 0, 0), probe);
                }
                trace.sweep(this, rig, step, substeps, stepMovement);
            }
        }
    }

    /** Apply each recorded slice to the entity. Tick thread only. */
    void flushTravel(PhysicsRig rig) {
        var moves = rig.substepMoves;
        int steps = moves.size() / 3;
        for (int step = 0; step < steps; step++) {
            double dx = moves.getDouble(step * 3);
            double dy = moves.getDouble(step * 3 + 1);
            double dz = moves.getDouble(step * 3 + 2);
            this.move(MoverType.SELF, new Vec3(dx, dy, dz));
            this.translateOBBs(dx, dy, dz);
            if (step < steps - 1) {
                this.supportEntities(rig.carried);
            }
        }
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

    @Deprecated
    @Nullable
    public ResourceLocation getStructureModel() {
        ResourceLocation id = this.getVehicleId();
        return YwzjVehicle.resourceLocation(id.getNamespace() + ":entity/" + id.getPath());
    }

}
