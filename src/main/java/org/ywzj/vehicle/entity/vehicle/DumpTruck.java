package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;

public class DumpTruck extends WheeledVehicle {

    private VehicleSound bedTurnSoundInstance;

    public DumpTruck(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.maxSpeedForward = 0.4f;
    }

    @Override
    public void initPartUnits() {
        RotatableUnit dumpTruckBed = new RotatableUnit("dump_truck_bed", 0, this);
        dumpTruckBed.setYRotMin(0);
        dumpTruckBed.setYRotMax(0);
        dumpTruckBed.setXRotMin(-45);
        dumpTruckBed.setXRotMax(0);
        dumpTruckBed.setXRotSpeed((float) 15 / 20);
        dumpTruckBed.setOwnerViewOffset(new Vec3(0.6, 2.5, 3.3));
        dumpTruckBed.setSeatOffset(new Vec3(0.7, 2.7, 3));
        this.partUnits.add(dumpTruckBed);
        this.seats.add(new Seat(0, dumpTruckBed));
        PartUnit passengerSeat = new PartUnit("passenger_seat", 1, this);
        passengerSeat.setOwnerViewOffset(new Vec3(0.6 - 1.4, 2.5, 3.3));
        passengerSeat.setSeatOffset(new Vec3(0.7 - 1.4, 2.7, 3));
        this.partUnits.add(passengerSeat);
        this.seats.add(new Seat(1, passengerSeat));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (getDriver() != null) {
                if (controlUnit.leftYaw || controlUnit.rightYaw) {
                    RotatableUnit bed = (RotatableUnit) seats.get(0).partUnit;
                    if (controlUnit.leftYaw) {
                        bed.xAimRot -= 5;
                    } else {
                        bed.xAimRot += 5;
                    }
                    bed.xAimRot = Mth.clamp(bed.xAimRot, bed.xRotMin, bed.xRotMax);
                    ClientVehicleAction control = new ClientVehicleAction();
                    control.vehicleEntityId = this.getId();
                    control.partUnitIndex = bed.getIndex();
                    control.xAimRot = bed.xAimRot;
                    control.yAimRot = 0;
                    Channel.CHANNEL.sendToServer(control);
                }
            }
        }
    }

    @Override
    protected void tickSound() {
        super.tickSound();
        RotatableUnit bed = (RotatableUnit) seats.get(0).partUnit;
        if (Math.abs(bed.xAimRot - bed.xRot) > 1 && bed.xRot < bed.xRotMax && bed.xRot > bed.xRotMin) {
            if (bedTurnSoundInstance == null) {
                bedTurnSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_V.get(), 1f, 1f, true, 10, true, true, this.getId());
                bedTurnSoundInstance.play();
            }
        } else {
            if (bedTurnSoundInstance != null) {
                bedTurnSoundInstance.stop();
                bedTurnSoundInstance = null;
            }
        }
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return AllSounds.LAV150_ENGINE_START.get();
    }

    @Override
    public SoundEvent getEngineIdleSound() {
        return AllSounds.TRUCK_ENGINE_IDLE.get();
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.TRUCK_ENGINE_RUN.get();
    }

    @Override
    protected void tickParticle() {
        if (hasPower() && tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(2f)).add(v2.scale(-1.6)).add(0, 3, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @Override
    public void support(Entity pEntity) {
        super.support(pEntity);
        // 自动进车斗
        if (pEntity instanceof LivingEntity) {
            PartUnit partUnit = seats.get(0).partUnit;
            Vec3 leftDoorPos = relativeRotPos(position().add(mainCubeOBB.obb().extents().x + 1, 0, partUnit != null ? partUnit.getSeatOffset().z : 0));
            if (pEntity.distanceToSqr(leftDoorPos) < 1) {
                Vec3 bedPos = relativeRotPos(position().add(0, 5, 0));
                pEntity.teleportTo(bedPos.x, bedPos.y, bedPos.z);
            }
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
