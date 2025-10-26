package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.VehicleDataManager;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import java.util.ArrayList;
import java.util.List;

public class Ztz99a extends TrackedVehicle {

    public Ztz99a(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void initData() {
        VehicleDataManager.get().getVehicleData(YwzjVehicle.modLoc("ztz99a")).ifPresent(data -> {
            var struct = data.getVehicleStructObbs();
            this.mainCubeOBB = struct.mainCubeOBB();
            this.vehicleOBBs = struct.obbs();
            var weapons = data.createPartUnits(this);
//            this.operatorUnits.addAll(weapons.values());
            List<WeaponUnit> weaponUnits = new ArrayList<>(weapons.values());
            for (int index = 0; index < weapons.size(); index++) {
                this.seats.add(new Seat(index, weaponUnits.get(index)));
            }
            this.partUnits.addAll(weapons.values());
        });
    }

    @Deprecated
    @Override
    public void initPartUnits() {
        WeaponUnit turret = new WeaponUnit("ztz99a_turret",
                0,
                this,
                new Vec3(0, 2.54d, -0.375d),
                8f,
                new Vec3(1, 0, 1),
                new Vec3(0, 1.5d, 0),
                new Vec3(0, 0, 0),
                null);
        turret.setXRotSpeed(30f / 20);
        turret.setYRotSpeed(15f / 20);
        turret.setXRotMax(5f);
        turret.setXRotMin(-13f);
        this.partUnits.add(turret);
        this.seats.add(new Seat(0, turret));
        WeaponUnit commanderMachineGun = new WeaponUnit("ztz99a_commander_machine_gun",
                1,
                this,
                new Vec3(-0.594d, 3.325d, 0.101d),
                1f,
                null,
                new Vec3(0.5d, 0.5d, -1d),
                new Vec3(0, -1d, 1.2d),
                turret);
        commanderMachineGun.setXRotSpeed(60f / 20);
        commanderMachineGun.setYRotSpeed(60f / 20);
        commanderMachineGun.setXRotMax(15f);
        commanderMachineGun.setXRotMin(-18f);
        this.partUnits.add(commanderMachineGun);
        this.seats.add(new Seat(1, commanderMachineGun));
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
        if ((!this.getPassengers().isEmpty() && velocity > 0 || tickCount % 10 == 0) && hasPower()) {
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
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (weaponIndex < seats.size()) {
            //todo 武器配置
            if (seats.get(weaponIndex).partUnit instanceof WeaponUnit weaponUnit) {
                if (weaponUnit.getName().getString().equals("ztz99a_turret")) {

                    // 测试主炮
                    weaponUnit.shoot(ammoSpawnPosition, ammoXRot, ammoYRot, true);
                    this.level().playSound(null, this, AllSounds.CANNON_125_MM_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                        } catch (Exception ex) {}
                        ServerLifecycleHooks.getCurrentServer().execute(() -> this.level().playSound(null, this, AllSounds.CANNON_SHELL_DROP.get(), SoundSource.PLAYERS, 16f, 1f));
                    }).start();
                    // 后坐
                    physicsEngine.recoil(weaponUnit);


                } else {
                    weaponUnit.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
                    this.level().playSound(null, this, AllSounds.AUTO_CANNON_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
                }
            }
        }
    }

}
