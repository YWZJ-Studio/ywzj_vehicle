package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.audio.VehicleSound;

public abstract class WheeledVehicle extends AbstractVehicle {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(WheeledVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_SPEED = SynchedEntityData.defineId(WheeledVehicle.class, EntityDataSerializers.FLOAT);
    public static float groundFrictionAcceleration = 0.005f;
    public static float forwardAcceleration = 0.005f + groundFrictionAcceleration;
    public static float backwardAcceleration = 0.005f + groundFrictionAcceleration;
    public static float maxSpeed = 0.5f;
    public static float turnAcceleration = 0.1f;
    public static float maxTurn = 2f;
    public float wheelRotation;
    public long lastRenderTime;
    private VehicleSound engineIdleSoundInstance;
    private VehicleSound engineRunSoundInstance;

    public WheeledVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
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
    public void onEnterVehicle(Player pPlayer) {
        super.onEnterVehicle(pPlayer);
        if (passengerIdsBySeat.size() == 1) {
            level().playSound(null, this.blockPosition(), getEngineStartSound(), SoundSource.HOSTILE);
        }
        level().playSound(null, this.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE);
    }

    @Override
    public void onLeaveVehicle(LivingEntity entity) {
        super.onLeaveVehicle(entity);
        level().playSound(null, this.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.HOSTILE);
    }

    @Override
    protected void tickSound() {
        if (getPassengers().isEmpty()) {
            if (engineIdleSoundInstance != null) {
                engineIdleSoundInstance.stop();
                engineIdleSoundInstance = null;
            }
            if (engineRunSoundInstance != null) {
                engineRunSoundInstance.stop();
                engineRunSoundInstance = null;
            }
        } else {
            float vf = entityData.get(FORWARD_SPEED);
            if (vf == 0) {
                if (engineRunSoundInstance != null) {
                    engineRunSoundInstance.stop();
                    engineRunSoundInstance = null;
                }
                if (engineIdleSoundInstance == null) {
                    engineIdleSoundInstance = new VehicleSound(getEngineIdleSound(), 1f, 1f, true, true, this.getId());
                    engineIdleSoundInstance.play();
                }
            } else {
                if (engineIdleSoundInstance != null) {
                    engineIdleSoundInstance.stop();
                    engineIdleSoundInstance = null;
                }
                float volume = Math.max(0.4f, vf / maxSpeed);
                if (engineRunSoundInstance == null) {
                    engineRunSoundInstance = new VehicleSound(getEngineRunSound(), volume, 1f, true, false, this.getId());
                    engineRunSoundInstance.play();
                } else {
                    engineRunSoundInstance.setVolume(volume);
                }
            }
        }
    }

    @Override
    protected void tickMove() {
        if (getDriver() == null) {
            controlUnit.reset();
        }
        float vf = entityData.get(FORWARD_SPEED);
        float vt = entityData.get(TURN_SPEED);
        // 考虑碰撞停滞
        if (new Vec3(getDeltaMovement().x, 0, getDeltaMovement().z).length() == 0) {
            vf = 0;
        }
        // 前后控制
        if (controlUnit.forward || controlUnit.backward) {
            vf += controlUnit.forward ? forwardAcceleration : -backwardAcceleration;
            vf = Mth.clamp(vf, -maxSpeed, maxSpeed);
        }
        // 地面摩擦力
        if (vf < 0) {
            vf += groundFrictionAcceleration;
            vf = Math.min(vf, 0);
        } else if (vf > 0) {
            vf -= groundFrictionAcceleration;
            vf = Math.max(vf, 0);
        }
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
        // 轮式载具仅存在前进速度时可运动转向
        if (Math.abs(vf) > 0.03) {
            if (vf < 0) {
                vt *= -1;
            }
            this.setYRot(this.getYRot() + vt);
        }
        // 前进速度应用于车身朝向
        Vec3 direction = getLookAngle();
        Vec3 motion = direction.normalize().scale(vf);
        // 重力影响
        motion = motion.add(0, -1f, 0);
        this.setDeltaMovement(motion);
    }

}
