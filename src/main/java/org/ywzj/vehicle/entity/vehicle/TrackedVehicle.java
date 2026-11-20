package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.render.animation.context.TrackAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.TrackedVehicleContext;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.TrackedVehicleDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleDisplay;
import org.ywzj.vehicle.util.ParticleUtil;
import org.ywzj.vehicle.util.PhysicsHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.SuspensionUnit;
import org.ywzj.vehicle.vehicle.part.TrackUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;
import java.util.Optional;

public class TrackedVehicle extends AbstractVehicle implements IAnimationEntity<TrackedVehicle, TrackedVehicleContext> {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(TrackedVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_SPEED = SynchedEntityData.defineId(TrackedVehicle.class, EntityDataSerializers.FLOAT);
    public float brakeAcceleration = 0.025f;
    public float forwardAcceleration = 0.01f;
    public float backwardAcceleration = 0.01f;
    public float maxSpeedForward = 0.5f;
    public float maxSpeedBackward = 0.2f;
    public boolean dualFlow = true;
    public float turnAcceleration = 1f;
    public float maxTurn = 2f;
    public float trackSize = 0.3f;
    private Vec3 leftTrackPivot;
    private Vec3 rightTrackPivot;
    private Vec3 lastSingleFlowDrive = Vec3.ZERO;
    public double trackLength;
    private VehicleSound engineIdleSoundInstance;
    private VehicleSound engineRunSoundInstance;
    private VehicleSound trackRunSoundInstance;
    private TrackAnimationInstance trackAnimationInstance;
    private IAnimationInstance<TrackedVehicleContext> animationInstance;

    public TrackedVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public IAnimationInstance<TrackedVehicleContext> getAnimationInstance() {
        return animationInstance;
    }

    @Override
    public void initDisplayData(VehicleDisplay<?, ?> display) {
        super.initDisplayData(display);
        if (display instanceof TrackedVehicleDisplay trackedVehicleDisplay) {
            this.animationInstance = trackedVehicleDisplay.createAnimationInstance(this);
            var trackConfig = trackedVehicleDisplay.getTrackConfig();
            var leftTrackAnimation = trackedVehicleDisplay.getLeftTrackAnimation();
            var rightTrackAnimation = trackedVehicleDisplay.getRightTrackAnimation();
            if (animationInstance != null && trackConfig != null
                    && leftTrackAnimation != null && rightTrackAnimation != null) {
                TrackAnimationInstance instance = new TrackAnimationInstance(leftTrackAnimation, rightTrackAnimation);
                instance.setTrackWidth(trackConfig.trackWidth);
                instance.setModuleLength(trackConfig.moduleLength);
                this.animationInstance.getContext().setTrackAnimationInstance(instance);
            }
        }
    }

    public void initTrackPivots() {
        AABB left = null;
        AABB right = null;
        for (var part : getPartUnits()) {
            if (part instanceof TrackUnit track) {
                for (var path : track.getData().getTrackDefinitions()) {
                    if (path.points().isEmpty()) {
                        continue;
                    }
                    Vec3 first = path.points().get(0);
                    AABB bounds = new AABB(first, first);
                    for (Vec3 point : path.points()) {
                        bounds = bounds.minmax(new AABB(point, point));
                    }
                    if (bounds.getCenter().x > 0) {
                        left = left == null ? bounds : left.minmax(bounds);
                    } else {
                        right = right == null ? bounds : right.minmax(bounds);
                    }
                }
            }
        }
        double halfWidth = mainCubeOBB.obb().extents().x;
        leftTrackPivot = left == null ? centerOffset.add(halfWidth, 0, 0) : left.getCenter();
        rightTrackPivot = right == null ? centerOffset.add(-halfWidth, 0, 0) : right.getCenter();
    }

    public float innerTrackBrake(float turnSpeed) {
        return maxTurn > 0 ? Math.min(Math.abs(turnSpeed) / maxTurn, 1f) : 0f;
    }

    public float getForwardSpeed() {
        return this.entityData.get(FORWARD_SPEED);
    }

    public float getTurnSpeed() {
        return this.entityData.get(TURN_SPEED);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FORWARD_SPEED, 0f);
        this.entityData.define(TURN_SPEED, 0f);
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(weaponIndex, aimContexts, operator);
        }
    }

    public SoundEvent getTrackRunSound() {
        Optional<VehicleDisplay<?, ?>> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(getDisplayId());
        return displayOptional.map(display -> display.getSoundEvents().get("track_run")).orElse(null);
    }

    @Override
    protected Vec3 tickMove() {
        if (getDriver() == null) {
            controlUnit.reset();
        }

        Vec3 velocity = getDeltaMovement();
        if (!hasPower() || !(onGround() || (isInWater() && canWade))) {
            lastSingleFlowDrive = Vec3.ZERO;
            Vec3 direction = VectorUtil.rotToVec(getXRot(), getYRot());
            entityData.set(FORWARD_SPEED, (float) velocity.dot(direction));
            entityData.set(TURN_SPEED, 0f);
            return Vec3.ZERO;
        }

        float powerScale = getSuspensionPowerScale();
        int throttle = (controlUnit.forward ? 1 : 0) - (controlUnit.backward ? 1 : 0);
        int steering = (controlUnit.right ? 1 : 0) - (controlUnit.left ? 1 : 0);
        float turnSpeed = Mth.approach(getTurnSpeed(), steering * maxTurn * powerScale, turnAcceleration * powerScale);
        Vec3 heading = VectorUtil.rotToVec(0, getYRot());
        if (dualFlow) {
            moveDualFlow(velocity, heading, throttle, steering, powerScale, turnSpeed);
        } else {
            moveSingleFlow(velocity, heading, throttle, steering, powerScale, turnSpeed);
        }
        return Vec3.ZERO;
    }

    private void moveDualFlow(Vec3 velocity, Vec3 heading, int throttle, int steering, float powerScale, float turnSpeed) {
        Vec3 direction = VectorUtil.rotToVec(getXRot(), getYRot());
        float previousForwardSpeed = (float) velocity.dot(direction);
        float forwardSpeed = previousForwardSpeed;
        if (throttle == 0 && steering != 0 && forwardSpeed < 0) {
            forwardSpeed = Math.min(0, forwardSpeed + brakeAcceleration);
        }
        forwardSpeed = applyThrottle(forwardSpeed, throttle, powerScale);
        // 转向幅度应用于车身朝向
        float yawStep = forwardSpeed < 0 ? -turnSpeed : turnSpeed;
        float speedRatio = maxSpeedForward > 0 ? Mth.clamp(Math.abs(forwardSpeed) / maxSpeedForward, 0, 1) : 0;
        setYRot(getYRot() + yawStep * (1 + speedRatio / 5));
        if (turnSpeed != 0) {
            forwardSpeed *= 0.98f;
        }

        // 轴向加速
        velocity = velocity.add(direction.scale(forwardSpeed - previousForwardSpeed));
        // 水平转向
        double horizontalSpeed = velocity.dot(heading);
        Vec3 newHeading = VectorUtil.rotToVec(0, getYRot());
        Vec3 movement = new Vec3(newHeading.x * horizontalSpeed, velocity.y, newHeading.z * horizontalSpeed);
        applyDriveMovement(movement, forwardSpeed, turnSpeed, throttle != 0 && powerScale > 0);
    }

    private void moveSingleFlow(Vec3 velocity, Vec3 heading, int throttle, int steering, float powerScale, float turnSpeed) {
        // 旋转中心与两履带中点可能不重合。用物理结算后保留的位移比例还原履带均速。
        double previousDistanceSqr = lastSingleFlowDrive.horizontalDistanceSqr();
        float meanSpeed = previousDistanceSqr > 1.0E-10
                ? (float) (getForwardSpeed() * velocity.dot(lastSingleFlowDrive) / previousDistanceSqr)
                : (float) velocity.dot(heading);
        float outerSpeed = meanSpeed / (1 - 0.5f * innerTrackBrake(getTurnSpeed()));
        outerSpeed = applyThrottle(outerSpeed, throttle, powerScale);
        double gauge = leftTrackPivot.x - rightTrackPivot.x;
        float turn = powerScale > 0 && gauge > 1.0E-5 ? turnSpeed : 0;
        float maxMeanSpeed = Math.abs(applyThrottle(meanSpeed, throttle, powerScale));
        if (throttle == 0 && steering != 0 && turn != 0 && outerSpeed >= 0) {
            // 仅打方向时，外履带也能带动车辆从静止起转。
            float pivotSpeed = Math.min(maxSpeedForward, (float) (Math.toRadians(maxTurn * powerScale) * gauge));
            if (outerSpeed < pivotSpeed) {
                outerSpeed = Math.min(pivotSpeed, outerSpeed + forwardAcceleration * powerScale);
                maxMeanSpeed += forwardAcceleration * powerScale;
            }
        }
        // 高速时限制履带速差，保证实际偏航和动画使用同一个制动比例。
        float brake = innerTrackBrake(turn);
        if (outerSpeed != 0) {
            float maxDifference = (float) (Math.toRadians(maxTurn * powerScale) * Math.max(0, gauge));
            brake = Math.min(brake, maxDifference / Math.abs(outerSpeed));
        }
        float meanRatio = 1 - 0.5f * brake;
        // 松开内侧制动或反打方向时，车体只能按可用驱动力恢复速度。
        meanSpeed = Mth.clamp(outerSpeed * meanRatio, -maxMeanSpeed, maxMeanSpeed);
        outerSpeed = meanSpeed / meanRatio;
        double yaw = brake > 0 ? Math.signum(turn) * outerSpeed * brake / gauge : 0;
        float yawDegrees = (float) Math.toDegrees(yaw);
        Vec3 drive = heading.scale(meanSpeed);
        if (yaw != 0) {
            Vec3 midpoint = leftTrackPivot.add(rightTrackPivot).scale(0.5).subtract(centerOffset);
            Vec3 offset = relativeRotDirection(midpoint, false);
            // 均速给出中点弧长，换成弦位移，再扣除中点绕实体原点的偏移。
            double halfYaw = yaw * 0.5;
            Vec3 travel = VectorUtil.rotToVec(0, getYRot() + yawDegrees * 0.5f)
                    .scale(meanSpeed * Math.sin(halfYaw) / halfYaw);
            drive = travel.add(offset.subtract(offset.yRot((float) -yaw)));
            setYRot(getYRot() + yawDegrees);
        }
        lastSingleFlowDrive = new Vec3(drive.x, 0, drive.z);
        Vec3 movement = new Vec3(drive.x, velocity.y, drive.z);
        boolean poweredDrive = powerScale > 0 && (throttle != 0 || (steering != 0 && turn != 0 && outerSpeed > 0));
        applyDriveMovement(movement, meanSpeed, Math.signum(turn) * brake * maxTurn, poweredDrive);
    }

    private float applyThrottle(float forwardSpeed, int throttle, float powerScale) {
        if (throttle > 0) {
            forwardSpeed = forwardSpeed < 0 ? Math.min(0, forwardSpeed + brakeAcceleration)
                    : forwardSpeed + forwardAcceleration * powerScale;
        } else if (throttle < 0) {
            forwardSpeed = forwardSpeed > 0 ? Math.max(0, forwardSpeed - brakeAcceleration)
                    : forwardSpeed - backwardAcceleration * powerScale;
        }
        return Mth.clamp(forwardSpeed, -maxSpeedBackward, maxSpeedForward);
    }

    private void applyDriveMovement(Vec3 movement, float forwardSpeed, float turnSpeed, boolean poweredDrive) {
        if (poweredDrive && movement.horizontalDistanceSqr() > 0) {
            movement = compensateDriveFriction(movement);
        }
        setDeltaMovement(movement);
        entityData.set(FORWARD_SPEED, forwardSpeed);
        entityData.set(TURN_SPEED, turnSpeed);
    }

    private Vec3 compensateDriveFriction(Vec3 velocity) {
        if (!onGround()) return velocity;
        Vec3 frictionVelocity = getSuspensionUnits().stream().anyMatch(SuspensionUnit::isGrounded)
                ? new Vec3(velocity.x, 0, velocity.z) : velocity;
        return velocity.add(frictionVelocity.normalize().scale(PhysicsHelper.accelerationPerTick(
                physicsEngine.physicsInfo.friction, physicsEngine.physicsInfo.mass)));
    }

    @Override
    protected void tickEngineSpeed() {
        super.tickEngineSpeed();
        float engineSpeed = getEngineSpeed();
        if (controlUnit.forward || controlUnit.backward || controlUnit.left || controlUnit.right) {
            if (hasPower()) {
                Vec3 velocity = getDeltaMovement();
                Vec3 vehicleDirection = getLookAngle();
                float angle = (float) Math.toDegrees(VectorUtil.angleBetween(velocity, vehicleDirection));
                if ((angle < 90 && controlUnit.forward) || (angle > 90 && controlUnit.backward)) {
                    setEngineSpeed(Mth.clamp(engineSpeed + 1, 0, 100));
                } else if (controlUnit.left || controlUnit.right) {
                    setEngineSpeed(Mth.clamp(engineSpeed + 2, 0, 100));
                } else {
                    setEngineSpeed(Mth.clamp(engineSpeed - 2, 0, 100));
                }
            }
        } else {
            if (engineSpeed > 60) {
                setEngineSpeed(Mth.clamp(engineSpeed - 2, 0, 100));
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {
        super.tickSound();
        Vec3 forwardDirection = new Vec3(axes()[2]);
        Vec3 velocity = getDeltaMovement();
        double speed = Math.abs(velocity.dot(forwardDirection));
        if (speed > 0.01) {
            if (trackRunSoundInstance == null) {
                SoundEvent trackRunSound = getTrackRunSound();
                if (trackRunSound != null) {
                    trackRunSoundInstance = new VehicleSound(trackRunSound, 1f, viewInfo.soundDistance, 1f, true, 50, true, true, this.getId());
                    trackRunSoundInstance.play();
                }
            } else {
                trackRunSoundInstance.setPitch((float) (maxSpeedForward - speed) / maxSpeedForward * 0.3f + 0.8f);
            }
        } else if (trackRunSoundInstance != null) {
            trackRunSoundInstance.stop();
            trackRunSoundInstance = null;
        }
        if (getPower() == 5 && isEngineOn()) {
            SoundEvent engineStartSound = getEngineStartSound();
            if (engineStartSound != null) {
                new VehicleSound(engineStartSound, 1f, viewInfo.soundDistance, 1f, false, 50, true, true, this.getId()).play();
            }
        }
        if (!hasPower()) {
            if (engineIdleSoundInstance != null) {
                engineIdleSoundInstance.stop();
                engineIdleSoundInstance = null;
            }
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.stop();
                engineRunSoundInstance = null;
            }
            return;
        }
        float engineSpeed = getEngineSpeed();
        if (engineSpeed <= 60) {
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.stop();
                engineRunSoundInstance = null;
            }
            if (engineIdleSoundInstance == null) {
                SoundEvent engineIdleSound = getEngineIdleSound();
                if (engineIdleSound != null) {
                    engineIdleSoundInstance = new VehicleSound(engineIdleSound, 1f, viewInfo.soundDistance, 1f, true, 50, true, true, this.getId());
                    engineIdleSoundInstance.play();
                }
            }
        } else {
            if (engineIdleSoundInstance != null) {
                engineIdleSoundInstance.stop();
                engineIdleSoundInstance = null;
            }
            if (engineRunSoundInstance == null) {
                SoundEvent engineRunSound = getEngineRunSound();
                if (engineRunSound != null) {
                    engineRunSoundInstance = new VehicleSound(engineRunSound, 1f, viewInfo.soundDistance, 1f, true, 50, true, true, this.getId());
                    engineRunSoundInstance.play();
                }
            } else {
                engineRunSoundInstance.setPitch((engineSpeed - 60) / 40 * 0.3f + 0.8f);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void tickParticle() {
        super.tickParticle();
        // 履带印
        trackLength += getDeltaMovement().length();
        if (trackLength >= 1) {
            trackLength = 0;
            if (hasSuspension()) {
                ParticleUtil.spawnSuspensionTracks(this);
            } else {
                Vec3 trackLeftPos = relativeRotPos(position().add(mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
                Vec3 trackRightPos = relativeRotPos(position().add(-mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
                ParticleUtil.spawnTracks(level(), trackSize, getYRot(), trackLeftPos, trackRightPos);
            }
        }
        // 扬尘
        if (level().isClientSide() && (getDeltaMovement().horizontalDistanceSqr() > 0.01 || getTurnSpeed() != 0)) {
            if (hasSuspension()) {
                ParticleUtil.spawnSuspensionDust(this);
            } else {
                Vec3 trackLeftPos = relativeRotPos(position().add(mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
                Vec3 trackRightPos = relativeRotPos(position().add(-mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
                ParticleUtil.spawnTrackDust(this, trackLeftPos, trackRightPos);
            }
        }
        // 引擎烟
        if (hasPower()) {
            double velocity = Math.abs(entityData.get(FORWARD_SPEED)) + Math.abs(entityData.get(TURN_SPEED));
            if (engineParticleTick > (maxSpeedForward * 0.5 - velocity) / maxSpeedForward * 10) {
                ParticleUtil.spawnEngineSmoke(level(), energyInfo.engineParticleOffsets, position(), new Vec3(xo, yo, zo),
                        pos -> relativeRotPos(pos, false), getLookAngle().normalize().scale(-0.2),
                        (int) Math.ceil(velocity / 16 + 1), 20, 0.3f, 0.4f);
                engineParticleTick = 0;
            } else {
                engineParticleTick += 1;
            }
        }
    }

    /**
     * 获取履带动画实例
     */
    @Nullable
    @OnlyIn(Dist.CLIENT)
    public TrackAnimationInstance getTrackAnimationInstance() {
        return trackAnimationInstance;
    }

    @OnlyIn(Dist.CLIENT)
    public void setTrackAnimationInstance(TrackAnimationInstance trackAnimationInstance) {
        this.trackAnimationInstance = trackAnimationInstance;
    }

}
