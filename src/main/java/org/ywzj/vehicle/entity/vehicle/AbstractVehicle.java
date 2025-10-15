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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.*;
import org.joml.Math;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.all.AllVehicles;
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.entity.ContainerMob;
import org.ywzj.vehicle.entity.OBBEntity;
import org.ywzj.vehicle.item.FuelTankItem;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.*;
import org.ywzj.vehicle.vehicle.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractVehicle extends ContainerMob implements OBBEntity {

    public static final EntityDataAccessor<Float> Z_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> FUEL = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    private final AllVehicles.VehicleType vehicleType;
    public int seats;
    public List<Integer> passengerIdsBySeat;
    public final ControlUnit controlUnit;
    protected final List<PartUnit> partUnits;
    public final List<PartUnit> operatorUnits;
    public SpotterUnit spotterUnit;
    public Vec3 thirdPersonOffset;
    public float curbWeight;
    public float fuelCapacity;
    public float fuelConsumptionPerTick;
    private float zRot;
    public float zRotO;
    public float lerpZRot;
    public int soundDistance;
    protected List<VehicleBedrockCubeOBB> vehicleBodyOBBs;
    protected VehicleBedrockCubeOBB mainCubeOBB;
    public final PhysicsEngine physicsEngine;

    protected AbstractVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.vehicleType = AllVehicles.getVehicleType(this.getClass());
        this.seats = getSeats();
        this.passengerIdsBySeat = new ArrayList<>(Collections.nCopies(seats, null));
        this.controlUnit = new ControlUnit();
        this.partUnits = new ArrayList<>();
        this.operatorUnits = new ArrayList<>();
        this.spotterUnit = new SpotterUnit(this, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, null);
        this.thirdPersonOffset = Vec3.ZERO;
        this.curbWeight = 1;
        this.fuelCapacity = 1;
        this.fuelConsumptionPerTick = 0.00001f;
        this.soundDistance = 3;
        this.vehicleBodyOBBs = new ArrayList<>();
        this.initData();
        this.physicsEngine = new PhysicsEngine(this, mainCubeOBB);
        this.lookControl = new VehicleLookControl(this);
    }

    public void initData() {
        this.setMaxUpStep(1.0f);
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
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (Z_ROT.equals(pKey)) {
            this.lerpZRot = this.entityData.get(Z_ROT);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.zRotO = this.zRot;
        tickParts();
        updateOBBs();
        if (level().isClientSide()) {
            tickAim();
            tickSound();
            tickParticle();
            spotterUnit.tick();
        } else {
            if (tickCount == 1) {
                for (Entity passenger : new ArrayList<>(getPassengers())) {
                    passenger.stopRiding();
                }
            }
            tickFuel();
            Vec3 force = tickMove();
//            DebugUtil.timer(null);
            tickCollide(force);
//            DebugUtil.timer("物理计算耗时(纳秒)");
        }
        getPassengers().forEach(passenger -> passenger.setYBodyRot(getYRot()));
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
        double velocity = this.getDeltaMovement().length() * 20;
        if (velocity > 1) {
            entity.hurt(this.damageSources().magic(), (float) (velocity * velocity));
        }
    }

    public abstract int getSeats();

    public void initPartUnits() {};

    @OnlyIn(Dist.CLIENT)
    protected abstract void tickAim();

    @OnlyIn(Dist.CLIENT)
    protected abstract void tickSound();

    @OnlyIn(Dist.CLIENT)
    protected abstract void tickParticle();

    protected abstract Vec3 tickMove();

    protected void tickParts() {
        partUnits.forEach(PartUnit::tick);
    }

    protected void initOBBs() {
        BedrockModel model = BedrockModelLoader.getModel(vehicleType.getStructureBedrockModel());
        BedrockBone bone = model.getBoneMap().get("vehicle_body");
        // 约定取体积最大的块计算物理
        List<BedrockCubePerFace> cubes = new ArrayList<>(bone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
        cubes.sort((cube1, cube2) -> (int) (cube1.getDepth() * cube1.getWidth() * cube1.getHeight() - cube2.getDepth() * cube2.getWidth() * cube2.getHeight()));
        mainCubeOBB = VehicleBedrockCubeOBB.init(bone, cubes.remove(0));
        vehicleBodyOBBs.add(mainCubeOBB);
        for (BedrockCubePerFace cube : cubes) {
            vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(bone, cube));
        }
        for (BedrockBone child : bone.getChildren()) {
            List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
            for (BedrockCubePerFace cube : childCubes) {
                vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(child, cube));
            }
        }
    }

    @Override
    public List<OBB> getOBBs() {
        List<OBB> vehicleOBBs = new ArrayList<>(vehicleBodyOBBs.stream().map(VehicleBedrockCubeOBB::obb).toList());
        for (PartUnit partUnit : partUnits) {
            vehicleOBBs.addAll(partUnit.getOBBs());
        }
        return vehicleOBBs;
    }

    @Override
    public void updateOBBs() {
        for (VehicleBedrockCubeOBB vehicleBedrockCubeOBB : vehicleBodyOBBs) {
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
        int seat = passengerIdsBySeat.indexOf(null);
        if (seat == -1 && passengerIdsBySeat.size() < seats) {
            passengerIdsBySeat.add(null);
            seat = passengerIdsBySeat.size() - 1;
        }
        if (seat != -1) {
            if (seat == 0) {
                controlUnit.setOperator(livingEntity);
            }
            if (seat < operatorUnits.size()) {
                operatorUnits.get(seat).setOperator(livingEntity);
            }
            passengerIdsBySeat.set(seat, livingEntity.getId());
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleSeatsChange(this));
        }
    }

    public void onLeaveVehicle(LivingEntity pPassenger) {
        int seat = passengerIdsBySeat.indexOf(pPassenger.getId());
        if (seat != -1) {
            if (seat == 0) {
                controlUnit.setOperator(null);
            }
            if (seat < operatorUnits.size()) {
                operatorUnits.get(seat).setOperator(null);
            }
            passengerIdsBySeat.set(seat, null);
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleSeatsChange(this));
        }
    }

    public boolean changeSeat(LivingEntity pPassenger, int toSeat) {
        if (toSeat < passengerIdsBySeat.size() && passengerIdsBySeat.get(toSeat) == null) {
            int origSeat = passengerIdsBySeat.indexOf(pPassenger.getId());
            if (origSeat == toSeat) {
                return false;
            }
            if (origSeat != -1) {
                if (origSeat == 0) {
                    controlUnit.setOperator(null);
                }
                if (origSeat < operatorUnits.size()) {
                    operatorUnits.get(origSeat).setOperator(null);
                }
                passengerIdsBySeat.set(origSeat, null);
            }
            if (toSeat == 0) {
                controlUnit.setOperator(pPassenger);
            }
            if (toSeat < operatorUnits.size()) {
                operatorUnits.get(toSeat).setOperator(pPassenger);
            }
            passengerIdsBySeat.set(toSeat, pPassenger.getId());
            Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerVehicleSeatsChange(this));
            return true;
        }
        return false;
    }

    public static void onClientVehicleChangeSeat(ClientVehicleChangeSeat message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ServerPlayer player = ctxSupplier.get().getSender();
        if (player.level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            vehicle.changeSeat(player, message.toSeat);
        }
    }

    public static void onClientVehicleAction(ClientVehicleAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ServerPlayer player = ctxSupplier.get().getSender();
        if (player.level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle) {
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
                passengerIdsBySeat.add(id == -1 ? null : id);
            }
            if (vehicle.passengerIdsBySeat.contains(player.getId()) && !passengerIdsBySeat.contains(player.getId())) {
                LocalVehiclePlayer.instance.switchViewType(LocalVehiclePlayer.ViewType.THIRD_PERSON);
            }
            vehicle.passengerIdsBySeat = passengerIdsBySeat;
            for (int index = 0; index < passengerIdsBySeat.size(); index += 1) {
                Entity entity = passengerIdsBySeat.get(index) == null ? null : level.getEntity(passengerIdsBySeat.get(index));
                if (entity instanceof LivingEntity passenger) {
                    if (index == 0) {
                        vehicle.controlUnit.setOperator(passenger);
                    }
                    if (index < vehicle.operatorUnits.size()) {
                        vehicle.operatorUnits.get(index).setOperator(passenger);
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void onServerPartUnitRot(ServerPartUnitRot message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            if (message.partUnitIndex < vehicle.partUnits.size()) {
                PartUnit partUnit = vehicle.partUnits.get(message.partUnitIndex);
                if (partUnit != null) {
                    partUnit.xAimRot = message.xAimRot;
                    partUnit.yAimRot = message.yAimRot;
                }
            }
        }
    }

    @Override
    protected boolean canAddPassenger(Entity pPassenger) {
        return passengerIdsBySeat.stream().filter(Objects::nonNull).count() < seats;
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        if (!this.level().isClientSide()) {
            if (pHand == InteractionHand.MAIN_HAND) {
                ItemStack itemStack = pPlayer.getItemInHand(pHand);
                if (itemStack.getItem().equals(AllItems.FUEL_TANK.get())) {
                    int amount = itemStack.getMaxDamage() - itemStack.getDamageValue();
                    amount = (int) (addFuel((float) amount / 1000) * 1000);
                    ((FuelTankItem) AllItems.FUEL_TANK.get()).remain(itemStack, amount);
                    return InteractionResult.SUCCESS;
                }
                if (!pPlayer.startRiding(this)) {
                    return InteractionResult.PASS;
                }
                onEnterVehicle(pPlayer);
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
            q.rotateY(Math.toRadians(-pPassenger.getYRot()));
            q.rotateX(Math.toRadians(pPassenger.getXRot()));
            q.get(axisRollMat);
            Vector3f rotOffset = axisRollMat.transform(thirdPersonOffset.toVector3f());
            return position().add(rotOffset.x, rotOffset.y, rotOffset.z);
        }
        return position().add(thirdPersonOffset);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pPassenger) {
        onLeaveVehicle(pPassenger);
        return relativeRotPos(position().add(mainCubeOBB.obb().extents().x, 1, 0));
    }

    @Override
    protected void positionRider(Entity pPassenger, Entity.MoveFunction pCallback) {
        if (!(pPassenger instanceof LivingEntity)) {
            super.positionRider(pPassenger, pCallback);
        }
        if (getOwnOperatorUnit((LivingEntity) pPassenger) instanceof WeaponUnit weaponUnit) {
            Vec3 pos = weaponUnit.worldSeatPosition();
            pCallback.accept(pPassenger, pos.x, pos.y, pos.z);
        } else {
            super.positionRider(pPassenger, pCallback);
        }
    }

    public LivingEntity getDriver() {
        return controlUnit.operator;
    }

    public PartUnit getOwnOperatorUnit(LivingEntity pPassenger) {
        if (pPassenger == null) {
            return null;
        }
        int index = passengerIdsBySeat.indexOf(pPassenger.getId());
        if (index != -1) {
            if (index < operatorUnits.size()) {
                return operatorUnits.get(index);
            }
        }
        return pPassenger.level().isClientSide() ? spotterUnit : null;
    }

    public void playVehicleSound(SoundEvent soundEvent, boolean on) {
        Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerSoundEvent(this.getId(), soundEvent.getLocation().getPath(), on));
    }

    public AllVehicles.VehicleType getVehicleType() {
        return vehicleType;
    }

    public VehicleBedrockCubeOBB getMainCubeOBB() {
        return mainCubeOBB;
    }

    public float getZRot() {
        return zRot;
    }

    public void setZRot(float rot) {
        zRot = rot;
        if (!this.level().isClientSide()) {
            this.entityData.set(Z_ROT, rot);
        }
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

    public abstract void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot);

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.PLAYER_ATTACK)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
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
    public void push(Entity pEntity) {
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
        Vec3 feetPosition = pEntity.position().subtract(new Vec3(0, 0.1f, 0));
        for (OBB obb : getOBBs()) {
            if (obb.contains(feetPosition)) {
                if (!pEntity.noPhysics && !this.noPhysics) {
                    double onVehicleGravity = Math.max(0, pEntity.getDeltaMovement().y);
                    if (onVehicleGravity == 0) {
                        pEntity.setOnGround(true);
                    }
                    double d = obb.embeddingDepth(feetPosition);
                    pEntity.setDeltaMovement(this.getDeltaMovement().add(0, onVehicleGravity + d < 0.1f ? 0 : d, 0));
                    continue;
                }
            }
            if (!pEntity.noPhysics && !this.noPhysics) {
                if (OBB.isColliding(obb, pEntity.getBoundingBox())) {
                    double d0 = pEntity.getX() - obb.center().x;
                    double d1 = pEntity.getZ() - obb.center().z;
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
    }

    @Override
    public void aiStep() {
        if (this.lerpSteps > 0) {
            double dX = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
            double dY = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
            double dZ = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
            double dYRot = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
            double dZRot = Mth.wrapDegrees(this.lerpZRot - (double)this.getZRot());
            this.setYRot(this.getYRot() + (float)dYRot / (float)this.lerpSteps);
            this.setZRot((this.getZRot() + (float)dZRot / (float)this.lerpSteps) % 360.0F);
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

}

class VehicleLookControl extends LookControl {

    VehicleLookControl(Mob pMob) {
        super(pMob);
    }

    protected boolean resetXRotOnTick() {
        return false;
    }

}
