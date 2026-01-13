package org.ywzj.vehicle.vehicle.parts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.UnmodifiableView;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.custom.part.data.RotatableUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

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

    public float xRotSpeed;
    public float yRotSpeed;
    public float xRotMax = 90;
    public float xRotMin = -90;
    public float yRotMax = Float.MAX_VALUE;
    public float yRotMin = -Float.MAX_VALUE;

    protected RotatableUnit<?> baseRotatableUnit;
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
        this.yRot = rotInfo.yRot;
        this.needPower = rotInfo.needPower;

        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setXRemoteAimRot, this::getXAimRot, 0f);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setYRemoteAimRot, this::getYAimRot, 0f);
    }

    @Deprecated
    public RotatableUnit(String id, int index, AbstractVehicle vehicle) {
        super(id, index, vehicle);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setXRemoteAimRot, this::getXAimRot, 0f);
        this.getSyncData().define(SyncDataSerializers.FLOAT, this::setYRemoteAimRot, this::getYAimRot, 0f);
    }

    @Override
    public void combineAndInit(@UnmodifiableView Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        super.combineAndInit(partUnitsView, vehicle);
        if (data.getBase() != null) {
            PartUnit<?> basePart = partUnitsView.get(data.getBase());
            if (basePart instanceof WeaponUnit base) {
                this.setBaseRotatableUnit(base);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (vehicle.level().isClientSide()) {
            tickSound();
        }
        tickRot();
    }

    @Override
    public void updateOBBs() {
        Quaternionf rot = new Quaternionf()
                .rotateY(Math.toRadians(-combineYRot()))
                .rotateX(Math.toRadians(xRot));
        Quaternionf vehicleRot = vehicle.rotYXZ();
        for (VehicleBedrockCubeOBB unitOBB : unitBedrockCubeOBBs) {
            OBB obb = unitOBB.obb();
            Vector3f offset = unitOBB.offset()
                    .subtract(pivotOffset)
                    .toVector3f();
            rot.transform(offset);
            obb.setCenter(worldPosition(new Vec3(offset).add(pivotOffset)).toVector3f());
            obb.setRotation(new Quaternionf(vehicleRot).mul(rot).mul(unitOBB.selfRot()));
        }
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

    protected void tickRot() {
        xRotO = xRot;
        yRotO = yRot;
        if (vehicle.level().isClientSide()) {
            xAimRot = xRemoteAimRot;
            yAimRot = yRemoteAimRot;
        }
        if (!needPower || vehicle.hasPower()) {
            float xDiff = Mth.wrapDegrees(xAimRot - xRot);
            float yDiff = Mth.wrapDegrees(yAimRot - yRot);
            if (Math.abs(xDiff) > xRotSpeed) {
                xRot += Math.signum(xDiff) * xRotSpeed;
            } else {
                xRot = xAimRot;
            }
            xRot = Mth.wrapDegrees(xRot);
            xRot = Math.max(Math.min(xRot, xRotMax), xRotMin);
            if (Math.abs(xRot - xRotO) > 180) {
                xRotO += Math.signum(xRot - xRotO) * 360;
            }
            if (Math.abs(yDiff) > yRotSpeed) {
                yRot += Math.signum(yDiff) * yRotSpeed;
            } else {
                yRot = yAimRot;
            }
            yRot = Mth.wrapDegrees(yRot);
            yRot = Math.max(Math.min(yRot, yRotMax), yRotMin);
            if (Math.abs(yRot - yRotO) > 180) {
                yRotO += Math.signum(yRot - yRotO) * 360;
            }
        }
    }

    public Vec2 worldRot() {
        return worldRot(xRot, yRot);
    }

    public Vec3 worldVec() {
        return worldVec(xRot, yRot);
    }

    public Vec2 worldRot(float xRot, float yRot) {
        Vec3 worldVec = worldVec(xRot, yRot);
        return VectorUtil.worldVecToRot(worldVec);
    }

    public Vec3 worldVec(float xRot, float yRot) {
        return vehicle.relativeRotDirection(VectorUtil.calculateViewVector(xRot, (baseRotatableUnit != null ? baseRotatableUnit.combineYRot() : 0) + yRot), false);
    }

    public Vec2 vecToRot(Vec3 worldVec) {
        Vec3 vehicleVec = vehicle.relativeRotDirection(worldVec, true);
        float pitch = (float) Math.toDegrees(Math.atan2(-vehicleVec.y, Math.sqrt(worldVec.x * worldVec.x + worldVec.z * worldVec.z)));
        float yaw = (float) Math.toDegrees(-Math.atan2(vehicleVec.x, vehicleVec.z));
        yaw -= combineYRot() - getYRot();
        return new Vec2(pitch, yaw);
    }

    public float combineYRot() {
        if (baseRotatableUnit == null) {
            return getYRot();
        }
        return getYRot() + baseRotatableUnit.combineYRot();
    }

    /**
     * 计算车身、部件、附着部件都未旋转时某相对于载具枢轴的偏移xyz在经由车身、部件、附着部件旋转后的实际世界坐标
     */
    @Override
    public Vec3 worldPosition(Vec3 offsetFromVehicle) {
        if (offsetFromVehicle == null) {
            return vehicle.position();
        }
        return vehicle.relativeRotPos(vehicle.position().add(rotatedOffsetWithSelfRot(offsetFromVehicle)), false);
    }

    /**
     * 多层部件站发生依次旋转，计算某相对于载具枢轴的偏移xz因其中某层部件下所有部件旋转而所在的新偏移x'z'
     * @param rotatableUnit 目标层部件
     * @param offsetX 相对于载具枢轴的偏移x
     * @param offsetZ 相对于载具枢轴的偏移z
     * @return 下一层部件的枢轴偏移xz，本层计算得新偏移xz
     */
    public Vector4d rotatedOffsetWithBaseRot(RotatableUnit<?> rotatableUnit, double offsetX, double offsetZ) {
        if (rotatableUnit.baseRotatableUnit == null) {
            return new Vector4d(rotatableUnit.pivotOffset.x, rotatableUnit.pivotOffset.z, offsetX, offsetZ);
        }
        Vector4d pivotAndTargetOffset = rotatedOffsetWithBaseRot(rotatableUnit.baseRotatableUnit, offsetX, offsetZ);
        float rot = Math.toRadians(rotatableUnit.baseRotatableUnit.getYRot());
        float cos = Math.cos(rot);
        float sin = Math.sin(rot);
        float dx1 = (float) (rotatableUnit.pivotOffset.x - pivotAndTargetOffset.x);
        float dy1 = (float) (rotatableUnit.pivotOffset.z - pivotAndTargetOffset.y);
        float dx2 = (float) (pivotAndTargetOffset.z - pivotAndTargetOffset.x);
        float dy2 = (float) (pivotAndTargetOffset.w - pivotAndTargetOffset.y);
        return new Vector4d(pivotAndTargetOffset.x + dx1 * cos - dy1 * sin,
                pivotAndTargetOffset.y + dx1 * sin + dy1 * cos,
                pivotAndTargetOffset.x + dx2 * cos - dy2 * sin,
                pivotAndTargetOffset.y + dx2 * sin + dy2 * cos);
    }

    public Vec3 rotatedOffsetWithSelfRot(Vec3 offsetFromVehicle) {
        Vector4d offset = rotatedOffsetWithBaseRot(this, offsetFromVehicle.x, offsetFromVehicle.z);
        float rot = Math.toRadians(getYRot());
        float cos = Math.cos(rot);
        float sin = Math.sin(rot);
        float dx = (float) (offset.z - offset.x);
        float dy = (float) (offset.w - offset.y);
        return new Vec3(offset.x + dx * cos - dy * sin, offsetFromVehicle.y, offset.y + dx * sin + dy * cos);
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

    public RotatableUnit<?> getBaseRotatableUnit() {
        return baseRotatableUnit;
    }

    public void setBaseRotatableUnit(RotatableUnit<?> baseRotatableUnit) {
        this.baseRotatableUnit = baseRotatableUnit;
    }

}
