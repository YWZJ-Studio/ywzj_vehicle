package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.ywzj.vehicle.all.AllParticleTypes;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.particle.DustSmokeOption;
import org.ywzj.vehicle.vehicle.PassengerPose;
import org.ywzj.vehicle.vehicle.SpotterUnit;

public class Motorcycle extends WheeledVehicle {

    public Motorcycle(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        groundFrictionAcceleration = 0.003f;
        forwardAcceleration = 0.01f + groundFrictionAcceleration;
        backwardAcceleration = 0.01f + groundFrictionAcceleration;
        maxSpeedForward = 0.8f;
        maxSpeedBackward= 0.1f;
        turnAcceleration = 1f;
        maxTurn = 3f;
        physicsEngine.lockZRot = true;
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return AllSounds.LAV_150_ENGINE_START.get();
    }

    @Override
    public SoundEvent getEngineIdleSound() {
        return AllSounds.LAV_150_ENGINE_IDLE.get();
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.LAV_150_ENGINE_RUN.get();
    }

    @Override
    public void initPartUnits() {
        this.spotterUnit = new SpotterUnit(this,
                new Vec3(0, 2, 0),
                new Vec3(0, -0.3f, 0),
                new Vec3(0, 0, -0.3),
                null);
        this.spotterUnit.operatorPose = new PassengerPose();
        this.spotterUnit.operatorPose.leftArmRotX = -1.5f;
        this.spotterUnit.operatorPose.rightArmRotX= -1.5f;
    }

    @Override
    protected void tickParticle() {
        double speedSqr = this.getDeltaMovement().lengthSqr();
        if (speedSqr <= 0.255) return;

        int interval;
        if (speedSqr < 0.64) {
            interval = 4;
        } else if (speedSqr < 0.81) {
            interval = 3;
        } else if (speedSqr < 1) {
            interval = 2;
        } else {
            interval = 1;
        }
        if (tickCount % interval != 0) return;

        var matrix4f = this.getWheelsTransform(1f);
        spawnDustParticles(0.8f, matrix4f);
        spawnDustParticles(-0.8f, matrix4f);
    }

    private void spawnDustParticles(float offset, Matrix4f matrix4f) {
        var wheel = this.transformPosition(matrix4f, 0, 0, offset);
        this.level().addParticle(new DustSmokeOption(2.5f),
                wheel.x() - this.getDeltaMovement().x,
                wheel.y() + 0.1,
                wheel.z() - this.getDeltaMovement().z,
                this.getDeltaMovement().x * 0.1,
                0.02,
                this.getDeltaMovement().z * 0.1);
        double dx = (offset > 0 ? 0.1 : 0.25);
        double dy = (offset > 0 ? 0.02 : 0.1);
        if (this.random.nextFloat() < 0.5f) return;
        this.level().addParticle(AllParticleTypes.DUST_STONE.get(),
                wheel.x() + this.random.nextDouble() * 0.1 - this.getDeltaMovement().x,
                wheel.y() + 0.1,
                wheel.z() + this.random.nextDouble() * 0.1 - this.getDeltaMovement().z,
                this.getDeltaMovement().x * dx + this.random.nextDouble() * 0.1,
                dy,
                this.getDeltaMovement().z * dx + this.random.nextDouble() * 0.1
        );
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        // 喇叭声
    }

}
