package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
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
    public void initWeaponUnits() {
        this.spotterUnit = new SpotterUnit(this,
                new Vec3(0, 2, 0),
                new Vec3(0, -0.3f, 0),
                new Vec3(0, 0, -0.3),
                null);
        this.spotterUnit.passengerPose = new PassengerPose();
        this.spotterUnit.passengerPose.leftArmRotX = -1.5f;
        this.spotterUnit.passengerPose.rightArmRotX= -1.5f;
    }

    @Override
    protected void tickParticle() {

    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        // 喇叭声
    }

}
