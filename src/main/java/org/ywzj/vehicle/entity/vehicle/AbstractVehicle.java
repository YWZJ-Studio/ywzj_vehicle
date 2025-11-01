package org.ywzj.vehicle.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.joml.Math;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.all.AllVehicles;
import org.ywzj.vehicle.api.entity.OBBEntity;
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.capability.VehicleCapabilityProvider;
import org.ywzj.vehicle.entity.ContainerMob;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.*;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PhysicsEngine;
import org.ywzj.vehicle.vehicle.control.ControlUnit;
import org.ywzj.vehicle.vehicle.parts.IRotatableUnit;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractVehicle extends ContainerMob implements OBBEntity {

    public static final EntityDataAccessor<Float> Z_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> FUEL = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    private final AllVehicles.VehicleType vehicleType;
    public final ControlUnit controlUnit;
    public List<Seat> seats;
    protected final List<PartUnit<?>> partUnits;
    public Vec3 thirdPersonCenterOffset;
    public float thirdPersonDistance;
    public float curbWeight;
    public float fuelCapacity;
    public float fuelConsumptionPerTick;
    private float zRot;
    public float zRotO;
    public int soundDistance;
    public boolean uav;
    protected final LinkedList<ChunkPos> forceChunksQueue;
    protected List<VehicleBedrockCubeOBB> vehicleOBBs;
    protected VehicleBedrockCubeOBB mainCubeOBB;
    public final PhysicsEngine physicsEngine;

    protected AbstractVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.vehicleType = AllVehicles.getVehicleType(this.getClass());
        this.seats = new ArrayList<>();
        this.controlUnit = new ControlUnit();
        this.partUnits = new ArrayList<>();
        this.thirdPersonDistance = 8;
        this.thirdPersonCenterOffset = Vec3.ZERO;
        this.curbWeight = 1;
        this.fuelCapacity = 1;
        this.fuelConsumptionPerTick = 0.00001f;
        this.soundDistance = 3;
        this.forceChunksQueue = new LinkedList<>();
        this.vehicleOBBs = new ArrayList<>();
        this.setMaxUpStep(1.0f);
        this.initData();
        this.physicsEngine = new PhysicsEngine(this, mainCubeOBB);
        this.lookControl = new VehicleLookControl(this);
    }

    public void initData() {
        initPartUnits();
        initOBBs();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(Z_ROT, 0f);
        this.entityData.define(FUEL, 0f);
        this.entityData.define(POWER, 0f);
    }

    @Override
    public void tick() {
        super.tick();
        tickParts();
        updateOBBs();
        if (level().isClientSide()) {
            tickSound();
            tickParticle();
        } else {
            if (tickCount == 1) {
                for (Entity passenger : new ArrayList<>(getPassengers())) {
                    passenger.stopRiding();
                }
            }
            tickFuel();
            tickPower();
            Vec3 force = tickMove();
            tickCollide(force);
            if (uav) {
                forceLoad(position());
                forceLoad(position().add(getLookAngle().normalize().scale(16)));
            }
        }
        getPassengers().forEach(passenger -> passenger.setYBodyRot(getYRot()));
        tickZRot();
    }

    protected void tickFuel() {
        getCapability(VehicleCapabilityProvider.CAPABILITY).ifPresent(cap -> {
            float fuel = cap.getFuel();
            fuel = Math.max(0, fuel - fuelConsumptionPerTick * getPower() / 100);
            physicsEngine.mass = curbWeight + fuel;
            entityData.set(FUEL, fuel);
            setFuel(fuel);
        });
    }

    protected void tickPower() {
        float power = getPower();
        if (getDriver() == null) {
            if (power > 0) {
                setPower(power - 1);
            }
        } else {
            if (power < 100) {
                setPower(power + 1);
            }
        }
        if (getFuel() == 0) {
            setPower(0);
        }
    }

    protected void tickCollide(Vec3 force) {
        Vector3f[] axes = mainCubeOBB.obb().getAxes();
        // 车体大OBB的表面采样点
        List<VehicleBedrockCubeOBB.CubePoint> surfacePoints = mainCubeOBB.cubePoints();
        // 接触方块的采样点
        List<VehicleBedrockCubeOBB.CubePoint> touchPoints = new ArrayList<>();

        for (VehicleBedrockCubeOBB.CubePoint point : surfacePoints) {
            Vector3f worldPos = point.worldPos(axes);
            BlockPos blockPos = BlockPos.containing(new Vec3(worldPos));

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

        setDeltaMovement(velocity);

//        if (this instanceof Ka50) {
//            DebugUtil.particle(level(), ((WeaponUnit)seats.get(0).partUnit).worldBoltPosition());
//            DebugUtil.particle(level(), ((WeaponUnit)seats.get(0).partUnit).worldOwnerViewPosition());
//            DebugUtil.particle(level(), ((WeaponUnit)seats.get(0).partUnit).worldOpticalSightPosition());
//            DebugUtil.particle(level(), seats.get(0).partUnit.worldSeatPosition());
//        }

    }

    @Override
    public void move(MoverType pType, Vec3 pPos) {
        this.setPos(this.getX() + pPos.x, this.getY() + pPos.y, this.getZ() + pPos.z);
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        this.moveRelative(0.02F, pTravelVector);
        this.move(MoverType.SELF, this.getDeltaMovement());
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
        double velocity = this.getDeltaMovement().length() * 20;
        if (velocity > 1) {
            entity.hurt(this.damageSources().magic(), (float) (velocity * velocity));
        }
    }

    public abstract int passengerCapacity();

    public void initPartUnits() {}

    @OnlyIn(Dist.CLIENT)
    protected abstract void tickSound();

    @OnlyIn(Dist.CLIENT)
    protected abstract void tickParticle();

    protected abstract Vec3 tickMove();

    protected void tickZRot() {
        if (level().isClientSide()) {
            this.zRotO = this.zRot;
            this.zRot = this.entityData.get(Z_ROT);
        } else {
            if (this.zRotO != this.zRot) {
                this.entityData.set(Z_ROT, zRot, true);
            }
            this.zRotO = this.zRot;
        }
    }

    protected void tickParts() {
        partUnits.forEach(PartUnit::tick);
    }

    protected void initOBBs() {
        BedrockModel model = BedrockModelLoader.getModel(vehicleType.getStructureBedrockModel());
        BedrockBone bone = model.getBoneMap().get("vehicle_body");
        // 约定取体积最大的块计算物理
        List<BedrockCubePerFace> cubes = new ArrayList<>(bone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
        cubes.sort((cube1, cube2) -> (int) -(cube1.getDepth() * cube1.getWidth() * cube1.getHeight() - cube2.getDepth() * cube2.getWidth() * cube2.getHeight()));
        mainCubeOBB = VehicleBedrockCubeOBB.init(bone, cubes.remove(0));
        vehicleOBBs.add(mainCubeOBB);
        for (BedrockCubePerFace cube : cubes) {
            vehicleOBBs.add(VehicleBedrockCubeOBB.init(bone, cube));
        }
        for (BedrockBone child : bone.getChildren()) {
            List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
            for (BedrockCubePerFace cube : childCubes) {
                vehicleOBBs.add(VehicleBedrockCubeOBB.init(child, cube));
            }
        }
    }

    @Override
    public List<OBB> getOBBs() {
        List<OBB> vehicleOBBs = new ArrayList<>(this.vehicleOBBs.stream().map(VehicleBedrockCubeOBB::obb).toList());
        for (PartUnit<?> partUnit : partUnits) {
            vehicleOBBs.addAll(partUnit.getOBBs());
        }
        return vehicleOBBs;
    }

    @Override
    public void updateOBBs() {
        for (VehicleBedrockCubeOBB vehicleBedrockCubeOBB : vehicleOBBs) {
            OBB obb = vehicleBedrockCubeOBB.obb();
            Vec3 center = vehicleBedrockCubeOBB.center(this);
            Quaternionf rot = vehicleBedrockCubeOBB.selfRot();
            obb.setCenter(relativeRotPos(center).toVector3f());
            obb.setRotation(rotYXZ().mul(rot));
        }
    }

    public AABB getAABB() {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (OBB obb : getOBBs()) {
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

    public void onEnterVehicle(LivingEntity livingEntity) {
        Optional<Seat> emptySeatOptional = seats.stream().filter(seat -> seat.passengerId == -1).findFirst();
        if (emptySeatOptional.isPresent()) {
            Seat seat = emptySeatOptional.get();
            if (seat.seatIndex == 0) {
                controlUnit.setOperator(livingEntity);
            }
            seat.partUnit.setOwner(livingEntity);
            seat.passengerId = livingEntity.getId();
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleSeatsChange(this));
        }
    }

    public void onLeaveVehicle(LivingEntity pPassenger) {
        Optional<Seat> ownSeat = seats.stream().filter(seat -> seat.passengerId == pPassenger.getId()).findFirst();
        if (ownSeat.isPresent()) {
            Seat seat = ownSeat.get();
            if (seat.seatIndex == 0) {
                controlUnit.setOperator(null);
            }
            seat.partUnit.setOwner(null);
            seat.passengerId = -1;
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleSeatsChange(this));
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
            }
            toSeat.partUnit.setOwner(pPassenger);
            toSeat.passengerId = pPassenger.getId();
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleSeatsChange(this));
            return true;
        }
        return false;
    }

    public static void onClientVehicleChangeSeat(ClientVehicleChangeSeat message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ServerPlayer player = ctxSupplier.get().getSender();
        if (player != null && player.level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            vehicle.changeSeat(player, message.toSeat);
        }
    }

    public static void onClientVehicleAction(ClientVehicleAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ServerPlayer player = ctxSupplier.get().getSender();
        if (player != null && player.level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle) {
            player.stopRiding();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void onServerVehicleSeatsChange(ServerVehicleSeatsChange message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            Player player = LocalVehiclePlayer.instance.getPlayer();
            List<Integer> passengerIdsBySeat = new ArrayList<>();
            for (int id : message.passengerIdsBySeat) {
                passengerIdsBySeat.add(id);
            }
            if (vehicle.seats.stream().anyMatch(seat -> seat.passengerId == player.getId())
                    && !passengerIdsBySeat.contains(player.getId())) {
                LocalVehiclePlayer.instance.switchViewType(LocalVehiclePlayer.ViewType.THIRD_PERSON);
            }
            for (int index = 0; index < passengerIdsBySeat.size(); index += 1) {
                Seat seat = vehicle.seats.get(index);
                Entity entity = passengerIdsBySeat.get(index) == null ? null : level.getEntity(passengerIdsBySeat.get(index));
                if (entity instanceof LivingEntity passenger) {
                    if (index == 0) {
                        vehicle.controlUnit.setOperator(passenger);
                    }
                    seat.partUnit.setOwner(passenger);
                    seat.passengerId = entity.getId();
                } else {
                    if (index == 0) {
                        vehicle.controlUnit.setOperator(null);
                    }
                    seat.partUnit.setOwner(null);
                    seat.passengerId = -1;
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void onServerRotatableUnitRot(ServerRotatableUnitRot message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            if (message.partUnitIndex < vehicle.partUnits.size()) {
                PartUnit<?> partUnit = vehicle.partUnits.get(message.partUnitIndex);
                if (partUnit instanceof IRotatableUnit rotatableUnit) {
                    rotatableUnit.setXAimRot(message.xAimRot);
                    rotatableUnit.setYAimRot(message.yAimRot);
                }
            }
        }
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity pPassenger) {
        return seats.stream().anyMatch(seat -> seat.passengerId == -1);
    }

    @NotNull
    @Override
    public InteractionResult mobInteract(@NotNull Player pPlayer, @NotNull InteractionHand pHand) {
        if (!this.level().isClientSide()) {
            if (pHand == InteractionHand.MAIN_HAND) {
                ItemStack itemStack = pPlayer.getItemInHand(pHand);
                if (itemStack.getItem().equals(AllItems.FUEL_TANK.get())) {
                    return InteractionResult.PASS;
                }
                if (pPlayer.startRiding(this)) {
                    onEnterVehicle(pPlayer);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @OnlyIn(Dist.CLIENT)
    public Vec3 thirdPersonPosition(LivingEntity pPassenger) {
        if (pPassenger != null) {
            Matrix3f axisRollMat = new Matrix3f();
            Quaternionf q = new Quaternionf();
            q.rotateY(Math.toRadians(-this.getYRot()));
            q.get(axisRollMat);
            Vector3f rotPos = axisRollMat.transform(thirdPersonCenterOffset.toVector3f());
            Vec3 thirdPersonCenter = this.position().add(new Vec3(rotPos.x, rotPos.y, rotPos.z));
            axisRollMat = new Matrix3f();
            q = new Quaternionf();
            q.rotateY(Math.toRadians(-pPassenger.getYRot()));
            q.rotateX(Math.toRadians(pPassenger.getXRot()));
            q.get(axisRollMat);
            float d = (float) (thirdPersonDistance - pPassenger.getXRot() / 90 * thirdPersonCenterOffset.y);
            Vector3f rotOffset = axisRollMat.transform(new Vector3f(0, 0, -d));
            Vec3 thirdPersonPos = thirdPersonCenter.add(rotOffset.x, rotOffset.y, rotOffset.z);
            Vec3 step = thirdPersonCenter.subtract(thirdPersonPos).normalize().scale(0.1);
            while (level().getBlockState(BlockPos.containing(thirdPersonPos)).isSolid() && thirdPersonPos.distanceTo(thirdPersonCenter) > 1) {
                thirdPersonPos = thirdPersonPos.add(step);
            }
            return thirdPersonPos;
        }
        return Vec3.ZERO;
    }

    @NotNull
    @Override
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity pPassenger) {
        PartUnit<?> partUnit = getOwnOperatorUnit(pPassenger);
        onLeaveVehicle(pPassenger);
        return relativeRotPos(position().add(mainCubeOBB.obb().extents().x + 1, 1, partUnit != null ? partUnit.getSeatOffset().z : 0));
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
        return controlUnit.operator;
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

    public AllVehicles.VehicleType getVehicleType() {
        return vehicleType;
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

    public VehicleBedrockCubeOBB getMainCubeOBB() {
        return mainCubeOBB;
    }

    public float getZRot() {
        return zRot;
    }

    public void setZRot(float rot) {
        zRot = rot;
    }

    public float getViewZRot(float pPartialTicks) {
        return pPartialTicks == 1.0F ? this.getXRot() : Mth.lerp(pPartialTicks, this.zRotO, this.getZRot());
    }

    public Quaternionf rotYXZ() {
        Quaternionf q = new Quaternionf();
        q.rotateY(Math.toRadians(-this.getYRot()))
                .rotateX(Math.toRadians(this.getXRot()))
                .rotateZ(Math.toRadians(this.getZRot()));
        return q;
    }

    /**
     * 某世界坐标随载具三轴旋转后的新坐标
     */
    public Vec3 relativeRotPos(Vec3 worldPos) {
        Vec3 relPos = worldPos.subtract(this.position());
        Matrix3f axisRollMat = new Matrix3f();
        rotYXZ().get(axisRollMat);
        Vector3f rotPos = axisRollMat.transform(new Vector3f((float) relPos.x, (float) relPos.y, (float) relPos.z));
        return this.position().add(new Vec3(rotPos.x, rotPos.y, rotPos.z));
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
        Vector3f d = axisRollMat.transform(new Vector3f((float) worldDirection.x(), (float) worldDirection.y(), (float) worldDirection.z()));
        return new Vec3(d.x, d.y, d.z);
    }

    public abstract void shoot(int weaponIndex, List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot);

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.PLAYER_ATTACK)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
        return AllSounds.BULLET_HIT_OUTSIDE.get();
    }

    public float getFuel() {
        float amount = entityData.get(FUEL);
        if (amount == 0 && AllConfigs.common.infiniteFuel.get()) {
            amount = Float.MIN_VALUE;
        }
        return amount;
    }

    public void setFuel(float amount) {
        getCapability(VehicleCapabilityProvider.CAPABILITY).ifPresent(cap -> {
            cap.setFuel(amount);
            entityData.set(FUEL, amount);
            physicsEngine.mass = curbWeight + amount;
        });
    }

    public float addFuel(float amount) {
        float fuel = getFuel();
        float space = fuelCapacity - fuel;
        if (space > amount) {
            setFuel(fuel + amount);
            return 0;
        } else {
            setFuel(fuelCapacity);
            return amount - space;
        }
    }

    public float getPower() {
        return entityData.get(POWER);
    }

    public void setPower(float power) {
        entityData.set(POWER, Mth.clamp(power, 0, 100));
    }

    public boolean hasPower() {
        return getPower() > 20;
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
        if (!this.isPassengerOfSameVehicle(pEntity)) {
            if (pEntity instanceof AbstractVehicle vehicle) {
                VehicleBedrockCubeOBB bodyCube = vehicle.getMainCubeOBB();
                if (!OBB.isColliding(bodyCube.obb(), this.getMainCubeOBB().obb())) {
                    return;
                }
            } else {
                if (!getMainCubeOBB().obb().contains(pEntity.getEyePosition())) {
                    return;
                }
            }
            impact(pEntity);
            if (!pEntity.noPhysics && !this.noPhysics) {
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
                    if (pEntity.isPushable()) {
                        pEntity.push(d0, 0.0D, d1);
                    }
                }
            }
        }
    }

    public void support(Entity pEntity) {
        if (pEntity.noPhysics || this.noPhysics) {
            return;
        }
        Vec3 feetPosition = pEntity.position().subtract(new Vec3(0, 0.1f, 0));
        Vec3 midPosition = feetPosition.add(0, pEntity.getEyeHeight() / 2, 0);
        Vec3 eyePosition = feetPosition.add(0, pEntity.getEyeHeight(), 0);
        for (OBB obb : getOBBs()) {
            if (obb.contains(feetPosition)) {
                double onVehicleGravity = Math.max(0, pEntity.getDeltaMovement().y);
                if (onVehicleGravity == 0) {
                    pEntity.setOnGround(true);
                }
                double d = obb.embeddingDepth(feetPosition);
                pEntity.setDeltaMovement(this.getDeltaMovement().add(0, onVehicleGravity + d <= 0.2f ? 0 : d, 0));
                pEntity.fallDistance = 0;
                continue;
            }
            if (obb.contains(eyePosition)) {
                double dx = pEntity.getX() - obb.center().x;
                double dz = pEntity.getZ() - obb.center().z;
                double dMax = Mth.absMax(dx, dz);
                if (dMax >= (double) 0.01F) {
                    dMax = Math.sqrt(dMax);
                    dx /= dMax;
                    dz /= dMax;
                    double d = 1.0D / dMax;
                    if (d > 1.0D) {
                        d = 1.0D;
                    }
                    dx *= d;
                    dz *= d;
                    dx *= 0.05F;
                    dz *= 0.05F;
                    if (pEntity.isPushable()) {
                        pEntity.push(dx, 0.0D, dz);
                    }
                    continue;
                }
            }
            AABB aabb = pEntity.getBoundingBox();
            if (OBB.isColliding(obb, aabb)) {
                int face = obb.embeddingFace(midPosition);
                Vector3f[] axes = obb.getAxes();
                Vector3f support = axes[Math.abs(face) - 1];
                if (face < 0) {
                    support.negate();
                }
                if (pEntity.isPushable()) {
                    float force = 0.1f;
                    if (this.getDeltaMovement().length() > 0.01 && Math.abs(face) != 2) {
                        force = 0.2f;
                    }
                    Vec3 move = new Vec3(support).scale(force);
                    move = new Vec3(move.x, Math.max(0, move.y), move.z);
                    pEntity.setPos(pEntity.position().add(move));
                    this.hasImpulse = true;
                }
            }
        }
    }

    @Override
    public void aiStep() {
        if (this.lerpSteps > 0) {
            double dX = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
            double dY = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
            double dZ = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
            double dYRot = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
            this.setYRot(this.getYRot() + (float)dYRot / (float)this.lerpSteps);
            this.setXRot((float) this.lerpXRot);
            --this.lerpSteps;
            this.setPos(dX, dY, dZ);
            this.setRot(this.getYRot(), this.getXRot());
        }

        if (this.lerpHeadSteps > 0) {
            this.yHeadRot += (float)Mth.wrapDegrees(this.lyHeadRot - (double)this.yHeadRot) / (float)this.lerpHeadSteps;
            --this.lerpHeadSteps;
        }

        Vec3 vec31 = this.getDeltaMovement();
        double d1 = vec31.x;
        double d3 = vec31.y;
        double d5 = vec31.z;
        if (Math.abs(vec31.x) < 0.003D) {
            d1 = 0.0D;
        }
        if (Math.abs(vec31.y) < 0.003D) {
            d3 = 0.0D;
        }
        if (Math.abs(vec31.z) < 0.003D) {
            d5 = 0.0D;
        }
        this.setDeltaMovement(d1, d3, d5);

        this.level().getProfiler().push("ai");
        {
            if (this.isImmobile()) {
                this.jumping = false;
                this.xxa = 0.0F;
                this.zza = 0.0F;
            } else if (this.isEffectiveAi()) {
                this.level().getProfiler().push("newAi");
                {
                    this.serverAiStep();
                }
                this.level().getProfiler().pop();
            }
        }
        this.level().getProfiler().pop();

        this.level().getProfiler().push("travel");
        AABB aabb = this.getBoundingBox();
        {
            this.xxa *= 0.98F;
            this.zza *= 0.98F;
            Vec3 vec3 = new Vec3(this.xxa, this.yya, this.zza);
            if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
                this.resetFallDistance();
            }
            this.travel(vec3);
        }
        this.level().getProfiler().pop();

        this.level().getProfiler().push("freezing");
        {
            if (!this.level().isClientSide && !this.isDeadOrDying()) {
                int i = this.getTicksFrozen();
                if (this.isInPowderSnow && this.canFreeze()) {
                    this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), i + 1));
                } else {
                    this.setTicksFrozen(Math.max(0, i - 2));
                }
            }
            this.removeFrost();
            this.tryAddFrost();
            if (!this.level().isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
                this.hurt(this.damageSources().freeze(), 1.0F);
            }
        }
        this.level().getProfiler().pop();

        this.level().getProfiler().push("push");
        {
            if (this.autoSpinAttackTicks > 0) {
                --this.autoSpinAttackTicks;
                this.checkAutoSpinAttack(aabb, this.getBoundingBox());
            }
            this.pushEntities();
        }
        this.level().getProfiler().pop();

        if (!this.level().isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
            this.hurt(this.damageSources().drown(), 1.0F);
        }
    }

    public void forceLoad(Vec3 position) {
        ChunkPos chunkPos = new ChunkPos(BlockPos.containing(position));
        if (!forceChunksQueue.contains(chunkPos)) {
            if (forceChunksQueue.size() >= 4) {
                ChunkPos unloadChunkPos = forceChunksQueue.pop();
                ForgeChunkManager.forceChunk((ServerLevel) level(), YwzjVehicle.MOD_ID, this, unloadChunkPos.x, unloadChunkPos.z, false, true);
            }
            ForgeChunkManager.forceChunk((ServerLevel) level(), YwzjVehicle.MOD_ID, this, chunkPos.x, chunkPos.z, true, true);
            forceChunksQueue.add(chunkPos);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!level().isClientSide) {
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                for (ChunkPos chunkPos : forceChunksQueue) {
                    ForgeChunkManager.forceChunk((ServerLevel) level(), YwzjVehicle.MOD_ID, this, chunkPos.x, chunkPos.z, false, true);
                }
            });
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double v) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isInWall() {
        return false;
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

}

class VehicleLookControl extends LookControl {

    VehicleLookControl(Mob pMob) {
        super(pMob);
    }

    protected boolean resetXRotOnTick() {
        return false;
    }

}
