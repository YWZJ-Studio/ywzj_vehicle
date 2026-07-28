package org.ywzj.vehicle.vehicle.part;

import com.mojang.math.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.custom.part.data.RotatableUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;

import java.util.Map;

/**
 * 可旋转运动的载具部件
 */
public class RotatableUnit<T extends RotatableUnitData> extends PartUnit<T> {

    protected float xAimRot;
    protected float yAimRot;
    protected float xRot;
    protected float yRot;
    public float xRotO;
    public float yRotO;
    protected float xRemoteAimRot;
    protected float yRemoteAimRot;

    public float xSelfRot;
    public float ySelfRot;
    public float xRotSpeed;
    public float yRotSpeed;
    public float xRotMax = 90;
    public float xRotMin = -90;
    public float yRotMax = Float.MAX_VALUE;
    public float yRotMin = -Float.MAX_VALUE;
    public boolean needPower = true;

    private VehicleSound turnYSoundInstance;
    private VehicleSound turnXSoundInstance;

    public RotatableUnit(int index, AbstractVehicle vehicle, T data) {
        super(index, vehicle, data);

        var rotInfo = data.getRotInfo();
        this.xRotSpeed = rotInfo.xRotSpeed;
        this.yRotSpeed = rotInfo.yRotSpeed;
        this.xRotMax = rotInfo.xRotMax;
        this.xRotMin = rotInfo.xRotMin;
        this.yRotMax = rotInfo.yRotMax;
        this.yRotMin = rotInfo.yRotMin;
        this.xRot = rotInfo.xRot;
        this.xAimRot = this.xRot;
        this.yRot = rotInfo.yRot;
        this.yAimRot = this.yRot;
        this.needPower = rotInfo.needPower;

        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setXRemoteAimRot, this::getXAimRot, 0f);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setYRemoteAimRot, this::getYAimRot, 0f);
    }

    public void buildStructure(Map<VehicleCubeGroup, VehicleCubeGroup> vehicleCubeGroupCopy) {
        super.buildStructure(vehicleCubeGroupCopy);
        if (structureGroup != null) {
            Vector3f selfRot = new Vector3f();
            structureGroup.baseRotation.getEulerAnglesYXZ(selfRot);
            this.xSelfRot = (float) Math.toDegrees(selfRot.x);
            this.ySelfRot = (float) Math.toDegrees(-selfRot.y);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (vehicle.level().isClientSide()) {
            tickRemoteRot();
            tickSound();
        }
        tickRot();
        updateRot();
    }

    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {
        if (!needPower || vehicle.hasPower()) {
            if (yRotSpeed != 0 && Math.abs((yAimRot - yRot) % 360) > 1 && yRot < yRotMax && yRot > yRotMin) {
                if (turnYSoundInstance == null) {
                    turnYSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_H.get(), 1f, 1f, 1f, true, 10, true, true, vehicle.getId());
                    turnYSoundInstance.play();
                }
            } else {
                if (turnYSoundInstance != null) {
                    turnYSoundInstance.stop();
                    turnYSoundInstance = null;
                }
            }
            if (xRotSpeed != 0 && Math.abs((xAimRot - xRot) % 360) > 1 && xRot < xRotMax && xRot > xRotMin) {
                if (turnXSoundInstance == null) {
                    turnXSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_V.get(), 1f, 1f, 1f, true, 10, true, true, vehicle.getId());
                    turnXSoundInstance.play();
                }
            } else {
                if (turnXSoundInstance != null) {
                    turnXSoundInstance.stop();
                    turnXSoundInstance = null;
                }
            }
        } else {
            if (turnXSoundInstance != null) {
                turnXSoundInstance.stop();
                turnXSoundInstance = null;
            }
            if (turnYSoundInstance != null) {
                turnYSoundInstance.stop();
                turnYSoundInstance = null;
            }
        }
    }

    protected void tickRemoteRot() {
        if (vehicle.level().isClientSide()
                && getOwner() != LocalVehiclePlayer.instance.getPlayer()) {
            xAimRot = xRemoteAimRot;
            yAimRot = yRemoteAimRot;
        }
    }

    protected void tickRot() {
        xRotO = xRot;
        yRotO = yRot;
        if (!needPower || vehicle.hasPower()) {
            float xDiff = Mth.wrapDegrees(xAimRot - xRot);
            float yDiff = Mth.wrapDegrees(yAimRot - yRot);
            if (Math.abs(xDiff) > xRotSpeed) {
                xRot += Math.signum(xDiff) * xRotSpeed;
            } else {
                xRot = xAimRot;
            }
            xRot = Mth.wrapDegrees(xRot);
            xRot = Math.max(Math.min(xRot, xRotMax - xSelfRot), xRotMin - xSelfRot);
            if (Math.abs(xRot - xRotO) > 180) {
                xRotO += Math.signum(xRot - xRotO) * 360;
            }
            if (Math.abs(yDiff) > yRotSpeed) {
                yRot += Math.signum(yDiff) * yRotSpeed;
            } else {
                yRot = yAimRot;
            }
            yRot = Mth.wrapDegrees(yRot);
            yRot = Math.max(Math.min(yRot, yRotMax - ySelfRot), yRotMin - ySelfRot);
            if (Math.abs(yRot - yRotO) > 180) {
                yRotO += Math.signum(yRot - yRotO) * 360;
            }
        }
    }

    public void updateRot() {
        if (structureGroup != null) {
            structureGroup.rotation = new Quaternionf(structureGroup.baseRotation).mul(Axis.YN.rotationDegrees(yRot).mul(Axis.XP.rotationDegrees(xRot)));
        }
    }

    @Override
    protected Quaternionf getViewGroupRotation(VehicleCubeGroup group, float partialTick) {
        if (partialTick != 1.0F && group == structureGroup) {
            return new Quaternionf(group.baseRotation)
                    .mul(Axis.YN.rotationDegrees(getViewYRot(partialTick))
                            .mul(Axis.XP.rotationDegrees(getViewXRot(partialTick))));
        }
        return super.getViewGroupRotation(group, partialTick);
    }

    public Vec2 worldRot() {
        return worldRot(xRot, yRot);
    }

    public Vec3 worldVec() {
        return worldVec(xRot, yRot);
    }

    public Vec2 worldRot(float xRot, float yRot) {
        Vec3 worldVec = worldVec(xRot, yRot);
        return VectorUtil.vecToRot(worldVec);
    }

    public Vec3 worldVec(float xRot, float yRot) {
        return new Vec3(baseRot().transform(VectorUtil.rotToVec(xRot, yRot).toVector3f()));
    }

    public Vec3 worldVecToLocalVec(Vec3 worldVec) {
        return new Vec3(baseRot().conjugate().transform(worldVec.toVector3f()));
    }

    public Vec2 worldVecToLocalRot(Vec3 worldVec) {
        return VectorUtil.vecToRot(worldVecToLocalVec(worldVec));
    }

    public float worldZRot() {
        Quaternionf rot = new Quaternionf();
        rot.rotateY(Math.toRadians(-yRot));
        rot.rotateX(Math.toRadians(-xRot));
        rot = baseRot().mul(rot);
        Vector3f eulerAngles = new Vector3f();
        rot.getEulerAnglesYXZ(eulerAngles);
        return (float) Math.toDegrees(eulerAngles.z);
    }

    public Quaternionf baseRot() {
        Quaternionf rotation = vehicle.rotYXZ();
        if (structureGroup != null) {
            rotation.mul(structureGroup.baseRotation);
            if (structureGroup.parent != null) {
                rotation.mul(structureGroup.parent.globalTransform().rotation());
            }
        }
        return rotation;
    }

    public Vec2 aimRot(Vec3 worldPosition) {
        if (structureGroup == null) {
            return new Vec2(0, 0);
        }
        Vec3 fromWorldPosition = vehicle.relativeRotPos(vehicle.position().add(structureGroup.globalTransform().offset()), false);
        Vec3 worldAim = new Vec3(worldPosition.x - fromWorldPosition.x, worldPosition.y - fromWorldPosition.y, worldPosition.z - fromWorldPosition.z);
        return worldVecToLocalRot(worldAim);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putFloat("xRot", this.xRot);
        tag.putFloat("yRot", this.yRot);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        this.xRot = nbt.getFloat("xRot");
        this.xRotO = this.xRot;
        this.xAimRot = this.xRot;
        this.yRot = nbt.getFloat("yRot");
        this.yRotO = this.yRot;
        this.yAimRot = this.yRot;
    }

    public float getViewXRot(float pPartialTicks) {
        return pPartialTicks == 1.0F ? xRot : Mth.lerp(pPartialTicks, this.xRotO, xRot);
    }

    public float getViewYRot(float pPartialTicks) {
        return pPartialTicks == 1.0F ? yRot : Mth.lerp(pPartialTicks, this.yRotO, yRot);
    }

    public float getXRotSpeed() {
        return xRotSpeed;
    }

    public void setXRotSpeed(float xRotSpeed) {
        this.xRotSpeed = xRotSpeed;
    }

    public float getYRotSpeed() {
        return yRotSpeed;
    }

    public void setYRotSpeed(float yRotSpeed) {
        this.yRotSpeed = yRotSpeed;
    }

    public float getXRotMax() {
        return xRotMax;
    }

    public void setXRotMax(float xRotMax) {
        this.xRotMax = xRotMax;
    }

    public float getXRotMin() {
        return xRotMin;
    }

    public void setXRotMin(float xRotMin) {
        this.xRotMin = xRotMin;
    }

    public float getYRotMax() {
        return yRotMax;
    }

    public void setYRotMax(float yRotMax) {
        this.yRotMax = yRotMax;
    }

    public float getYRotMin() {
        return yRotMin;
    }

    public void setYRotMin(float yRotMin) {
        this.yRotMin = yRotMin;
    }

    public float getXAimRot() {
        return xAimRot;
    }

    public void setXAimRot(float xAimRot) {
        this.xAimRot = xAimRot;
    }

    public float getYAimRot() {
        return yAimRot;
    }

    public void setYAimRot(float yAimRot) {
        this.yAimRot = yAimRot;
    }

    public float getXRot() {
        return xRot;
    }

    public void setXRot(float xRot) {
        this.xRot = xRot;
    }

    public float getYRot() {
        return yRot;
    }

    public void setYRot(float yRot) {
        this.yRot = yRot;
    }

    public float getXRemoteAimRot() {
        return xRemoteAimRot;
    }

    public void setXRemoteAimRot(float xRemoteAimRot) {
        this.xRemoteAimRot = xRemoteAimRot;
    }

    public float getYRemoteAimRot() {
        return yRemoteAimRot;
    }

    public void setYRemoteAimRot(float yRemoteAimRot) {
        this.yRemoteAimRot = yRemoteAimRot;
    }

    @Deprecated
    public RotatableUnit(String id, int index, AbstractVehicle vehicle) {
        super(id, index, vehicle);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setXRemoteAimRot, this::getXAimRot, 0f);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setYRemoteAimRot, this::getYAimRot, 0f);
    }

}
