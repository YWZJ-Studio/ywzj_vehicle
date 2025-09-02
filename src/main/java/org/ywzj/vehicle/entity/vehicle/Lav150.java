package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class Lav150 extends WheeledVehicle {

    public Lav150(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.wide = 2.7f;
        this.length = 3.61f;
    }

    @Override
    public int getSeats() {
        return 1;
    }

    @Override
    public void initWeaponUnits() {
        WeaponUnit machineGunTurret = new WeaponUnit("lav150_main_gun_turret", 0, this, new Vec3(0d, 2.5d, 0d), 3.3f, null, null, null);
        machineGunTurret.xRotSpeed = 3f;
        machineGunTurret.yRotSpeed = 3f;
        machineGunTurret.xRotMax = 15;
        machineGunTurret.xRotMin = -30;
        this.weaponUnits.add(machineGunTurret);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.8f;
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
    protected void tickParticle() {
        if (!this.getPassengers().isEmpty() && tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(-2f)).add(v2.scale(-1.2)).add(0, 2, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (weaponIndex == 0) {
            WeaponUnit machineGunTurret = weaponUnits.get(0);
            machineGunTurret.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
            this.level().playSound(null, this, AllSounds.LAV_150_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);
        }
    }

}
