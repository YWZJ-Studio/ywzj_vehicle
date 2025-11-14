package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllParticleTypes;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.util.EntityUtil;
import org.ywzj.vehicle.util.VectorUtil;

public abstract class WheeledVehicle extends AbstractVehicle {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(WheeledVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_ANGLE = SynchedEntityData.defineId(WheeledVehicle.class, EntityDataSerializers.FLOAT);
    public float brakeForce = 0.025f;
    public float forwardForce = 0.01f;
    public float backwardForce = 0.01f;
    public float maxSpeedForward = 0.5f;
    public float maxSpeedBackward = 0.2f;
    public float turnStep = 0.1f;
    public float maxTurn = 2f;
    public boolean loseTraction;
    public int regainTractionTick;
    public float wheelRotation;
    public double trackLength;
    public long lastRenderTime;
    private VehicleSound engineIdleSoundInstance;
    private VehicleSound engineRunSoundInstance;
    private VehicleSound tireSquealSoundInstance;

    public WheeledVehicle(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.thirdPersonCenterOffset = new Vec3(0, 4, 0);
        this.thirdPersonDistance = 7;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FORWARD_SPEED, 0f);
        this.entityData.define(TURN_ANGLE, 0f);
    }

    @Override
    public void onEnterVehicle(LivingEntity pPlayer) {
        super.onEnterVehicle(pPlayer);
        this.playSound(SoundEvents.IRON_TRAPDOOR_OPEN);
    }

    @Override
    public void onLeaveVehicle(LivingEntity entity) {
        super.onLeaveVehicle(entity);
        this.playSound(SoundEvents.IRON_TRAPDOOR_CLOSE);
    }

    @Override
    protected void tickSound() {
        super.tickSound();
        if (getDriver() == null) {
            if (engineIdleSoundInstance != null) {
                engineIdleSoundInstance.stop();
                engineIdleSoundInstance = null;
            }
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.stop();
                engineRunSoundInstance = null;
            }
        } else {
            if (getFuel() != 0 && getPower() == 5 && isEngineOn()) {
                SoundEvent engineStartSound = getEngineStartSound();
                if (engineStartSound != null) {
                    new VehicleSound(engineStartSound, soundDistance, 1f, false, 50, true, true, this.getId()).play();
                }
            }
            float vf = getSpeed();
            if (vf == 0) {
                if (engineRunSoundInstance != null) {
                    engineRunSoundInstance.stop();
                    engineRunSoundInstance = null;
                }
                if (!hasPower()) {
                    if (engineIdleSoundInstance != null) {
                        engineIdleSoundInstance.stop();
                        engineIdleSoundInstance = null;
                    }
                } else if (engineIdleSoundInstance == null) {
                    SoundEvent engineIdleSound = getEngineIdleSound();
                    if (engineIdleSound != null) {
                        engineIdleSoundInstance = new VehicleSound(engineIdleSound, soundDistance, 1f, true, 50, true, true, this.getId());
                        engineIdleSoundInstance.play();
                    }
                }
            } else {
                if (engineIdleSoundInstance != null) {
                    engineIdleSoundInstance.stop();
                    engineIdleSoundInstance = null;
                }
                float volume = Math.max(0.7f, vf / maxSpeedForward);
                float pitch = Math.abs(vf) / maxSpeedForward * 0.3f + 0.8f;
                if (engineRunSoundInstance == null) {
                    SoundEvent engineRunSound = getEngineRunSound();
                    if (engineRunSound != null) {
                        engineRunSoundInstance = new VehicleSound(engineRunSound, volume * soundDistance, 1f, true, 50, true, true, this.getId());
                        engineRunSoundInstance.play();
                    }
                } else {
                    engineRunSoundInstance.setVolume(volume * soundDistance);
                    engineRunSoundInstance.setPitch(pitch);
                }
            }
        }
        Vec3 velocity = new Vec3(getDeltaMovement().x, 0, getDeltaMovement().z);
        if (velocity.length() > 0.1 && Math.sin(VectorUtil.angleBetween(velocity, getLookAngle())) > Math.sin(Math.PI / 10)) {
            if (tireSquealSoundInstance == null) {
                tireSquealSoundInstance = new VehicleSound(AllSounds.TIRE_SQUEAL.get(), 1, 1, true, 50, true, true, this.getId());
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
        trackLength += getDeltaMovement().length();
        if (trackLength >= 0.5) {
            trackLength = 0;
            Vec3 trackLeftPos = relativeRotPos(position().add(mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
            Vec3 trackRightPos = relativeRotPos(position().add(-mainCubeOBB.obb().extents().x, 0, -mainCubeOBB.obb().extents().z), false);
            if (EntityUtil.isOnBlockSurface(this, trackLeftPos)) {
                this.level().addParticle(AllParticleTypes.TRACK.get(), true,
                        trackLeftPos.x, trackLeftPos.y, trackLeftPos.z,  0.1f, this.getYRot(), 0
                );
            }
            if (EntityUtil.isOnBlockSurface(this, trackRightPos)) {
                this.level().addParticle(AllParticleTypes.TRACK.get(), true,
                        trackRightPos.x, trackRightPos.y, trackRightPos.z,  0.1f, this.getYRot(), 0
                );
            }
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
        float turnAngle = entityData.get(TURN_ANGLE);
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
        entityData.set(TURN_ANGLE, turnAngle);
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

        // 速度与转向角度产生转向力，并产生车头偏转
        float turnStep = (float) Math.toDegrees(Math.atan2(motion * turnAngle / maxTurn, mainCubeOBB.depth));
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
                float k2 = (float) (Math.cos(Math.min(Math.PI / 2, Math.toRadians(angle * 4))) * 0.9f + 0.1f);
                float k3 = handbrake ? (float) 0.01 : 1;
                float k4 = k1 * k2 * k3;
                if (k4 < 0.003) {
                    loseTraction = true;
                    regainTractionTick = 10;
                }
                float f = (float) (Math.min(1, 10 * k4 * (loseTraction ? 0.5 : 1)) * turnLength);
                if (f != 0) {
                    velocity = velocity.add(turnVector.normalize().scale(f));
                    velocity = velocity.normalize().scale(Math.max(0, motion - 0.001 * angle / 90));
                }
            } else if (regainTractionTick > 0) {
                regainTractionTick -= 1;
                if (regainTractionTick == 0) {
                    loseTraction = false;
                }
            }
        }

        // 轴向速度
        motion = velocity.length();
        angle = (float) Math.toDegrees(VectorUtil.angleBetween(velocity, vehicleDirection));
        entityData.set(FORWARD_SPEED, (angle < 90 ? 1 : -1) * (float) motion);

        this.setDeltaMovement(new Vec3(velocity.x, velocity.y + gVelocity, velocity.z));
        return Vec3.ZERO;
    }

    public float getSpeed() {
        return (float) getDeltaMovement().length();
    }

}
