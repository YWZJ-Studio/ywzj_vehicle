package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;

import java.util.List;

public class Ztz99a extends TrackedVehicle {

    public Ztz99a(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

//    @Override
//    public void initData() {
//        VehicleDataManager.get().getVehicleData(YwzjVehicle.modLoc("ztz99a")).ifPresent(data -> {
//            var struct = data.getVehicleStructObbs();
//            this.mainCubeOBB = struct.mainCubeOBB();
//            this.vehicleOBBs = struct.obbs();
//            var weapons = data.createPartUnits(this);
//            List<WeaponUnit> weaponUnits = new ArrayList<>(weapons.values());
//            for (int index = 0; index < weapons.size(); index++) {
//                this.seats.add(new Seat(index, weaponUnits.get(index)));
//            }
//            this.partUnits.addAll(weapons.values());
//        });
//    }

    @Deprecated
    @Override
    public void initPartUnits() {
        WeaponUnit turret = new WeaponUnit("turret",
                0,
                this,
                new Vec3(0, 1.875, 0.5),
                8.233f,
                new Vec3(0.75, 0.5, 0.5),
                new Vec3(0, 1.5d, 0),
                new Vec3(0, 2, 0),
                null);
        turret.crosshairStyle = WeaponUnit.CrosshairStyle.CIRCLE;
        turret.setXRotSpeed(30f / 20);
        turret.setYRotSpeed(15f / 20);
        turret.setXRotMax(5f);
        turret.setXRotMin(-13f);
        turret.currentWeaponIndexHolder = turret.getSyncData().define(
                SyncDataSerializers.INT,
                turret::setCurrentWeaponIndex,
                turret::getCurrentWeaponIndex,
                0
        );
        VehicleCannonWeaponData weaponDataCannon = new VehicleCannonWeaponData();
        weaponDataCannon.setName("cannon");
        weaponDataCannon.setMaxCapacity(1);
        weaponDataCannon.setExplosion(true);
        weaponDataCannon.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_ARTILLERY.get())));
        VehicleCannon vehicleCannon = new VehicleCannon(this, turret, 0, weaponDataCannon);
        vehicleCannon.defineSyncData(turret.getSyncData());
        turret.weapons.add(vehicleCannon);
        this.partUnits.add(turret);
        this.seats.add(new Seat(0, turret));
        WeaponUnit commanderMachineGun = new WeaponUnit("commander_machine_gun",
                1,
                this,
                new Vec3(-0.736d, 3.056d, -0.595),
                1.7f,
                new Vec3(-1d, 0d, -1),
                new Vec3(0.5d, 0.5d, -1d),
                new Vec3(0, 1d, 1.2d),
                turret);
        commanderMachineGun.crosshairStyle = WeaponUnit.CrosshairStyle.CIRCLE;
        commanderMachineGun.opticalSightType = WeaponUnit.OpticalSightType.OPERATOR;
        commanderMachineGun.setXRotSpeed(60f / 20);
        commanderMachineGun.setYRotSpeed(60f / 20);
        commanderMachineGun.setXRotMax(15f);
        commanderMachineGun.setXRotMin(-18f);
        VehicleCannonWeaponData weaponDataMachineGun = new VehicleCannonWeaponData();
        weaponDataMachineGun.setName("machine_gun");
        weaponDataMachineGun.setMaxCapacity(120);
        weaponDataMachineGun.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_MACHINE_GUN.get())));
        VehicleCannon vehicleMachineGun = new VehicleCannon(this, commanderMachineGun, 1, weaponDataMachineGun);
        vehicleMachineGun.defineSyncData(commanderMachineGun.getSyncData());
        commanderMachineGun.weapons.add(vehicleMachineGun);
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
            Vec3 engineSmokePosLeft = this.position().add(this.getLookAngle().normalize().scale(-2.5f)).add(v2.scale(-2)).add(0, 1.7, 0);
            Vec3 engineSmokePosRight = this.position().add(this.getLookAngle().normalize().scale(-2.5f)).add(v2.scale(2)).add(0, 1.7, 0);
            for (int count = 0; count < velocity / 16 + 1; count++) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosLeft.x, engineSmokePosLeft.y, engineSmokePosLeft.z, 0, 0, 0);
                level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosRight.x, engineSmokePosRight.y, engineSmokePosRight.z, 0, 0, 0);
            }
        }
    }

    @Override
    public void shoot(int weaponIndex, List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot) {
        if (partUnits.get(weaponIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(ammoSpawnPositions, ammoXRot, ammoYRot);
            if (weaponIndex == 0) {
                // todo: 测试音效
                this.level().playSound(null, this, AllSounds.CANNON_125_MM_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ex) {}
                    ServerLifecycleHooks.getCurrentServer().execute(() -> this.level().playSound(null, this, AllSounds.CANNON_SHELL_DROP.get(), SoundSource.PLAYERS, 16f, 1f));
                }).start();
                // todo: 后坐
                physicsEngine.recoil(weaponUnit);
                // todo: 测试粒子
                Vec3 muzzlePos = ammoSpawnPositions.get(0);
                if (this.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 20; i++) {
                        double dx = (serverLevel.random.nextDouble() - 0.5) * 0.4;
                        double dy = (serverLevel.random.nextDouble() - 0.5) * 0.2;
                        double dz = (serverLevel.random.nextDouble() - 0.5) * 0.4;
                        serverLevel.sendParticles(
                                ParticleTypes.CAMPFIRE_COSY_SMOKE, // 可换成自定义粒子
                                muzzlePos.x, muzzlePos.y, muzzlePos.z,
                                1, dx, dy, dz, 0.01
                        );
                    }
                    serverLevel.sendParticles(ParticleTypes.FLAME, muzzlePos.x, muzzlePos.y, muzzlePos.z, 10, 0.1, 0.1, 0.1, 0.01);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, muzzlePos.x, muzzlePos.y, muzzlePos.z, 15, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }
    }

}
