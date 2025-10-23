package org.ywzj.vehicle.vehicle;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Math;
import org.joml.Quaternionf;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerRotatableUnitRot;
import org.ywzj.vehicle.util.EntityUtil;

/**
 * 可旋转运动的载具部件
 */
public class RotatableUnit extends PartUnit {

    public float xRot;
    public float yRot;
    public float xRotO;
    public float yRotO;
    public float xRotSpeed;
    public float yRotSpeed;
    public float xRotMax = 90;
    public float xRotMin = -90;
    public float yRotMax = Float.MAX_VALUE;
    public float yRotMin = -Float.MAX_VALUE;
    public float xAimRot;
    public float yAimRot;
    private VehicleSound turnYSoundInstance;
    private VehicleSound turnXSoundInstance;

    public RotatableUnit(String name, int index, AbstractVehicle vehicle) {
        super(name, index, vehicle);
        this.initStructureModel(name);
        this.initOBBs();
    }

    public RotatableUnit(Component name, int index, AbstractVehicle vehicle) {
        super(name, index, vehicle);
    }

    public void tick() {
        super.tick();
        if (vehicle.hasPower()) {
            tickRot();
            tickSound();
        } else {
            this.xRotO = this.xRot;
            this.yRotO = this.yRot;
        }
    }

    public void updateOBBs() {
        for (VehicleBedrockCubeOBB unitBedrockCubeOBB : unitBedrockCubeOBBs) {
            OBB obb = unitBedrockCubeOBB.obb();
            Quaternionf rotSelf = new Quaternionf(unitBedrockCubeOBB.selfRot());
            rotSelf.rotateY(Math.toRadians(-yRot));
            Vec3 pivotOffset = new Vec3(unitBone.x / 16, unitBone.y / 16, unitBone.z / 16);
            Vec3 centerToPivot = unitBedrockCubeOBB.offset().subtract(pivotOffset);
            Quaternionf rotY = new Quaternionf().rotationY(Math.toRadians(yRot));
            Quaternionf rotX = new Quaternionf().rotationX(Math.toRadians(xRot));
            Quaternionf rotation = new Quaternionf(rotX).mul(rotY);
            Vec3 centerToPivotRot = new Vec3(rotation.transform(centerToPivot.toVector3f()));
            obb.setCenter(vehicle.relativeRotPos(vehicle.position().add(pivotOffset.add(centerToPivotRot))).toVector3f());
            rotSelf.rotateX(Math.toRadians(180 + xRot));
            obb.setRotation(vehicle.rotYXZ().mul(rotSelf));
        }
    }

    protected void tickRot() {
        this.xRotO = this.xRot;
        this.yRotO = this.yRot;
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
        if (!vehicle.level().isClientSide()) {
            if (xDiff != 0 || yDiff != 0) {
                vehicle.level().players().stream()
                        .filter(player -> EntityUtil.withinBroadcastRange(vehicle, player) && getOwner() != player)
                        .forEach(player ->
                                Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ServerRotatableUnitRot(this)));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {
        if (vehicle.hasPower()) {
            if (Math.abs(yAimRot - yRot) > 1) {
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
            if (Math.abs(xAimRot - xRot) > 1 && xRot < xRotMax && xRot > xRotMin) {
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

}
