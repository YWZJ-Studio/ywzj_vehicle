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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
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
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.all.AllVehicles;
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.entity.OBBEntity;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleChangeSeat;
import org.ywzj.vehicle.network.message.ServerSoundEvent;
import org.ywzj.vehicle.network.message.ServerVehicleSeatsChange;
import org.ywzj.vehicle.network.message.ServerWeaponUnitRot;
import org.ywzj.vehicle.vehicle.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractVehicle extends Mob implements OBBEntity {

    private final AllVehicles.VehicleType vehicleType;
    public int seats;
    public List<Integer> passengerIdsBySeat;
    public final ControlUnit controlUnit;
    public final List<WeaponUnit> weaponUnits;
    public SpotterUnit spotterUnit;
    public float wide;
    public float length;
    public float height;
    private float zRot;
    public float zRotO;
    public static final EntityDataAccessor<Float> Z_ROT = SynchedEntityData.defineId(AbstractVehicle.class, EntityDataSerializers.FLOAT);
    private final List<VehicleBedrockCubeOBB> vehicleBodyOBBs;
    private final PhysicsEngine physicsEngine = new PhysicsEngine(this);

    protected AbstractVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.vehicleType = AllVehicles.getVehicleType(this.getClass());
        this.seats = getSeats();
        this.passengerIdsBySeat = new ArrayList<>(Collections.nCopies(seats, null));
        this.controlUnit = new ControlUnit();
        this.weaponUnits = new ArrayList<>();
        this.spotterUnit = new SpotterUnit(this, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, null);
        this.vehicleBodyOBBs = new ArrayList<>();
        this.setMaxUpStep(1.0f);
        this.initWeaponUnits();
        this.initOBBs();
        this.lookControl = new VehicleLookControl(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(Z_ROT, 0f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (Z_ROT.equals(pKey)) {
            zRot = this.entityData.get(Z_ROT);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.zRotO = this.zRot;
        tickWeapon();
        updateOBBs();
        if (level().isClientSide()) {
            tickAim();
            tickSound();
            tickParticle();
            spotterUnit.tick();
        } else {
            tickMove();
//            DebugUtil.timer(null);
            tickCollide(getDeltaMovement());
//            DebugUtil.timer("物理计算耗时(纳秒)");
        }
        getPassengers().forEach(passenger -> passenger.setYBodyRot(getYRot()));
    }

    private void tickCollide(Vec3 velocity) {
        VehicleBedrockCubeOBB bodyCube = vehicleBodyOBBs.get(0);
        physicsEngine.physicsCube = bodyCube;
        Vector3f[] axes = bodyCube.obb().getAxes();
        // 车体大OBB的表面采样点
        List<VehicleBedrockCubeOBB.CubePoint> surfacePoints = bodyCube.cubePoints();
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

        // 碰撞
        velocity = physicsEngine.impact(touchPoints, axes, velocity);
        // 旋转
        velocity = physicsEngine.rotAndFallByGravity(touchPoints, new Vector3f(0, 0, 0), axes, velocity);

        setDeltaMovement(velocity);
    }

    @Override
    public void move(MoverType pType, Vec3 pPos) {
        this.setPos(this.getX() + pPos.x, this.getY() + pPos.y, this.getZ() + pPos.z);
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

    public abstract void initWeaponUnits();

    @OnlyIn(Dist.CLIENT)
    protected abstract void tickAim();

    @OnlyIn(Dist.CLIENT)
    protected abstract void tickSound();

    @OnlyIn(Dist.CLIENT)
    protected abstract void tickParticle();

    protected abstract void tickMove();

    protected void tickWeapon() {
        weaponUnits.forEach(WeaponUnit::tick);
    }

    protected void initOBBs() {
        BedrockModel model = BedrockModelLoader.getModel(vehicleType.getStructureBedrockModel());
        BedrockBone bone = model.getBoneMap().get("vehicle_body");
        // 约定取体积最大的块表达车体的长宽高
        List<BedrockCubePerFace> cubes = new ArrayList<>(bone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
        cubes.sort((cube1, cube2) -> (int) (cube1.getDepth() * cube1.getWidth() * cube1.getHeight() - cube2.getDepth() * cube2.getWidth() * cube2.getHeight()));
        this.wide = cubes.get(0).getWidth();
        this.length = cubes.get(0).getDepth();
        this.height = cubes.get(0).getHeight();
        for (BedrockCubePerFace cube : cubes) {
            vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(this, bone, cube));
        }
        for (BedrockBone child : bone.getChildren()) {
            List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
            for (BedrockCubePerFace cube : childCubes) {
                vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(this, child, cube));
            }
        }
        // 由武器单元拓展车体长宽
        for (WeaponUnit weaponUnit : weaponUnits) {
            for (VehicleBedrockCubeOBB yTurnUnitOBB : weaponUnit.getYTurnUnitOBBs()) {
                Vec3 cubeOffset = yTurnUnitOBB.offset();
                this.wide = (float) Math.max((Math.abs(cubeOffset.x) + yTurnUnitOBB.cube().getWidth() / 2) * 2, this.wide);
                this.length = (float) Math.max((Math.abs(cubeOffset.z) + yTurnUnitOBB.cube().getDepth() / 2) * 2, this.length);
                this.height = (float) Math.max(Math.abs(cubeOffset.y) + yTurnUnitOBB.cube().getHeight() / 2, this.height);
            }
            for (VehicleBedrockCubeOBB xTurnUnitOBB : weaponUnit.getXTurnUnitOBBs()) {
                Vec3 cubeOffset = xTurnUnitOBB.offset();
                this.wide = (float) Math.max((Math.abs(cubeOffset.x) + xTurnUnitOBB.cube().getWidth() / 2) * 2, this.wide);
                this.length = (float) Math.max((Math.abs(cubeOffset.z) + xTurnUnitOBB.cube().getDepth() / 2) * 2, this.length);
                this.height = (float) Math.max(Math.abs(cubeOffset.y) + xTurnUnitOBB.cube().getHeight() / 2, this.height);
            }
        }
    }

    @Override
    public List<OBB> getOBBs() {
        List<OBB> vehicleOBBs = new ArrayList<>(vehicleBodyOBBs.stream().map(VehicleBedrockCubeOBB::obb).toList());
        for (WeaponUnit weaponUnit : weaponUnits) {
            vehicleOBBs.addAll(weaponUnit.getOBBs());
        }
        return vehicleOBBs;
    }

    @Override
    public void updateOBBs() {
        for (VehicleBedrockCubeOBB vehicleBedrockCubeOBB : vehicleBodyOBBs) {
            OBB obb = vehicleBedrockCubeOBB.obb();
            Vec3 center = vehicleBedrockCubeOBB.center();
            Quaternionf rot = vehicleBedrockCubeOBB.rot();
            obb.setCenter(relativeRotPos(center).toVector3f());
            obb.setRotation(rotYXZ().mul(rot));
        }
    }

    public AABB getAABB() {
        Vec3 p1 = relativeRotPos(position().add(wide / 2, height / 2, length / 2));
        Vec3 p2 = relativeRotPos(position().add(wide / 2, -height / 2, length / 2));
        Vec3 p3 = relativeRotPos(position().add(wide / 2, height / 2, -length / 2));
        Vec3 p4 = relativeRotPos(position().add(wide / 2, -height / 2, -length / 2));
        Vec3 p5 = relativeRotPos(position().add(-wide / 2, height / 2, length / 2));
        Vec3 p6 = relativeRotPos(position().add(-wide / 2, -height / 2, length / 2));
        Vec3 p7 = relativeRotPos(position().add(-wide / 2, height / 2, -length / 2));
        Vec3 p8 = relativeRotPos(position().add(-wide / 2, -height / 2, -length / 2));

        Vec3[] points = {p1, p2, p3, p4, p5, p6, p7, p8};

        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (Vec3 p : points) {
            double x = p.x();
            double y = p.y();
            double z = p.z();
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        return new AABB(minX, minY + height / 2, minZ, maxX, maxY + height / 2, maxZ);
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
            if (seat < weaponUnits.size()) {
                weaponUnits.get(seat).setOperator(livingEntity);
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
            if (seat < weaponUnits.size()) {
                weaponUnits.get(seat).setOperator(null);
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
                if (origSeat < weaponUnits.size()) {
                    weaponUnits.get(origSeat).setOperator(null);
                }
                passengerIdsBySeat.set(origSeat, null);
            }
            if (toSeat == 0) {
                controlUnit.setOperator(pPassenger);
            }
            if (toSeat < weaponUnits.size()) {
                weaponUnits.get(toSeat).setOperator(pPassenger);
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
                LocalVehiclePlayer.instance.switchViewType(LocalVehiclePlayer.ViewType.DEFAULT);
            }
            vehicle.passengerIdsBySeat = passengerIdsBySeat;
            for (int index = 0; index < passengerIdsBySeat.size(); index += 1) {
                Entity entity = passengerIdsBySeat.get(index) == null ? null : level.getEntity(passengerIdsBySeat.get(index));
                if (entity instanceof LivingEntity passenger) {
                    if (index == 0) {
                        vehicle.controlUnit.setOperator(passenger);
                    }
                    if (index < vehicle.weaponUnits.size()) {
                        vehicle.weaponUnits.get(index).setOperator(passenger);
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void onServerWeaponUnitRot(ServerWeaponUnitRot message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            if (message.weaponIndex < vehicle.weaponUnits.size()) {
                WeaponUnit weaponUnit = vehicle.weaponUnits.get(message.weaponIndex);
                if (weaponUnit != null) {
                    weaponUnit.xAimRot = message.xRot;
                    weaponUnit.yAimRot = message.yRot;
                    weaponUnit.xRot = message.xRot;
                    weaponUnit.yRot = message.yRot;
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
            if (!pPlayer.startRiding(this)) {
                return InteractionResult.PASS;
            }
            onEnterVehicle(pPlayer);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pPassenger) {
        onLeaveVehicle(pPassenger);
        return super.getDismountLocationForPassenger(pPassenger);
    }

    @Override
    protected void positionRider(Entity pPassenger, Entity.MoveFunction pCallback) {
        if (!(pPassenger instanceof LivingEntity)) {
            super.positionRider(pPassenger, pCallback);
        }
        WeaponUnit weaponUnit = getOwnWeaponUnit((LivingEntity) pPassenger);
        if (weaponUnit != null) {
            Vec3 pos = weaponUnit.worldSeatPosition();
            pCallback.accept(pPassenger, pos.x, pos.y, pos.z);
        } else {
            super.positionRider(pPassenger, pCallback);
        }
    }

    public LivingEntity getDriver() {
        return controlUnit.operator;
    }

    public WeaponUnit getOwnWeaponUnit(LivingEntity pPassenger) {
        if (pPassenger == null) {
            return null;
        }
        int index = passengerIdsBySeat.indexOf(pPassenger.getId());
        if (index != -1) {
            if (index < weaponUnits.size()) {
                return weaponUnits.get(index);
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

    public List<VehicleBedrockCubeOBB> getVehicleBodyOBBs() {
        return vehicleBodyOBBs;
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

    public Vec3 relativeRotDirection(Vec3 worldDirection, boolean reverse) {
        Quaternionf q = new Quaternionf();
        q.rotateY(Math.toRadians(this.getYRot()))
                .rotateX(Math.toRadians(this.getXRot()))
                .rotateZ(Math.toRadians(-this.getZRot()));
        Matrix3f axisRollMat = new Matrix3f();
        q.get(axisRollMat);
        if (reverse) {
            axisRollMat = axisRollMat.transpose();
        }
        Vector3f d = axisRollMat.transform(new Vector3f((float) -worldDirection.x(), (float) worldDirection.y(), (float) worldDirection.z()));
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
    public boolean shouldBeSaved() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double v) {
        return false;
    }

    @Override
    public boolean isPushable() {
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
