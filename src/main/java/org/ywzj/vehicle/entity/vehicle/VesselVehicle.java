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
import org.ywzj.vehicle.client.render.animation.context.VesselVehicleContext;
import org.ywzj.vehicle.client.resource.vehicle.VehicleDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VesselVehicleDisplay;
import org.ywzj.vehicle.util.PhysicsHelper;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VesselVehicle extends AbstractVehicle implements IAnimationEntity<VesselVehicle, VesselVehicleContext> {

    public static final EntityDataAccessor<Float> FORWARD_SPEED = SynchedEntityData.defineId(VesselVehicle.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN_ANGLE = SynchedEntityData.defineId(VesselVehicle.class, EntityDataSerializers.FLOAT);
    public float brakeForce = 10000f;
    public float forwardForce = 4000f;
    public float backwardForce = 4000f;
    public float maxSpeedForward = 0.5f;
    public float maxSpeedBackward = 0.2f;
    public float turnStep = 0.1f;
    public float maxTurn = 2f;
    public float turnAngle;
    public float turnAngleO;
    private VehicleSound engineIdleSoundInstance;
    private VehicleSound engineRunSoundInstance;
    private IAnimationInstance<VesselVehicleContext> animationInstance;

    public VesselVehicle(EntityType<? extends AbstractVehicle> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public IAnimationInstance<VesselVehicleContext> getAnimationInstance() {
        return animationInstance;
    }

    @Override
    public void initDisplayData(VehicleDisplay<?, ?> display) {
        super.initDisplayData(display);
        if (display instanceof VesselVehicleDisplay vesselDisplay) {
            animationInstance = vesselDisplay.createAnimationInstance(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FORWARD_SPEED, 0f);
        entityData.define(TURN_ANGLE, 0f);
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
            turnAngleO = turnAngle;
            turnAngle = getTurnAngle();
        }
    }

    @Override
    protected Vec3 tickMove() {
        if (getDriver() == null) {
            controlUnit.reset();
        }

        boolean onGround = level().getBlockState(blockPosition().below()).isSolid();
        Vec3 velocity = getDeltaMovement();
        Vec3 direction = new Vec3(axes()[2]).normalize();
        Vec3 horizontalDirection = new Vec3(direction.x, 0, direction.z).normalize();
        Vec3 horizontalVelocity = new Vec3(velocity.x, 0, velocity.z);
        double forwardSpeed = velocity.dot(direction);
        double motion = horizontalVelocity.length();

        float rudder = getTurnAngle();
        if (controlUnit.left || controlUnit.right) {
            rudder = Mth.clamp(rudder + (controlUnit.right ? turnStep : -turnStep), -maxTurn, maxTurn);
        } else if (rudder != 0) {
            rudder -= Math.signum(rudder) * Math.min(Math.abs(rudder), turnStep);
        }
        setTurnAngle(rudder);

        if (isInWater()) {
            double powerScale = getPower() / 100;
            if (controlUnit.forward && forwardSpeed < maxSpeedForward) {
                velocity = velocity.add(direction.scale(PhysicsHelper.accelerationPerTick(forwardForce * powerScale, physicsEngine.physicsInfo.mass)));
            } else if (controlUnit.backward && forwardSpeed > -maxSpeedBackward) {
                velocity = velocity.add(direction.scale(PhysicsHelper.accelerationPerTick(-backwardForce * powerScale, physicsEngine.physicsInfo.mass)));
            }
            if (maxTurn > 0 && mainCubeOBB.depth > 0) {
                float yawStep = (float) Math.toDegrees(Math.atan2(motion * rudder / maxTurn * (forwardSpeed < 0 ? -1 : 1), mainCubeOBB.depth)) * 0.2f;
                yawStep = Mth.clamp(yawStep, -maxTurn, maxTurn);
                setYRot(getYRot() + yawStep);
            }
            Vec3 lateralDirection = new Vec3(-horizontalDirection.z, 0, horizontalDirection.x);
            double lateralSpeed = velocity.dot(lateralDirection);
            velocity = velocity.subtract(lateralDirection.scale(lateralSpeed * 0.01));
        } else if (onGround) {
            velocity = velocity.normalize().scale(Math.max(0, velocity.length() - 0.1));
        }

        entityData.set(FORWARD_SPEED, (float) velocity.dot(direction));
        setDeltaMovement(velocity);
        return Vec3.ZERO;
    }

    @Override
    protected void tickEngineSpeed() {
        super.tickEngineSpeed();
        float engineSpeed = getEngineSpeed();
        if ((controlUnit.forward || controlUnit.backward) && hasPower()) {
            double forwardSpeed = getDeltaMovement().dot(new Vec3(axes()[2]));
            if ((forwardSpeed >= 0 && controlUnit.forward) || (forwardSpeed <= 0 && controlUnit.backward)) {
                setEngineSpeed(Mth.clamp(engineSpeed + 1, 0, 100));
            } else {
                setEngineSpeed(Mth.clamp(engineSpeed - 2, 0, 100));
            }
        } else if (engineSpeed > 60) {
            setEngineSpeed(Mth.clamp(engineSpeed - 2, 0, 100));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {
        super.tickSound();
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
                SoundEvent idleSound = getEngineIdleSound();
                if (idleSound != null) {
                    engineIdleSoundInstance = new VehicleSound(idleSound, 1f, viewInfo.soundDistance, 1f, true, 50, true, true, getId());
                    engineIdleSoundInstance.play();
                }
            }
        } else {
            if (engineIdleSoundInstance != null) {
                engineIdleSoundInstance.stop();
                engineIdleSoundInstance = null;
            }
            if (engineRunSoundInstance == null) {
                SoundEvent runSound = getEngineRunSound();
                if (runSound != null) {
                    engineRunSoundInstance = new VehicleSound(runSound, 1f, viewInfo.soundDistance, 1f, true, 50, true, true, getId());
                    engineRunSoundInstance.play();
                }
            } else {
                engineRunSoundInstance.setPitch((engineSpeed - 60) / 40 * 0.3f + 0.8f);
            }
        }
    }

    public float getForwardSpeed() {
        return entityData.get(FORWARD_SPEED);
    }

    public float getTurnAngle() {
        return entityData.get(TURN_ANGLE);
    }

    public void setTurnAngle(float angle) {
        turnAngle = angle;
        entityData.set(TURN_ANGLE, angle);
    }

}
