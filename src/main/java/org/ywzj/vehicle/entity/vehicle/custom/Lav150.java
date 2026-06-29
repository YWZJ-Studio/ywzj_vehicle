package org.ywzj.vehicle.entity.vehicle.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class Lav150 extends WheeledVehicle {

    public Lav150(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public void initPartUnits() {
        WeaponUnit machineGunTurret = new WeaponUnit("lav150_main_gun_turret", 0, this, new Vec3(0d, 2.5d, 0d), 3.3f, null, null, null, null);
        machineGunTurret.setXRotSpeed(3f);
        machineGunTurret.setYRotSpeed(3f);
        machineGunTurret.setXRotMax(15);
        machineGunTurret.setXRotMin(-30);
        this.partUnits.add(machineGunTurret);
        this.seats.add(new Seat(0, machineGunTurret));
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.8f;
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
        super.tickParticle();
        if (!this.getPassengers().isEmpty() && tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(-2f)).add(v2.scale(-1.2)).add(0, 2, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @Override
    public void shoot(int partUnitIndex, int weaponIndex, List<AimContext> aimContexts, @Nullable LivingEntity operator) {
        if (partUnitIndex == 0) {
            if (seats.get(0).partUnit instanceof WeaponUnit machineGunTurret) {
                machineGunTurret.shoot(weaponIndex, aimContexts, operator);
                this.level().playSound(null, this, AllSounds.AUTO_CANNON_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
            }
        }
    }

}
