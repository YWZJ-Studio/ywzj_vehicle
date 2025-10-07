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
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.client.event.InputHandler;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public abstract class WheeledVehicle extends AbstractVehicle {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(WheeledVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_SPEED = SynchedEntityData.defineId(WheeledVehicle.class, EntityDataSerializers.FLOAT);
    public static float brakeAcceleration = 0.025f;
    public static float forwardAcceleration = 0.01f;
    public static float backwardAcceleration = 0.01f;
    public static float maxSpeedForward = 0.5f;
    public static float maxSpeedBackward = 0.2f;
    public static float turnAcceleration = 0.1f;
    public static float maxTurn = 2f;
    public float wheelRotation;
    public long lastRenderTime;
    private VehicleSound engineIdleSoundInstance;
    private VehicleSound engineRunSoundInstance;

    public WheeledVehicle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public int getSeats() {
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
    protected void tickAim() {
        if (InputHandler.freeCamera) {
            return;
        }
        if (getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
            Vec2 rot = null;
            if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.DEFAULT) {
                rot = LocalVehiclePlayer.instance.cameraToWeaponRot();
            } else if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                rot = LocalVehiclePlayer.instance.scopeAimRot();
            }
            if (rot == null) {
                return;
            }
            if (weaponUnit.xAimRot != rot.x || weaponUnit.yAimRot != rot.y) {
                weaponUnit.xAimRot = rot.x;
                weaponUnit.yAimRot = rot.y;
                ClientVehicleAction control = new ClientVehicleAction();
                control.vehicleEntityId = this.getId();
                control.weaponIndex = weaponUnit.getIndex();
                control.xAimRot = rot.x;
                control.yAimRot = rot.y;
                Channel.CHANNEL.sendToServer(control);
            }
        }
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
                if (!hasPower()) {
                    if (engineIdleSoundInstance != null) {
                        engineIdleSoundInstance.stop();
                        engineIdleSoundInstance = null;
                    }
                } else if (engineIdleSoundInstance == null) {
                    engineIdleSoundInstance = new VehicleSound(getEngineIdleSound(), soundDistance, 1f, true, 50, true, true, this.getId());
                    engineIdleSoundInstance.play();
                }
            } else {
                if (engineIdleSoundInstance != null) {
                    engineIdleSoundInstance.stop();
                    engineIdleSoundInstance = null;
                }
                float volume = Math.max(0.4f, vf / maxSpeedForward);
                if (engineRunSoundInstance == null) {
                    engineRunSoundInstance = new VehicleSound(getEngineRunSound(), volume * soundDistance, 1f, true, 50, false, true, this.getId());
                    engineRunSoundInstance.play();
                } else {
                    engineRunSoundInstance.setVolume(volume);
                }
            }
        }
    }

    @Override
    protected Vec3 tickMove() {
        if (getDriver() == null) {
            controlUnit.reset();
        } else {
            float power = getPower();
            if (getFuel() == 0) {
                setPower(0);
            } else if (power < 100) {
                if (power == 0) {
                    playVehicleSound(getEngineStartSound(), true);
                }
                setPower(power + 1);
            }
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
        // 轮式载具仅存在前进速度时可运动转向
        if (Math.abs(vf) > 0.03) {
            if (controlUnit.backward) {
                vt *= -1;
            }
            this.setYRot(this.getYRot() + vt);
        }
        // 前进速度应用于车身朝向
        Vec3 direction = getLookAngle();
        Vec3 motion = direction.normalize().scale(vf);
        this.setDeltaMovement(motion);
        return new Vec3(0, 0, 0);
    }

}
