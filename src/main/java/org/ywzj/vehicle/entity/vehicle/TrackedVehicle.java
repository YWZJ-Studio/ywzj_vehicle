package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.audio.VehicleSound;

public abstract class TrackedVehicle extends AbstractVehicle {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(TrackedVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_SPEED = SynchedEntityData.defineId(TrackedVehicle.class, EntityDataSerializers.FLOAT);
    public float brakeAcceleration = 0.025f;
    public float forwardAcceleration = 0.01f;
    public float backwardAcceleration = 0.01f;
    public float maxSpeedForward = 0.5f;
    public float maxSpeedBackward = 0.2f;
    public float turnAcceleration = 1f;
    public float maxTurn = 2f;
    public long lastRenderTime;
    private VehicleSound engineIdleSoundInstance;
    private VehicleSound engineRunSoundInstance;

    public TrackedVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.thirdPersonCenterOffset = new Vec3(0, 3, 0);
        this.thirdPersonDistance = 7;
    }

    @Override
    public int passengerCapacity() {
        return 3;
    }

    public abstract SoundEvent getEngineStartSound();

    public abstract SoundEvent getEngineIdleSound();

    public abstract SoundEvent getEngineRunSound();

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FORWARD_SPEED, 0f);
        this.entityData.define(TURN_SPEED, 0f);
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
            if (getFuel() != 0 && getPower() == 5) {
                SoundEvent engineStartSound = getEngineStartSound();
                if (engineStartSound != null) {
                    new VehicleSound(engineStartSound, soundDistance, 1f, false, 50, true, true, this.getId()).play();
                }
            }
            float vf = entityData.get(FORWARD_SPEED);
            float vt = entityData.get(TURN_SPEED);
            if (vf == 0 && vt == 0) {
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
                float volume = Math.max(0.4f, vf != 0 ? Math.abs(vf) / maxSpeedForward : 0.7f);
                if (engineRunSoundInstance == null) {
                    SoundEvent engineRunSound = getEngineRunSound();
                    if (engineRunSound != null) {
                        engineRunSoundInstance = new VehicleSound(engineRunSound, volume * soundDistance, 1f, true, 50, true, true, this.getId());
                        engineRunSoundInstance.play();
                    }
                } else {
                    engineRunSoundInstance.setVolume(volume * soundDistance);
                }
            }
        }
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
        // 转向幅度应用于车身朝向
        if (controlUnit.backward) {
            vt *= -1;
        }
        this.setYRot(this.getYRot() + vt);
        // 前进速度应用于车身朝向
        Vec3 direction = getLookAngle();
        Vec3 motion = direction.normalize().scale(vf);
        motion = motion.add(0, Math.min(0, getDeltaMovement().y), 0);
        this.setDeltaMovement(motion);
        return new Vec3(0, 0, 0);
    }

}
