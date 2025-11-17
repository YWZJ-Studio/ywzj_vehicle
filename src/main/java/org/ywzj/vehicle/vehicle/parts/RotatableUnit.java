package org.ywzj.vehicle.vehicle.parts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Math;
import org.joml.Quaternionf;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.custom.part.data.RotatableUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

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
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setXRemoteAimRot, this::getXAimRot, 0f);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setYRemoteAimRot, this::getYAimRot, 0f);
    }

    @Deprecated
    public RotatableUnit(String id, int index, AbstractVehicle vehicle) {
        super(id, index, vehicle);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setXRemoteAimRot, this::getXAimRot, 0f);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setYRemoteAimRot, this::getYAimRot, 0f);
    }

    public void tick() {
        super.tick();
        if (vehicle.level().isClientSide()) {
            tickSound();
        }
        tickRot();
    }

    public void updateOBBs() {
        for (VehicleBedrockCubeOBB unitBedrockCubeOBB : unitBedrockCubeOBBs) {
            OBB obb = unitBedrockCubeOBB.obb();
            Quaternionf rotSelf = new Quaternionf(unitBedrockCubeOBB.selfRot());
            rotSelf.rotateY(Math.toRadians(-getYRot()));
            Vec3 centerToPivot = unitBedrockCubeOBB.offset().subtract(this.pivotOffset);
            Quaternionf rotY = new Quaternionf().rotationY(Math.toRadians(getYRot()));
            Quaternionf rotX = new Quaternionf().rotationX(Math.toRadians(getXRot()));
            Quaternionf rotation = new Quaternionf(rotX).mul(rotY);
            Vec3 centerToPivotRot = new Vec3(rotation.transform(centerToPivot.toVector3f()));
            obb.setCenter(vehicle.relativeRotPos(vehicle.position().add(this.pivotOffset.add(centerToPivotRot)), false).toVector3f());
            rotSelf.rotateX(Math.toRadians(180 + getXRot()));
            obb.setRotation(vehicle.rotYXZ().mul(rotSelf));
        }
    }

    protected void tickRot() {
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        if (vehicle.level().isClientSide()) {
            this.xAimRot = this.xRemoteAimRot;
            this.yAimRot = this.yRemoteAimRot;
        }
        if (!needPower || vehicle.hasPower()) {
            float xDiff = Mth.wrapDegrees(this.xAimRot - this.xRot);
            float yDiff = Mth.wrapDegrees(this.yAimRot - this.yRot);
            if (Math.abs(xDiff) > getXRotSpeed()) {
                this.xRot += Math.signum(xDiff) * getXRotSpeed();
            } else {
                this.xRot = this.xAimRot;
            }
            this.xRot = Math.max(Math.min(this.xRot, getXRotMax()), getXRotMin());
            if (Math.abs(yDiff) > getYRotSpeed()) {
                this.yRot += Math.signum(yDiff) * getYRotSpeed();
            } else {
                this.yRot = this.yAimRot;
            }
            this.yRot = Math.max(Math.min(this.yRot, yRotMax), yRotMin);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {
        if (!needPower || vehicle.hasPower()) {
            if (yRotSpeed != 0 && Math.abs(yAimRot - yRot) > 1 && yRot < yRotMax && yRot > yRotMin) {
                if (turnYSoundInstance == null) {
                    turnYSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_H.get(), 1f, 1f, true, 10, true, true, vehicle.getId());
                    turnYSoundInstance.play();
                }
            } else {
                if (turnYSoundInstance != null) {
                    turnYSoundInstance.stop();
                    turnYSoundInstance = null;
                }
            }
            if (xRotSpeed != 0 && Math.abs(xAimRot - xRot) > 1 && xRot < xRotMax && xRot > xRotMin) {
                if (turnXSoundInstance == null) {
                    turnXSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_V.get(), 1f, 1f, true, 10, true, true, vehicle.getId());
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

}
