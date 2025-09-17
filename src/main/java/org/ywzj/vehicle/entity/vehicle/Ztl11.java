package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.vehicle.SpotterUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class Ztl11 extends WheeledVehicle {

    public Ztl11(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void initPartUnits() {
        WeaponUnit turret = new WeaponUnit("ztl11_turret",
                0,
                this,
                new Vec3(0, 2.54d, -0.375d),
                8f,
                new Vec3(0, 1.5d, 0),
                new Vec3(0, 0, 0),
                null);
        turret.xRotSpeed = 22.1f / 20;
        turret.yRotSpeed = 15.5f / 20;
        turret.xRotMax = 5f;
        turret.xRotMin = -18f;
        this.partUnits.add(turret);
        this.operatorUnits.add(turret);
        WeaponUnit commanderMachineGun = new WeaponUnit("ztl11_commander_machine_gun",
                1,
                this,
                new Vec3(-0.594d, 3.325d, 0.101d),
                1f,
                new Vec3(0.5d, 0.5d, -1d),
                new Vec3(0, 0.3d, -0.4d),
                turret);
        commanderMachineGun.xRotSpeed = 60f / 20;
        commanderMachineGun.yRotSpeed = 60f / 20;
        commanderMachineGun.xRotMax = 15f;
        commanderMachineGun.xRotMin = -18f;
        this.partUnits.add(commanderMachineGun);
        this.operatorUnits.add(commanderMachineGun);
        this.spotterUnit = new SpotterUnit(this,
                new Vec3(0, 4.54d, -0.375d),
                new Vec3(0, 1.5d, 0),
                new Vec3(0, 0, 0),
                null);
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
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(-2f)).add(v2.scale(-1.6)).add(0, 2, 0);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePos.x, engineSmokePos.y, engineSmokePos.z, 0, 0, 0);
        }
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (weaponIndex < operatorUnits.size()) {
            if (operatorUnits.get(weaponIndex) instanceof WeaponUnit machineGunTurret) {
                machineGunTurret.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
                this.level().playSound(null, this, AllSounds.LAV_150_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);
            }
        }
    }

}
