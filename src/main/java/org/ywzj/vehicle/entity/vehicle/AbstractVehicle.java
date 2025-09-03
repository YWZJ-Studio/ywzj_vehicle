package org.ywzj.vehicle.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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

import java.util.*;
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
    private float zRot;
    public float zRotO;
    private final List<VehicleBedrockCubeOBB> vehicleBodyOBBs;

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
    }

    @Override
    public void tick() {
        super.tick();
        this.zRotO = this.zRot;
        terrainCompact(wide, length);
        if (level().isClientSide) {
            tickAim();
            tickSound();
            tickParticle();
            spotterUnit.tick();
        } else {
            tickMove();
        }
        tickWeapon();
        updateOBBs();
        getPassengers().forEach(passenger -> passenger.setYBodyRot(getYRot()));
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
        for (BedrockCubePerFace cube : cubes) {
            vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(this, bone, cube));
        }
        for (BedrockBone child : bone.getChildren()) {
            List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
            for (BedrockCubePerFace cube : childCubes) {
                vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(this, child, cube));
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
        if (!this.level().isClientSide) {
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
            } else {
                return pPassenger.level().isClientSide ? spotterUnit : null;
            }
        }
        return null;
    }

    public void playVehicleSound(SoundEvent soundEvent, boolean on) {
        Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), new ServerSoundEvent(this.getId(), soundEvent.getLocation().getPath(), on));
    }

    public AllVehicles.VehicleType getVehicleType() {
        return vehicleType;
    }

    public float getZRot() {
        return zRot;
    }

    public void setZRot(float rot) {
        zRot = rot;
    }

    public Quaternionf rotYXZ() {
        Quaternionf q = new Quaternionf();
        q.rotateY(Math.toRadians(-this.getYRot()))
                .rotateX(Math.toRadians(this.getXRot()))
                .rotateZ(Math.toRadians(this.getZRot()));
        return q;
    }

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

//    @Override
//    public void load(CompoundTag pCompound) {
//        super.load(pCompound);
//        if (this.getDriver() != null) {
//            controlUnit.setOperator(this.getDriver());
//        }
//        for (int index = 0; index < getPassengers().size(); index++) {
//            if (index >= weaponUnits.size()) {
//                break;
//            }
//            if (getPassengers().get(index) instanceof LivingEntity livingEntity) {
//                weaponUnits.get(index).setOperator(livingEntity);
//            }
//        }
//    }

//    public Matrix4f getVehicleYOffsetTransform(float ticks) {
//        Matrix4f transform = new Matrix4f();
//        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + rotateYOffset(), getY() + rotateYOffset()), (float) Mth.lerp(ticks, zo, getZ()));
//        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
//        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(ticks, xRotO, getXRot())));
//        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, zRotO, getZRot())));
//        return transform;
//    }

//    public Matrix4f getVehicleTransform(float ticks) {
//        Matrix4f transformV = getVehicleYOffsetTransform(ticks);
//        Matrix4f transform = new Matrix4f();
//        Vector4f worldPosition = transformPosition(transform, 0, -rotateYOffset(), 0);
//        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
//        return transformV;
//    }

//    public float rotateYOffset() {
//        return 0;
//    }

    public Matrix4f getWheelsTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        return transform;
    }

    public Vector4f transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
    }

    public double traceBlockY(Vec3 pos, double maxLength) {
        var res = this.level().clip(new ClipContext(pos, pos.add(0, -maxLength, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        double targetY;

        BlockState state = level().getBlockState(BlockPos.containing(pos));
        VoxelShape shape = state.getCollisionShape(level(), BlockPos.containing(pos));
        if (!shape.isEmpty()) {
            targetY = pos.y + shape.max(Direction.Axis.Y);
        } else if (res.getType() == HitResult.Type.BLOCK && this.level().noCollision(new AABB(pos, pos))) {
            targetY = res.getLocation().y;
        } else {
            targetY = pos.y - maxLength;
        }

        double diffY = targetY - pos.y;
        return pos.y + 0.5f * diffY;
    }

//    public static double getYRotFromVector(Vec3 vec3) {
//        return Mth.atan2(vec3.x, vec3.z) * (180F / Math.PI);
//    }

    public static double getXRotFromVector(Vec3 vec3) {
        double d0 = vec3.horizontalDistance();
        return Mth.atan2(vec3.y, d0) * (180F / Math.PI);
    }

    // 地形适应测试
    public void terrainCompact(float w, float l) {
        if (onGround()) {
            Matrix4f transform = this.getWheelsTransform(1);

            // 1格间距在底面采样可能的支撑点
            PriorityQueue<Vec3> top4 = new PriorityQueue<>(Comparator.comparingDouble(v -> v.y));
            for (float x = -w / 2; x <= w / 2; x += 1f) {
                for (float y = -l / 2; y <= l / 2; y += 1f) {
                    Vector4f position = transformPosition(transform, x, 0, y);
                    Vec3 p = new Vec3(position.x, position.y, position.z);
                    p = p.add(0, 8, 0);
                    float py = (float) this.traceBlockY(p, 16);
                    p = new Vec3(p.x, py, p.z);
                    // 仅保留前4高
                    top4.offer(p);
                    if (top4.size() > 4) {
                        top4.poll();
                    }
                }
            }
            List<Vec3> supportingBlockPos = new ArrayList<>(top4);

            // 调试用
//            for (Vec3 p : supportingBlockPos) {
//                DebugUtil.particle(this.level(), p);
//            }

            Vec3 positionLF = null, positionRF = null, positionLB = null, positionRB = null;
            // 得到左前、右前、左后、右后
            supportingBlockPos.sort(Comparator.comparingDouble(v -> -v.z));
            List<Vec3> frontPoints = supportingBlockPos.subList(0, 2);
            List<Vec3> backPoints = supportingBlockPos.subList(2, supportingBlockPos.size());
            if (frontPoints.size() == 2) {
                if (frontPoints.get(0).x >= frontPoints.get(1).x) {
                    positionLF = frontPoints.get(0);
                    positionRF = frontPoints.get(1);
                } else {
                    positionLF = frontPoints.get(1);
                    positionRF = frontPoints.get(0);
                }
            }
            if (backPoints.size() == 2) {
                if (backPoints.get(0).x >= backPoints.get(1).x) {
                    positionLB = backPoints.get(0);
                    positionRB = backPoints.get(1);
                } else {
                    positionLB = backPoints.get(1);
                    positionRB = backPoints.get(0);
                }
            }

            // 左后-左前
            Vec3 v0 = positionLB.vectorTo(positionLF);
            // 右后-右前
            Vec3 v1 = positionRB.vectorTo(positionRF);
            // 左前-右前
            Vec3 v2 = positionLF.vectorTo(positionRF);
            // 左后-右后
            Vec3 v3 = positionLB.vectorTo(positionRB);

            double x1 = getXRotFromVector(v0);
            double x2 = getXRotFromVector(v1);
            double z1 = getXRotFromVector(v2);
            double z2 = getXRotFromVector(v3);

            float diffX = org.joml.Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(x1 + x2)) - getXRot()));
            setXRot(Mth.clamp(getXRot() + 0.15f * diffX, -45f, 45f));

            float diffZ = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(z1 + z2)) - zRot));
            setZRot(Mth.clamp(zRot + 0.15f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(zRot * 0.9f);
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

}
