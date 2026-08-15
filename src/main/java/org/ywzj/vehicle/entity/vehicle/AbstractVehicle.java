package org.ywzj.vehicle.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.util.VehicleExplosion;
import org.ywzj.vehicle.vehicle.DamageSystem;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PhysicsEngine;
import org.ywzj.vehicle.vehicle.PhysicsTrace;
import org.ywzj.vehicle.vehicle.collision.BoxBuffer;
import org.ywzj.vehicle.vehicle.collision.GroundFollower;
import org.ywzj.vehicle.vehicle.collision.MoverSolver;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;
import org.ywzj.vehicle.vehicle.collision.ContactSynthesis;
import org.ywzj.vehicle.vehicle.collision.SweptHull;
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
    private static final String DETACHED_ANCHORS_TAG = "DetachedAnchors";
    private ResourceLocation vehicleId;
    private ResourceLocation displayId;
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
    private ChunkCollisionCache.Cursor collisionCursor;
    private ContactSynthesis.ContactResolver collisionResolver;
    private final BoxBuffer scratchBoxes = new BoxBuffer();
    // Providers hand back real AABBs, so that path keeps a list — reused, not rebuilt per tick.
    private final List<AABB> providerBoxes = new ArrayList<>();
    // Reused across ticks. This used to be allocated fresh every tick along with one AABB per
    // merged box in the swept region, all of which escaped into it and so survived the JIT.
    private final BoxBuffer sweptBoxes = new BoxBuffer();
    // The hull actually swept against the world: the main OBB with its climb skirt trimmed off.
    // Per-vehicle and rewritten every substep, so it must not live on the shared cube groups.
    private final OBB sweepHull = new OBB(new Vector3f(), new Vector3f(), new Quaternionf());
    private final MoverSolver.Workspace moverWork = new MoverSolver.Workspace();
    private final GroundFollower groundFollower = new GroundFollower();
    private final Vector3f moverDelta = new Vector3f();
    private final Vector3f clipScratch = new Vector3f();
    /**
     * Ride height the ground spring holds, in blocks below the hull's underside. Small: this is
     * suspension travel, not clearance, and a vehicle should look like it is sitting on the ground.
     */
    private static final double GROUND_CLEARANCE = 0.05;
    /**
     * How far a kerb — geometry low enough to drive over — may push the hull back horizontally.
     * Enough that a hull cannot bury itself in one, small enough that it does not stop it.
     */
    private static final float RIDE_PUSH_LIMIT = 0.05f;
    // How far to widen the hull bound before asking which sections to prepare. Sample points sit
    // up to 0.2 blocks outside their OBB (the `slack` overshoot in initCubePoints), rotating that
    // local margin into world space costs at most 0.2*sqrt(3), and flooring to a block position
    // can reach one block further. 2.0 covers all of it with room to spare, and since sections
    // are 16 blocks wide the extra margin almost never pulls in another section.
    private static final double SAMPLE_GATE_MARGIN = 2.0;
    // Flattened body + part geometry, indexed once in buildOBBIndex(). Neither vehicleCubeOBBs
    // nor any partUnit's cube list changes after initData(), so rebuilding these every
    // updateOBBs() was pure waste. cubeGroupIndex is parallel to allCubeOBBs with one extra
    // trailing slot for mainCubeOBB, which is a separate copy and not a member of either list.
    private List<VehicleCubeOBB> allCubeOBBs;
    private int[] cubeGroupIndex = new int[0];
    // Group forest in topological order (parents first), plus this vehicle's own copy of each
    // group's transform relative to the vehicle pivot. The transforms must NOT live on the
    // VehicleCubeGroup itself: body and main-structure groups are shared between every instance
    // of a vehicle type (see BaseVehicleData.getVehicleStructObbs), only part groups are cloned.
    private VehicleCubeGroup[] groupOrder = new VehicleCubeGroup[0];
    private int[] groupParent = new int[0];
    private Vector3f[] groupOffset = new Vector3f[0];
    private Quaternionf[] groupRotation = new Quaternionf[0];
    // Bound over all OBBs in vehicle-local space, refreshed in updateOBBs(). makeBoundingBox()
    // rotates its 8 corners rather than re-deriving a hull from every OBB.
    private double localMinX, localMinY, localMinZ, localMaxX, localMaxY, localMaxZ;
    private boolean localBoundsValid;
    private final Matrix3f scratchLocalRot = new Matrix3f();
    // Resolve passes over the OBB set in support(). Overlapping part boxes can each push the
    // entity, so one pass leaves it displaced by their sum; re-testing until nothing overlaps
    // converges instead. Bounded so pathological geometry cannot spin here.
    private static final int SUPPORT_RESOLVE_PASSES = 4;
    // Upper bound on movement substeps, so a fast vehicle cannot multiply per-tick cost without limit
    private static final int MAX_COLLISION_SUBSTEPS = 16;
    /**
     * Largest displacement allowed in one collision step. Half a block, so a slab is the thinnest
     * geometry that can hide between two samples; blocks and walls never can.
     */
    private static final double SAFE_STEP = 0.5;
    /**
     * Clearance added above {@link #maxUpStep()} when deciding how much of the hull to sweep.
     * <p>
     * Without it the sweep hull's underside sits exactly on the tallest climbable step, so a step
     * of precisely that height is a boundary touch and may clip the vehicle to a halt instead of
     * letting {@code climb} lift it. The cost is a sliver of wall height — 1.00 to 1.05 blocks —
     * that no sweep will stop, which is not a height any block combination produces.
     */
    private static final double SWEEP_STEP_MARGIN = 0.05;
    protected double structureLength;
    public WarningReceiver warningReceiver;
    public PhysicsEngine physicsEngine;
    /** Null unless someone asked for a trace; every recording site null-checks this. */
    @Nullable
    private PhysicsTrace physicsTrace;
    private final HashMap<LivingEntity, Vec3> dismountLocations;
    protected boolean driverXYRotControl = false;
    public boolean uav = false;
    public boolean collision = true;
    public boolean remote = false;
    public PlayerTeam remoteTeam;
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
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Engine", isEngineOn());
        compound.putFloat("Energy", getEnergy());
        compound.putFloat("Power", getPower());
        compound.putBoolean("Destroyed", isDestroyed());
        compound.putLong("DestroyedTime", destroyedTime);
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
        if (compound.contains(DETACHED_ANCHORS_TAG, Tag.TAG_COMPOUND)) {
            entityData.set(DETACHED_ANCHORS, compound.getCompound(DETACHED_ANCHORS_TAG).copy());
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
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
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.vehicleId = buffer.readResourceLocation();
        this.displayId = buffer.readResourceLocation();
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
        this.physicsEngine.center = vehicleData.getPhysicsInfo().center;
        this.physicsEngine.canDestroyBlock = vehicleData.getPhysicsInfo().canDestroyBlock;
        this.physicsEngine.radarCrossSection = vehicleData.getPhysicsInfo().radarCrossSection;
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
        // Re-index from scratch: initData can run more than once (save data, then spawn data),
        // and parts only finish attaching their cubes during createPartUnits above.
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
        // Opened here rather than around tickPhysics so the window covers a whole tick: ordinary
        // movement is applied in aiStep, before physics runs, and the ledger only closes if it
        // sees that too.
        PhysicsTrace trace = physicsTrace;
        if (trace != null) {
            trace.beginTick(this);
        }
        super.tick();
        deltaMovementO = getDeltaMovement();
        tickPosAndRot();
        if (!this.isRemoved()) {
            aiStep();
        }
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
                clearDetachedBodyAnchors();
            }
            if (isDestroyed() && System.currentTimeMillis() - destroyedTime > 60000) {
                this.discard();
            }
            tickEnergy();
            tickPower();
            tickEngineSpeed();
            this.level().getProfiler().push("vehicle_physics");
            tickPhysics(tickMove());
            this.level().getProfiler().pop();
            VehicleMoveEvent __event = new VehicleMoveEvent(this);
            NeoForge.EVENT_BUS.post(__event);
            if (__event.isCanceled()) {
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
        tickParts();
        tickDecorations();
        afterVehicleRot();
        this.level().getProfiler().push("vehicle_obb");
        updateOBBs();
        this.level().getProfiler().pop();
        if (trace != null) {
            trace.endTick(this);
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
        Vector3f[] axes = mainCubeOBB.obb().getAxes();
        // 接触方块的采样点
        List<VehicleCubeOBB.CubePoint> touchPoints = new ArrayList<>();

        this.level().getProfiler().push("sample");
        AABB hullBounds = getBoundingBox().inflate(SAMPLE_GATE_MARGIN);
        ChunkCollisionCache collisionCache = ChunkCollisionCache.of(this.level());
        // prepare() returning false is exact, not a guess: every section overlapping the hull was
        // proven to hold nothing with a collision shape, so the query below could only have
        // produced an empty list. Skipping it is what makes an airborne or floating vehicle cost
        // nothing here.
        boolean anySolidNearby = collisionCache.prepare(this.level(), hullBounds);

        List<CollisionProvider.Session> providers = openProviderSessions(hullBounds);
        boolean inverted = AllConfigs.common.invertedCollisionQuery.get();
        // Sessions that could not describe themselves as boxes, and so still need the hull grid.
        List<CollisionProvider.Session> gridSessions = providers;
        if (anySolidNearby || !providers.isEmpty()) {
            if (collisionCursor == null) {
                collisionCursor = collisionCache.cursor();
                collisionResolver = ContactSynthesis.blocks(collisionCursor);
            }
            collisionCursor.reset();

            if (inverted) {
                if (anySolidNearby) {
                    // Gather the merged block boxes near the hull and generate contacts only
                    // where they actually touch it, so cost tracks contact area, not hull area.
                    scratchBoxes.clear();
                    collisionCache.collectBoxes(hullBounds, scratchBoxes);
                    ContactSynthesis.collect(mainCubeOBB, axes, scratchBoxes, collisionResolver, touchPoints);
                }
                // A provider that can describe itself as boxes goes through exactly the same
                // path, so installing Sable or Create no longer drags the whole hull grid back
                // in. One that cannot falls through to the grid below, alone.
                gridSessions = List.of();
                for (int i = 0, size = providers.size(); i < size; i++) {
                    CollisionProvider.Session session = providers.get(i);
                    providerBoxes.clear();
                    if (session.collectBoxes(hullBounds, providerBoxes)) {
                        ContactSynthesis.collect(mainCubeOBB, axes, providerBoxes,
                                ContactSynthesis.provider(session), touchPoints);
                    } else {
                        if (gridSessions.isEmpty()) {
                            gridSessions = new ArrayList<>(size - i);
                        }
                        gridSessions.add(session);
                    }
                }
            }

            // A part can attach sample points below the hull — landing gear legs reach a couple of
            // blocks under the OBB — and those are not on the OBB's surface, so no box pass can
            // produce them. Probe them as points, which is what lets a plane rest on its wheels
            // instead of sinking onto its belly. They are skipped by the grid loop below, which
            // under the inverted query serves only providers that could not describe themselves
            // as boxes.
            List<VehicleCubeOBB.CubePoint> attachedPoints = mainCubeOBB.attachedPoints();
            if (inverted && !attachedPoints.isEmpty()) {
                for (int i = 0, size = attachedPoints.size(); i < size; i++) {
                    VehicleCubeOBB.CubePoint point = attachedPoints.get(i);
                    Vector3f worldPos = point.worldPos(axes);
                    if (anySolidNearby && ContactSynthesis.resolveColumn(
                            collisionCursor, point.cubePointContext, worldPos)) {
                        touchPoints.add(point);
                        continue;
                    }
                    for (int p = 0, count = providers.size(); p < count; p++) {
                        CollisionProvider.Contact contact = providers.get(p).contactAt(point, worldPos);
                        if (contact != null) {
                            point.cubePointContext.setBlockPos(contact.blockPos());
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
                // 车体大OBB的表面采样点. One pass, shared by every provider — each point is
                // transformed into world space exactly once no matter how many are installed.
                for (VehicleCubeOBB.CubePoint point : mainCubeOBB.cubePoints()) {
                    if (inverted && attachedPoints.contains(point)) {
                        continue;
                    }
                    Vector3f worldPos = point.worldPos(axes);

                    // 调试
//                    DebugUtil.particle(level(), new Vec3(worldPos), point.cubeFace());

                    if (gridForBlocks
                            && ContactSynthesis.resolveColumn(
                                    collisionCursor, point.cubePointContext, worldPos)) {
                        touchPoints.add(point);
                        continue;
                    }
                    for (int i = 0, size = gridSessions.size(); i < size; i++) {
                        CollisionProvider.Contact contact = gridSessions.get(i).contactAt(point, worldPos);
                        if (contact != null) {
                            point.cubePointContext.setBlockPos(contact.blockPos());
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
        this.level().getProfiler().pop();
        NeoForge.EVENT_BUS.post(new VehicleCollectCollisionEvent(this, touchPoints));

        // 调试
//        touchPoints.forEach(p -> DebugUtil.particle(level(), new Vec3(p.worldPos(axes)), p.cubeFace()));
//        touchPoints.forEach(p -> {
//            BlockPos blockPos = p.cubePointContext.blockPos();
//            DebugUtil.particle(level(), new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), p.cubeFace());
//        });

        // 碰撞
        Vec3 velocity = getDeltaMovement();
        if (collision) {
            velocity = physicsEngine.motionByImpact(touchPoints, axes, velocity);
        }
        // 阻力
        velocity = physicsEngine.decelerationByFriction(touchPoints, velocity);
        // 重力与旋转
        velocity = physicsEngine.rotAndFallByGravity(touchPoints, axes, force.toVector3f(), velocity.toVector3f());
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
                this.level().broadcastDamageEvent(this, damageSource);
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
        return cachedOBBs;
    }

    @Override
    public void updateOBBs() {
        if (mainCubeOBB == null) {
            return;
        }
        if (allCubeOBBs == null) {
            buildOBBIndex();
        }
        refreshGroupTransforms();

        Quaternionf vehicleRotation = rotYXZ();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0, size = allCubeOBBs.size(); i <= size; i++) {
            VehicleCubeOBB cubeOBB = i == size ? mainCubeOBB : allCubeOBBs.get(i);
            int group = cubeGroupIndex[i];
            cubeOBB.update(this, vehicleRotation, groupOffset[group], groupRotation[group]);

            // Local-space AABB of this cube: project its extents onto the vehicle-local axes.
            Vector3f center = cubeOBB.localCenter();
            Vector3f extents = cubeOBB.obb().extents();
            cubeOBB.localRotation().get(scratchLocalRot);
            float rx = Math.abs(scratchLocalRot.m00) * extents.x + Math.abs(scratchLocalRot.m10) * extents.y + Math.abs(scratchLocalRot.m20) * extents.z;
            float ry = Math.abs(scratchLocalRot.m01) * extents.x + Math.abs(scratchLocalRot.m11) * extents.y + Math.abs(scratchLocalRot.m21) * extents.z;
            float rz = Math.abs(scratchLocalRot.m02) * extents.x + Math.abs(scratchLocalRot.m12) * extents.y + Math.abs(scratchLocalRot.m22) * extents.z;
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

        // Vanilla only refreshes the bound on setPos, so a pure rotation — selfRighting snapping
        // 75 degrees to level, or Sable yawing the hull — used to leave it describing the old
        // orientation until something moved. That now matters more, because the broad-phase gate
        // in tickPhysics decides which sections to look at from this box. Cheap enough to just
        // do since Phase 2 made makeBoundingBox O(1).
        setBoundingBox(makeBoundingBox());
    }

    /**
     * Flattens body and part geometry once, and lays out the group forest in topological
     * order so {@link #refreshGroupTransforms()} can resolve each group from its parent in
     * constant time instead of re-walking the chain per cube.
     */
    private void buildOBBIndex() {
        List<VehicleCubeOBB> cubes = new ArrayList<>(vehicleCubeOBBs);
        for (PartUnit<?> partUnit : partUnits) {
            cubes.addAll(partUnit.getPartCubeOBBs());
        }
        allCubeOBBs = List.copyOf(cubes);

        List<OBB> obbs = new ArrayList<>(cubes.size());
        for (VehicleCubeOBB cubeOBB : cubes) {
            obbs.add(cubeOBB.obb());
        }
        cachedOBBs = Collections.unmodifiableList(obbs);

        // Roots first, then each group is appended after its parent, so a single forward
        // sweep resolves the whole forest. A cube with no group gets the identity slot 0.
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

    /**
     * Registers a group and every ancestor of it, parents before children.
     */
    private static void indexGroupChain(VehicleCubeGroup group, List<VehicleCubeGroup> order, Map<VehicleCubeGroup, Integer> indexOf) {
        if (group == null || indexOf.containsKey(group)) {
            return;
        }
        indexGroupChain(group.parent, order, indexOf);
        indexOf.put(group, order.size());
        order.add(group);
    }

    /**
     * Resolves every group's transform relative to the vehicle pivot in one forward sweep.
     * Equivalent to {@link VehicleCubeGroup#globalTransform()} per group, but O(groups)
     * instead of O(cubes x depth) and without allocating.
     */
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

    /**
     * Vanilla calls this from every {@code setPos}, so it has to stay O(1). Rather than
     * re-deriving a tight hull from the 8 vertices of every OBB, it rotates the 8 corners of
     * the vehicle-local bound cached by {@link #updateOBBs()}.
     * <p>
     * The result is a conservative superset of the old tight hull — identical while the
     * vehicle is level, and looser the further a sparse model (wings, tail) is rotated. Every
     * consumer treats this box as a broad phase and re-tests against the OBBs themselves, so
     * a superset costs a few extra rejected candidates and nothing else.
     */
    @Override
    protected AABB makeBoundingBox() {
        if (remote || !dataInitialized || !localBoundsValid) {
            return AABB.ofSize(position(), 1, 1, 1);
        }
        Quaternionf rotation = rotYXZ();
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
            Vec3 dismountLocation;
            DoorUnit doorUnit = getNearestDoorUnit(livingEntity);
            if (doorUnit != null) {
                dismountLocation = doorUnit.worldPosition(doorUnit.getPivotOffset()).subtract(0, pPassenger.getEyeHeight() / 2, 0);
            } else {
                PartUnit<?> partUnit = getOwnOperatorUnit(livingEntity);
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
        Quaternionf q = new Quaternionf();
        q.rotateY(Math.toRadians(-yRot))
                .rotateX(Math.toRadians(xRot))
                .rotateZ(Math.toRadians(zRot));
        return q;
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

    @Override
    public Vec3 getLightProbePosition(float pPartialTicks) {
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

    public void support(Entity pEntity) {
        if (pEntity.noPhysics || this.noPhysics || !collision) {
            return;
        }
        boolean carried = false;
        for (int pass = 0; pass < SUPPORT_RESOLVE_PASSES; pass++) {
            boolean resolvedAny = false;
            for (OBB obb : getOBBs()) {
                AABB entityAABB = pEntity.getBoundingBox();
                if (!OBB.isColliding(obb, entityAABB)) {
                    continue;
                }
                Vec3 mtv = new Vec3(obb.calculateMTV(entityAABB));
                if (mtv.lengthSqr() <= 0) {
                    continue;
                }
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

    private void supportEntities() {
        boolean clientSide = this.level().isClientSide();
        for (Entity entity : this.level().getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this))) {
            if (entity instanceof AbstractVehicle || entity.isPassengerOfSameVehicle(this)) {
                continue;
            }
            if (clientSide && !(entity instanceof Player)) {
                continue;
            }
            support(entity);
        }
    }


    /**
     * How finely to slice this tick's movement.
     * <p>
     * The divisor used to be the hull's own thinnest dimension, which is the wrong quantity
     * entirely: whether a vehicle steps over a wall depends on how thick the <em>wall</em> is, not
     * on how fat the vehicle is. An 8×3×10 tank got a divisor of 3, so at three blocks per tick it
     * took a single three-block step and passed straight through a one-block wall without ever
     * generating a contact. Slicing against a world feature size instead is what makes the step
     * bounded by something the world can actually be thin enough to hide behind.
     */
    private int collisionSubsteps(Vec3 movement) {
        Vector3f extents = mainCubeOBB.obb().extents();
        float radius = extents.length();
        double tipDisplacement = movement.length() + Math.abs(physicsEngine.rotV) * radius;
        return Mth.clamp(Mth.ceil(tipDisplacement / SAFE_STEP), 1, MAX_COLLISION_SUBSTEPS);
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

    /**
     * Hull-local Y of the underside of the hull that gets swept against the world.
     * <p>
     * Normally the climb skirt, so that geometry the contact stage rides over does not read as a
     * wall to the sweep. But the skirt is derived from sample spacing, not from what the vehicle
     * can climb, and for a three-block-tall hull it works out at about 1.55 blocks against a
     * {@link #maxUpStep()} of 1. Everything in between is a hole: too tall for {@code climb} to
     * lift over, too low for a contact or a sweep to notice, so the vehicle drives through it. A
     * block-and-a-slab wall is exactly 1.5.
     * <p>
     * Capping at the step height closes it. The sweep is a backstop, so being a little more
     * conservative than the contact stage is the safe direction: obstacles it now stops against
     * are precisely the ones {@code climb} would have refused to lift anyway.
     */
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

    /**
     * Moves the hull by redirecting the desired motion against contact planes and following the
     * ground with a spring, rather than truncating it with a scalar time of impact.
     * <p>
     * Replaces the substep-and-clip loop, and with it {@code climb()}, the support lift and the
     * centre kick — see {@link MoverSolver}. Stepping is no longer a mechanism: a step's top face
     * is a supporting plane, the solver pushes the hull onto it, and the spring smooths the result
     * into a ramp. A two-block riser is the same code path and simply refuses to move the hull,
     * because its face is a wall rather than support.
     */
    private void moveByPlanes(Vec3 movement, @Nullable PhysicsTrace trace) {
        OBB hull = mainCubeOBB.obb();
        double rideHeight = maxUpStep() + SWEEP_STEP_MARGIN;

        // Ground spring. Probed across the footprint from the hull's underside, so a vehicle
        // spanning several blocks follows the highest ground beneath any part of it.
        Vector3f extents = hull.extents();
        Vector3f centre = hull.center();
        Matrix3f basis = hull.rotation().get(new Matrix3f());
        // World-space footprint, not the hull's own extents: a vehicle at any yaw covers a
        // different patch of ground than its local dimensions suggest, and probing the local box
        // would miss the ground under a diagonally-parked hull's corners.
        float halfHeight = Math.abs(basis.m01()) * extents.x
                + Math.abs(basis.m11()) * extents.y
                + Math.abs(basis.m21()) * extents.z;
        float halfX = Math.abs(basis.m00()) * extents.x
                + Math.abs(basis.m10()) * extents.y
                + Math.abs(basis.m20()) * extents.z;
        float halfZ = Math.abs(basis.m02()) * extents.x
                + Math.abs(basis.m12()) * extents.y
                + Math.abs(basis.m22()) * extents.z;
        double bottom = centre.y - halfHeight;
        double lift = 0;
        if (!sweptBoxes.isEmpty()) {
            double ground = GroundFollower.probe(sweptBoxes, centre.x, centre.z,
                    halfX, halfZ, bottom + rideHeight);
            float measured = ground == GroundFollower.NO_GROUND
                    ? groundFollower.maxLength
                    : (float) (bottom - ground);
            groundFollower.restLength = (float) GROUND_CLEARANCE;
            groundFollower.maxLength = (float) (GROUND_CLEARANCE + rideHeight);
            lift = groundFollower.step(measured, (float) movement.y, 1.0f);
        } else {
            groundFollower.reset();
        }

        MoverSolver.move(hull, sweptBoxes,
                movement.x, movement.y + lift, movement.z,
                rideHeight, RIDE_PUSH_LIMIT, moverWork, moverDelta);

        this.move(MoverType.SELF, new Vec3(moverDelta.x, moverDelta.y, moverDelta.z));
        this.level().getProfiler().push("vehicle_obb");
        this.updateOBBs();
        this.level().getProfiler().pop();

        // Velocity is clipped, never written, by the position solve. Letting depenetration leave
        // momentum behind is the mechanism behind every launch this codebase has had; the spring
        // deliberately contributes nothing here either.
        Vec3 velocity = this.getDeltaMovement();
        clipScratch.set((float) velocity.x, (float) velocity.y, (float) velocity.z);
        MoverSolver.clipVelocity(moverWork, clipScratch);
        this.setDeltaMovement(clipScratch.x, clipScratch.y, clipScratch.z);

        if (trace != null) {
            trace.add(PhysicsTrace.Source.CLIMB, lift);
            trace.sweep(this, 0, 1, movement);
        }
        this.supportEntities();
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
            Vec3 movement = this.getDeltaMovement();
            int substeps = collisionSubsteps(movement);
            Vec3 stepMovement = substeps > 1 ? movement.scale(1.0 / substeps) : movement;
            // Boxes over the whole swept path, not just where the hull is now — the geometry a
            // fast vehicle is about to hit is by definition not the geometry it currently
            // overlaps, and querying only the latter is why it could pass through anything.
            sweptBoxes.clear();
            if (collision && movement.lengthSqr() > 1.0e-8) {
                AABB swept = getBoundingBox().expandTowards(movement).inflate(1.0);
                ChunkCollisionCache cache = ChunkCollisionCache.of(this.level());
                if (cache.prepare(this.level(), swept)) {
                    cache.collectBoxes(swept, sweptBoxes);
                }
            }
            PhysicsTrace trace = physicsTrace != null && physicsTrace.isRecording()
                    ? physicsTrace : null;
            SweptHull.Probe probe = trace != null ? trace.probe() : null;
            boolean swept = !sweptBoxes.isEmpty();
            if (AllConfigs.common.planeSolverMovement.get()) {
                moveByPlanes(movement, trace);
                this.level().getProfiler().pop();
                this.level().getProfiler().push("push");
                this.pushEntities();
                this.level().getProfiler().pop();
                return;
            }
            for (int step = 0; step < substeps; step++) {
                Vec3 clipped = stepMovement;
                if (swept) {
                    // Never end a step on the far side of something solid. What the velocity does
                    // about that is still the physics engine's call, next tick, with the hull now
                    // resting against the obstacle rather than buried in it.
                    //
                    // Swept above the climb skirt, never on the full hull: below the skirt is the
                    // band the contact stage ignores so steps can be climbed, so a full-hull sweep
                    // collides with the floor the vehicle is driving on.
                    OBB hull = SweptHull.climbHull(mainCubeOBB.obb(), sweepSkirt(), sweepHull);
                    double toi = SweptHull.timeOfImpact(hull, sweptBoxes, stepMovement, probe);
                    if (toi < 1.0) {
                        clipped = stepMovement.scale(toi);
                    }
                } else if (probe != null) {
                    probe.reset();
                }
                this.move(MoverType.SELF, clipped);
                this.level().getProfiler().push("vehicle_obb");
                this.updateOBBs();
                this.level().getProfiler().pop();
                if (trace != null) {
                    // After the OBB refresh, so the overlap measured is the pose the substep
                    // actually ended in rather than the one it started from, and against the same
                    // trimmed hull the sweep used — overlap inside the skirt is by design and
                    // reporting it as penetration buries the real signal.
                    if (swept) {
                        SweptHull.measurePenetration(
                                SweptHull.climbHull(mainCubeOBB.obb(), sweepSkirt(), sweepHull),
                                sweptBoxes, probe);
                    }
                    trace.sweep(this, step, substeps, stepMovement);
                }
                if (step < substeps - 1) {
                    this.supportEntities();
                }
            }
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

    @Deprecated
    @Nullable
    public ResourceLocation getStructureModel() {
        ResourceLocation id = this.getVehicleId();
        return YwzjVehicle.resourceLocation(id.getNamespace() + ":entity/" + id.getPath());
    }

}
