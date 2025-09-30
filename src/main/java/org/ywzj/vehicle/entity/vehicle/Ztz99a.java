package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.vehicle.PartUnit;
import org.ywzj.vehicle.vehicle.SpotterUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class Ztz99a extends TrackedVehicle {

    private VehicleSound turretTurnYSoundInstance;
    private VehicleSound turretTurnXSoundInstance;

    public Ztz99a(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void initPartUnits() {
        WeaponUnit turret = new WeaponUnit("ztz99a_turret",
                0,
                this,
                new Vec3(0, 2.54d, -0.375d),
                8f,
                new Vec3(0, 1.5d, 0),
                new Vec3(0, 0, 0),
                null);
        turret.xRotSpeed = 30f / 20;
        turret.yRotSpeed = 15f / 20;
        turret.xRotMax = 5f;
        turret.xRotMin = -13f;
        this.partUnits.add(turret);
        this.operatorUnits.add(turret);
        WeaponUnit commanderMachineGun = new WeaponUnit("ztz99a_commander_machine_gun",
                1,
                this,
                new Vec3(-0.594d, 3.325d, 0.101d),
                1f,
                new Vec3(0.5d, 0.5d, -1d),
                new Vec3(0, -1d, 1.2d),
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
                new Vec3(0, -2.2d, -1.2d),
                null);
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return AllSounds.ZTZ99A_ENGINE_START.get();
    }

    @Override
    public SoundEvent getEngineIdleSound() {
        return AllSounds.ZTZ99A_ENGINE_IDLE.get();
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.ZTZ99A_ENGINE_RUN.get();
    }

    @Override
    protected void tickParticle() {
        double velocity = Math.abs(entityData.get(FORWARD_SPEED)) * 20 + Math.abs(entityData.get(TURN_SPEED)) * 5;
        if (!this.getPassengers().isEmpty() && velocity > 0 || tickCount % 10 == 0) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePosLeft = this.position().add(this.getLookAngle().normalize().scale(-3.7f)).add(v2.scale(-0.6)).add(0, 2, 0);
            Vec3 engineSmokePosRight = this.position().add(this.getLookAngle().normalize().scale(-3.7f)).add(v2.scale(0.6)).add(0, 2, 0);
            for (int count = 0; count < velocity / 8 + 1; count++) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosLeft.x, engineSmokePosLeft.y, engineSmokePosLeft.z, 0, 0, 0);
                level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosRight.x, engineSmokePosRight.y, engineSmokePosRight.z, 0, 0, 0);
            }
        }
    }

    @Override
    protected void tickSound() {
        super.tickSound();
        for (PartUnit operatorUnit : operatorUnits) {
            if (operatorUnit.getName().getString().equals("ztz99a_turret")) {
                if (Math.abs(operatorUnit.yAimRot - operatorUnit.yRot) > 1) {
                    if (turretTurnYSoundInstance == null) {
                        turretTurnYSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_H.get(), 1f, 1f, true, 10, true, true, this.getId());
                        turretTurnYSoundInstance.play();
                    }
                } else {
                    if (turretTurnYSoundInstance != null) {
                        turretTurnYSoundInstance.stop();
                        turretTurnYSoundInstance = null;
                    }
                }
                if (Math.abs(operatorUnit.xAimRot - operatorUnit.xRot) > 1 && operatorUnit.xRot < operatorUnit.xRotMax && operatorUnit.xRot > operatorUnit.xRotMin) {
                    if (turretTurnXSoundInstance == null) {
                        turretTurnXSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_V.get(), 1f, 1f, true, 10, true, true, this.getId());
                        turretTurnXSoundInstance.play();
                    }
                } else {
                    if (turretTurnXSoundInstance != null) {
                        turretTurnXSoundInstance.stop();
                        turretTurnXSoundInstance = null;
                    }
                }
            }
        }
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (weaponIndex < operatorUnits.size()) {
            //todo 武器配置
            if (operatorUnits.get(weaponIndex) instanceof WeaponUnit weaponUnit) {
                if (weaponUnit.getName().getString().equals("ztz99a_turret")) {
                    weaponUnit.shoot(ammoSpawnPosition, ammoXRot, ammoYRot, true);
                    this.level().playSound(null, this, AllSounds.CANNON_125_MM_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                        } catch (Exception ex) {}
                        ServerLifecycleHooks.getCurrentServer().execute(() -> this.level().playSound(null, this, AllSounds.CANNON_SHELL_DROP.get(), SoundSource.PLAYERS, 16f, 1f));
                    }).start();
                } else {
                    weaponUnit.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
                    this.level().playSound(null, this, AllSounds.LAV150_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);
                }
            }
        }
    }

}
