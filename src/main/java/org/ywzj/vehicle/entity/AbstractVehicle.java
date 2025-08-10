package org.ywzj.vehicle.entity;

import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractVehicle extends Mob {

    public ControlUnit controlUnit = new ControlUnit();
    public List<WeaponUnit> weaponUnits = new ArrayList<>();

    public float roll;
    public float prevRoll;

    protected AbstractVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setMaxUpStep(1.0f);
    }

    public Entity getDriver() {
        return getFirstPassenger();
    }

    public abstract void shoot(int weaponIndex);

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.PLAYER_ATTACK)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    public Vec3 getCameraOffset() {
        return new Vec3(0, 1.5, 0);
    }

    @Override
    public void load(CompoundTag pCompound) {
        super.load(pCompound);
        if (this.getDriver() != null) {
            controlUnit.setOperator(this.getDriver());
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.prevRoll = this.getRoll();
        this.terrainCompact(2.7f, 3.61f);
    }

    public float getRoll() {
        return roll;
    }

    public void setZRot(float rot) {
        roll = rot;
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

    public static double getYRotFromVector(Vec3 vec3) {
        return Mth.atan2(vec3.x, vec3.z) * (180F / Math.PI);
    }

    public static double getXRotFromVector(Vec3 vec3) {
        double d0 = vec3.horizontalDistance();
        return Mth.atan2(vec3.y, d0) * (180F / Math.PI);
    }

    // 地形适应测试
    public void terrainCompact(float w, float l) {
        if (onGround()) {
            Matrix4f transform = this.getWheelsTransform(1);

            // 左前
            Vector4f positionLF = transformPosition(transform, w / 2, 0, l / 2);
            // 右前
            Vector4f positionRF = transformPosition(transform, -w / 2, 0, l / 2);
            // 左后
            Vector4f positionLB = transformPosition(transform, w / 2, 0, -l / 2);
            // 右后
            Vector4f positionRB = transformPosition(transform, -w / 2, 0, -l / 2);

            Vec3 p1 = new Vec3(positionLF.x, positionLF.y, positionLF.z);
            Vec3 p2 = new Vec3(positionRF.x, positionRF.y, positionRF.z);
            Vec3 p3 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
            Vec3 p4 = new Vec3(positionRB.x, positionRB.y, positionRB.z);

//            if (mainSupportingBlockPos.isPresent()) {
//                BlockPos blockpos = this.mainSupportingBlockPos.get();
//            }

            // 确定点位是否在墙里来调整点位高度
            float p1y = (float) this.traceBlockY(p1, 3);
            float p2y = (float) this.traceBlockY(p2, 3);
            float p3y = (float) this.traceBlockY(p3, 3);
            float p4y = (float) this.traceBlockY(p4, 3);

            p1 = new Vec3(positionLF.x, p1y, positionLF.z);
            p2 = new Vec3(positionRF.x, p2y, positionRF.z);
            p3 = new Vec3(positionLB.x, p3y, positionLB.z);
            p4 = new Vec3(positionRB.x, p4y, positionRB.z);

            // 测试用粒子效果，用于确定点位位置

//            List<Entity> entities = getPlayer(level());
//            for (var e : entities) {
//                if (e instanceof ServerPlayer player) {
//                    if (player.level() instanceof ServerLevel serverLevel) {
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p1.x, p1.y, p1.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p2.x, p2.y, p2.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p3.x, p3.y, p3.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p4.x, p4.y, p4.z, 1, 0, 0, 0, 0, true);
//                    }
//                }
//            }

            // 通过点位位置获取角度

            // 左后-左前
            Vec3 v0 = p3.vectorTo(p1);
            // 右后-右前
            Vec3 v1 = p4.vectorTo(p2);
            // 左前-右前
            Vec3 v2 = p1.vectorTo(p2);
            // 左后-右后
            Vec3 v3 = p3.vectorTo(p4);

            double x1 = getXRotFromVector(v0);
            double x2 = getXRotFromVector(v1);
            double z1 = getXRotFromVector(v2);
            double z2 = getXRotFromVector(v3);

            float diffX = org.joml.Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(x1 + x2)) - getXRot()));
            setXRot(Mth.clamp(getXRot() + 0.15f * diffX, -45f, 45f));

            float diffZ = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(z1 + z2)) - getRoll()));
            setZRot(Mth.clamp(getRoll() + 0.15f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(getRoll() * 0.9f);
        }
    }

    public static class ControlUnit {

        public Entity operator;
        public boolean forward;
        public boolean backward;
        public boolean left;
        public boolean right;

        public void setOperator(Entity operator) {
            this.operator = operator;
        }

        public void reset() {
            forward = false;
            backward = false;
            left = false;
            right = false;
        }

    }

}
