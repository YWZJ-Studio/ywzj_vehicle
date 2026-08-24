package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
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
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;
import java.util.Optional;

public class TrackedVehicle extends AbstractVehicle
        implements IAnimationEntity<TrackedVehicle, TrackedVehicleContext> {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(TrackedVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_SPEED = SynchedEntityData.defineId(TrackedVehicle.class, EntityDataSerializers.FLOAT);
    public float brakeAcceleration = 0.025f;
    public float forwardAcceleration = 0.01f;
    public float backwardAcceleration = 0.01f;
    public float maxSpeedForward = 0.5f;
    public float maxSpeedBackward = 0.2f;
    public float turnAcceleration = 1f;
    public float maxTurn = 2f;
    public float trackSize = 0.3f;
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

        float vt = entityData.get(TURN_SPEED);
        int sig = (getLookAngle().dot(getDeltaMovement()) > 0 ? 1 : -1);
        float vf = (float) (new Vec3(getDeltaMovement().x, 0, getDeltaMovement().z).length() * sig);
        vf = Math.min(Math.abs(vf), Math.abs(entityData.get(FORWARD_SPEED))) * sig;
        if (!hasPower()) {
            entityData.set(FORWARD_SPEED, vf);
            entityData.set(TURN_SPEED, 0f);
            return new Vec3(0, 0, 0);
        }

        // 前后控制
        if (controlUnit.forward || controlUnit.backward) {
            if (controlUnit.forward) {
                if (vf < 0) {
                    vf += brakeAcceleration;
                } else {
                    vf += forwardAcceleration;
                }
            } else {
                if (vf > 0) {
                    vf -= brakeAcceleration;
                } else {
                    vf -= backwardAcceleration;
                }
            }
        }
        if (controlUnit.left || controlUnit.right) {
            if (vf < 0 && !controlUnit.backward) {
                vf += brakeAcceleration;
            }
        }
        vf = Mth.clamp(vf, -maxSpeedBackward, maxSpeedForward);
        entityData.set(FORWARD_SPEED, vf);

        // 转向控制
        if (controlUnit.left || controlUnit.right) {
            vt += controlUnit.right ? turnAcceleration : -turnAcceleration;
            vt = Mth.clamp(vt, -maxTurn, maxTurn);
        } else {
            if (vt < 0) {
                vt += turnAcceleration;
                vt = Math.min(vt, 0);
            } else if (vt > 0) {
                vt -= turnAcceleration;
                vt = Math.max(vt, 0);
            }
        }
        entityData.set(TURN_SPEED, vt);

        if (onGround() || (isInWater() && canWade)) {
            // 转向幅度应用于车身朝向
            if (controlUnit.backward) {
                vt *= -1;
            }
            this.setYRot(this.getYRot() + vt + Math.abs(vf) / maxSpeedForward * vt / 5);
            if (Math.abs(vt) > 0) {
                vf *= 0.98f;
            }

            // 前进速度应用于车身朝向
            Vec3 direction = getLookAngle();
            Vec3 motion = direction.normalize().scale(vf);
            motion = motion.add(0, Math.min(0, getDeltaMovement().y), 0);
            this.setDeltaMovement(motion);
        }
        return new Vec3(0, 0, 0);
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
        }
        double speed = getDeltaMovement().length();
        if (speed > 0) {
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
            Vec3 trackLeftPos = relativeRotPos(position().add(mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
            Vec3 trackRightPos = relativeRotPos(position().add(-mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
            ParticleUtil.spawnTracks(level(), trackSize, getYRot(), trackLeftPos, trackRightPos);
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
