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
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllParticleTypes;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.render.animation.context.WheeledVehicleContext;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.WheeledVehicleDisplay;
import org.ywzj.vehicle.particle.SmokeCloudOption;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class WheeledVehicle extends AbstractVehicle implements IAnimationEntity<WheeledVehicle, WheeledVehicleContext> {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(WheeledVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_ANGLE = SynchedEntityData.defineId(WheeledVehicle.class, EntityDataSerializers.FLOAT);
    public float brakeForce = 0.025f;
    public float forwardForce = 0.01f;
    public float backwardForce = 0.01f;
    public float maxSpeedForward = 0.5f;
    public float maxSpeedBackward = 0.2f;
    public float turnStep = 0.1f;
    public float maxTurn = 2f;
    public float trackSize = 0.1f;
    public boolean loseTraction;
    public int regainTractionTick;
    public float turnAngle;
    public float turnAngleO;
    public float wheelRotation;
    public double trackLength;
    public long lastRenderTime;
    private VehicleSound engineIdleSoundInstance;
    private VehicleSound engineRunSoundInstance;
    private VehicleSound tireSquealSoundInstance;
    private IAnimationInstance<WheeledVehicleContext> animationInstance;

    public WheeledVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public IAnimationInstance<WheeledVehicleContext> getAnimationInstance() {
        return animationInstance;
    }

    @Override
    public void initDisplayData(BaseDisplay display) {
        if (display instanceof WheeledVehicleDisplay wheeledVehicleDisplay) {
            this.animationInstance = wheeledVehicleDisplay.createAnimationInstance(this);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORWARD_SPEED, 0f);
        builder.define(TURN_ANGLE, 0f);
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(weaponIndex, aimContexts, operator);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickInput();
        }
    }

    private void tickInput() {
        turnAngleO = turnAngle;
        if (level().isClientSide()) {
            turnAngle = getTurnAngle();
        }
    }

    @Override
    protected Vec3 tickMove() {
        if (getDriver() == null) {
            controlUnit.reset();
        }

        double powerScale = getPower() / 100;
        Vec3 velocity = getDeltaMovement();
        double gVelocity = velocity.y;
        velocity = new Vec3(velocity.x, 0, velocity.z);
        Vec3 vehicleDirection = getLookAngle();
        double motion = velocity.length();
        float angle = (float) Math.toDegrees(VectorUtil.angleBetween(velocity, vehicleDirection));
        vehicleDirection = new Vec3(vehicleDirection.x, 0, vehicleDirection.z);
        Vec3 turnDirection = new Vec3(-vehicleDirection.z, 0, vehicleDirection.x);

        // 手刹控制
        boolean handbrake = controlUnit.up;
        if (handbrake) {
            velocity = velocity.scale(0.98);
            motion = velocity.length();
        }

        // 转向控制
        float turnAngle = getTurnAngle();
        if (controlUnit.left || controlUnit.right) {
            turnAngle += controlUnit.right ? turnStep : -turnStep;
            turnAngle = Mth.clamp(turnAngle, -maxTurn, maxTurn);
        } else {
            if (turnAngle < 0) {
                turnAngle += turnStep;
                turnAngle = Math.min(turnAngle, 0);
            } else if (turnAngle > 0) {
                turnAngle -= turnStep;
                turnAngle = Math.max(turnAngle, 0);
            }
        }
        setTurnAngle(turnAngle);
        if (angle > 90) {
            turnAngle *= -1;
        }

        // 前后控制
        Vec3 propulsiveForce = Vec3.ZERO;
        if (controlUnit.forward) {
            if (motion != 0 && angle > 90) {
                propulsiveForce = vehicleDirection.normalize().scale(brakeForce);
            } else {
                if (motion < maxSpeedForward) {
                    propulsiveForce = vehicleDirection.normalize().scale(forwardForce * powerScale);
                }
            }
        } else if (controlUnit.backward) {
            if (motion != 0 && angle < 90) {
                propulsiveForce = vehicleDirection.normalize().scale(-brakeForce);
            } else {
                if (motion < maxSpeedBackward) {
                    propulsiveForce = vehicleDirection.normalize().scale(-backwardForce * powerScale);
                }
            }
        }

        // 失去抓地力
        if (!onGround()) {
            motion *= 0.33f;
            propulsiveForce = Vec3.ZERO;
        }

        // 速度与转向角度产生转向力，并产生车头偏转
        float turnStep = (float) Math.toDegrees(Math.atan2(motion
                * (turnAngle / maxTurn)
                * (1 - motion / maxSpeedForward * 0.5)
                * (loseTraction ? 0.8 : 1), mainCubeOBB.depth));
        Vec3 turnForce = turnDirection.normalize().scale(turnStep / 100);
        this.setYRot(this.getYRot() + turnStep);
        // 受力产生加速度
        Vec3 deltaVelocity = propulsiveForce.add(turnForce).scale(1 / physicsEngine.mass);
        velocity = velocity.add(deltaVelocity);
        motion = velocity.length();
        angle = (float) Math.toDegrees(VectorUtil.angleBetween(velocity, vehicleDirection));
        // 车头朝向与速度方向不一致，产生回正倾向
        if (motion != 0) {
            Vec3 turnVector = vehicleDirection.scale(angle > 90 ? -motion : motion).subtract(velocity);
            if (turnVector.length() > 0.001f) {
                float turnLength = (float) turnVector.length();
                float k1 = (float) Math.max(0.01, 1 - Math.pow(motion / maxSpeedForward, 0.5));
                float k2 = 0.1f / turnLength;
                float k3 = handbrake ? (float) 0.01 : 1;
                float k4 = k1 * k2 * k3;
                if (k4 < 0.003) {
                    loseTraction = true;
                    regainTractionTick = 10;
                } else if (regainTractionTick > 0) {
                    regainTractionTick -= 1;
                    if (regainTractionTick == 0) {
                        loseTraction = false;
                    }
                }
                float f = (float) (Math.min(1, 10 * k4 * (loseTraction ? 0.5 : 1)) * turnLength);
                if (f != 0) {
                    velocity = velocity.add(turnVector.normalize().scale(f));
                    velocity = velocity.normalize().scale(Math.max(0, motion - 0.001 * angle / 90));
                }
            } else {
                loseTraction = false;
            }
        }

        // 轴向速度
        motion = velocity.length();
        angle = (float) Math.toDegrees(VectorUtil.angleBetween(velocity, vehicleDirection));
        entityData.set(FORWARD_SPEED, (angle < 90 ? 1 : -1) * (float) motion);

        this.setDeltaMovement(new Vec3(velocity.x, velocity.y + gVelocity, velocity.z));
        return Vec3.ZERO;
    }

    @Override
    protected void tickEngineSpeed() {
        super.tickEngineSpeed();
        float engineSpeed = getEngineSpeed();
        if (controlUnit.forward || controlUnit.backward) {
            if (hasPower()) {
                Vec3 velocity = getDeltaMovement();
                Vec3 vehicleDirection = getLookAngle();
                float angle = (float) Math.toDegrees(VectorUtil.angleBetween(velocity, vehicleDirection));
                if ((angle < 90 && controlUnit.forward) || (angle > 90 && controlUnit.backward)) {
                    setEngineSpeed(Mth.clamp(engineSpeed + 1, 0, 100));
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
        float engineSpeed = getEngineSpeed();
        if (engineSpeed == 60) {
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
        } else if (engineSpeed > 60) {
            if (engineIdleSoundInstance != null) {
                engineIdleSoundInstance.stop();
                engineIdleSoundInstance = null;
            }
            float pitch = (engineSpeed - 60) / 40 * 0.3f + 0.8f;
            if (engineRunSoundInstance == null) {
                SoundEvent engineRunSound = getEngineRunSound();
                if (engineRunSound != null) {
                    engineRunSoundInstance = new VehicleSound(engineRunSound, 1f, viewInfo.soundDistance, 1f, true, 50, true, true, this.getId());
                    engineRunSoundInstance.play();
                }
            } else {
                engineRunSoundInstance.setPitch(pitch);
            }
        }
        Vec3 velocity = new Vec3(getDeltaMovement().x, 0, getDeltaMovement().z);
        if (velocity.length() > 0.1 && Math.sin(VectorUtil.angleBetween(velocity, getLookAngle())) > Math.sin(Math.PI / 10)) {
            if (tireSquealSoundInstance == null) {
                tireSquealSoundInstance = new VehicleSound(AllSounds.TIRE_SQUEAL.get(), 1f, 1f, 1f, true, 50, true, true, this.getId());
                tireSquealSoundInstance.play();
            }
        } else if (tireSquealSoundInstance != null) {
            tireSquealSoundInstance.stop();
            tireSquealSoundInstance = null;
        }
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
        // 履带印
        trackLength += getDeltaMovement().length();
        if (trackLength >= 0.5) {
            trackLength = 0;
            Vec3 trackLeftPos = relativeRotPos(position().add(mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
            Vec3 trackRightPos = relativeRotPos(position().add(-mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
            if (EntityUtil.isOnBlockSurface(this, trackLeftPos)) {
                this.level().addParticle(AllParticleTypes.TRACK.get(), true,
                        trackLeftPos.x, trackLeftPos.y, trackLeftPos.z,  trackSize, this.getYRot(), 0
                );
            }
            if (EntityUtil.isOnBlockSurface(this, trackRightPos)) {
                this.level().addParticle(AllParticleTypes.TRACK.get(), true,
                        trackRightPos.x, trackRightPos.y, trackRightPos.z,  trackSize, this.getYRot(), 0
                );
            }
        }
        // 引擎烟
        if (hasPower()) {
            double velocity = Math.abs(entityData.get(FORWARD_SPEED));
            if (engineParticleTick > (maxSpeedForward * 0.5 - velocity) / maxSpeedForward * 10) {
                energyInfo.engineParticleOffsets.forEach(offset -> {
                    Vec3 engineSmokePos = this.position().add(offset);
                    Vec3 engineSmokeVelocity = this.getLookAngle().normalize().scale(-0.1);
                    engineSmokePos = relativeRotPos(engineSmokePos, false);
                    for (int count = 0; count < velocity / 16 + 1; count++) {
                        level().addParticle(new SmokeCloudOption(0.3f, 0.3f, 0.3f,
                                        0.0f, 0.0f, 0.0f, 0.7f,
                                        10, 0.2f, 0.3f, 0.005f), true,
                                engineSmokePos.x, engineSmokePos.y, engineSmokePos.z,
                                engineSmokeVelocity.x + (level().random.nextDouble() - 0.5) * 0.05,
                                engineSmokeVelocity.y + (level().random.nextDouble() - 0.5) * 0.05,
                                engineSmokeVelocity.z + (level().random.nextDouble() - 0.5) * 0.05);
                    }
                });
                engineParticleTick = 0;
            } else {
                engineParticleTick += 1;
            }
        }
    }

    public float getForwardSpeed() {
        return this.entityData.get(FORWARD_SPEED);
    }

    public float getTurnAngle() {
        return this.entityData.get(TURN_ANGLE);
    }

    public void setTurnAngle(float value) {
        turnAngle = value;
        this.entityData.set(TURN_ANGLE, value);
    }

    public float getSpeed() {
        return (float) getDeltaMovement().length();
    }

}
