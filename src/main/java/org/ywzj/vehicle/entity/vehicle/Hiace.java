package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.vehicle.PartUnit;

public class Hiace extends WheeledVehicle {

    public Hiace(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.thirdPersonOffset = new Vec3(0, 4, -7);
        physicsEngine.friction = 0.003f;
        forwardAcceleration = 0.013f;
        backwardAcceleration = 0.013f;
        maxSpeedForward = 0.6f;
        maxSpeedBackward= 0.2f;
        turnAcceleration = 1f;
        maxTurn = 3f;
    }

    @Override
    public void initPartUnits() {
        PartUnit passengerSeat0 = new PartUnit("passenger_seat0", 0, this);
        passengerSeat0.setOwnerViewOffset(new Vec3(0.5,0.2, 1.5));
        passengerSeat0.setSeatOffset(new Vec3(0.5,0.2, 1.5));
        this.partUnits.add(passengerSeat0);
        this.seats.add(new Seat(0, passengerSeat0));

        PartUnit passengerSeat1 = new PartUnit("passenger_seat1", 0, this);
        passengerSeat1.setOwnerViewOffset(new Vec3(0.5 - 1,0.2, 1.5));
        passengerSeat1.setSeatOffset(new Vec3(0.5 - 1,0.2, 1.5));
        this.partUnits.add(passengerSeat1);
        this.seats.add(new Seat(1, passengerSeat1));

        PartUnit passengerSeat2 = new PartUnit("passenger_seat2", 0, this);
        passengerSeat2.setOwnerViewOffset(new Vec3(0.5 - 0.3,0.3, 1.5 - 1.6));
        passengerSeat2.setSeatOffset(new Vec3(0.5 - 0.3,0.3, 1.5 - 1.6));
        this.partUnits.add(passengerSeat2);
        this.seats.add(new Seat(2, passengerSeat2));

        PartUnit passengerSeat3 = new PartUnit("passenger_seat3", 0, this);
        passengerSeat3.setOwnerViewOffset(new Vec3(0.5 - 1,0.3, 1.5 - 1.6));
        passengerSeat3.setSeatOffset(new Vec3(0.5 - 1,0.3, 1.5 - 1.6));
        this.partUnits.add(passengerSeat3);
        this.seats.add(new Seat(3, passengerSeat3));

        PartUnit passengerSeat4 = new PartUnit("passenger_seat4", 0, this);
        passengerSeat4.setOwnerViewOffset(new Vec3(0.5 - 0.3,0.3, 1.5 - 2.8));
        passengerSeat4.setSeatOffset(new Vec3(0.5 - 0.3,0.3, 1.5 - 2.8));
        this.partUnits.add(passengerSeat4);
        this.seats.add(new Seat(4, passengerSeat4));

        PartUnit passengerSeat5 = new PartUnit("passenger_seat5", 0, this);
        passengerSeat5.setOwnerViewOffset(new Vec3(0.5 - 1,0.3, 1.5 - 2.8));
        passengerSeat5.setSeatOffset(new Vec3(0.5 - 1,0.3, 1.5 - 2.8));
        this.partUnits.add(passengerSeat5);
        this.seats.add(new Seat(5, passengerSeat5));
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return AllSounds.LAV150_ENGINE_START.get();
    }

    @Override
    public SoundEvent getEngineIdleSound() {
        return AllSounds.LAV150_ENGINE_IDLE.get();
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.LAV150_ENGINE_RUN.get();
    }

    @Override
    protected void tickParticle() {
        if (hasPower() && tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(-3f)).add(v2.scale(-0.5)).add(0, 0.5, 0);
            level().addParticle(ParticleTypes.SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
//        if (weaponIndex < operatorUnits.size()) {
//            if (seats.get(weaponIndex) instanceof WeaponUnit machineGunTurret) {
//                machineGunTurret.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
//                this.level().playSound(null, this, AllSounds.LAV150_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);
//            }
//        }
    }

}
