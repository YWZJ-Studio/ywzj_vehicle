package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.weapon.BaseVehicleWeaponData;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;
import org.ywzj.vehicle.vehicle.weapon.VehicleMissile;
import org.ywzj.vehicle.vehicle.weapon.VehicleRocket;

public class Z10 extends HelicopterVehicle {

    public Z10(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return AllSounds.Z10_ENGINE_START.get();
    }

    @Override
    public SoundEvent getEngineStopSound() {
        return AllSounds.Z10_ENGINE_STOP.get();
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.Z10_ENGINE_RUN.get();
    }

    @Override
    public void initPartUnits() {
        // 观瞄
        WeaponUnit sightingSystem = new WeaponUnit("sighting_system",
                0,
                this,
                new Vec3(0, 0.5d, 5d),
                1f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        sightingSystem.setXRotSpeed(60f / 20);
        sightingSystem.setYRotSpeed(60f / 20);
        sightingSystem.setXRotMax(45f);
        sightingSystem.setXRotMin(-13f);
        sightingSystem.setYRotMax(90f);
        sightingSystem.setYRotMin(-90f);
        sightingSystem.setOperatorOnWeaponUnit(false);
        sightingSystem.currentWeaponIndexHolder = sightingSystem.getSyncData().define(
                SyncDataSerializers.INT,
                sightingSystem::setCurrentWeaponIndex,
                sightingSystem::getCurrentWeaponIndex,
                0
        );
        this.partUnits.add(sightingSystem);
        this.seats.add(new Seat(0, sightingSystem));
        // 机炮
        WeaponUnit autoCannon = new WeaponUnit("auto_cannon",
                1,
                this,
                new Vec3(0, 0.5d, 5d),
                1f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        autoCannon.setXRotSpeed(60f / 20);
        autoCannon.setYRotSpeed(60f / 20);
        autoCannon.setXRotMax(45f);
        autoCannon.setXRotMin(-13f);
        autoCannon.setYRotMax(90f);
        autoCannon.setYRotMin(-90f);
        autoCannon.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(autoCannon);
        BaseVehicleWeaponData weaponDataAutoCannon = new BaseVehicleWeaponData();
        weaponDataAutoCannon.setName("auto_cannon");
        weaponDataAutoCannon.setMaxCapacity(120);
        weaponDataAutoCannon.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_AUTO_CANNON.get())));
        VehicleCannon vehicleCannon = new VehicleCannon(this, autoCannon, 0, weaponDataAutoCannon);
        vehicleCannon.defineSyncData(autoCannon.getSyncData());
        sightingSystem.weapons.add(vehicleCannon);
        this.partUnits.add(autoCannon);
        // 导弹挂架左
        WeaponUnit missileLeft = new WeaponUnit("missile_left",
                2,
                this,
                new Vec3(2.5d, 1d, 1d),
                0f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        missileLeft.setXRotSpeed(0);
        missileLeft.setYRotSpeed(0);
        missileLeft.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(missileLeft);
        BaseVehicleWeaponData weaponDataMissileLeft = new BaseVehicleWeaponData();
        weaponDataMissileLeft.setName("missile_left");
        weaponDataMissileLeft.setMaxCapacity(8);
        weaponDataMissileLeft.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_MISSILE.get())));
        VehicleMissile vehicleMissileLeft = new VehicleMissile(this, missileLeft, 1, weaponDataMissileLeft);
        vehicleMissileLeft.defineSyncData(missileLeft.getSyncData());
        sightingSystem.weapons.add(vehicleMissileLeft);
        this.partUnits.add(missileLeft);
        // 导弹挂架右
        WeaponUnit missileRight = new WeaponUnit("missile_right",
                3,
                this,
                new Vec3(-2.5d, 1d, 1d),
                0f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        missileRight.setXRotSpeed(0);
        missileRight.setYRotSpeed(0);
        missileRight.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(missileRight);
        BaseVehicleWeaponData weaponDataMissileRight = new BaseVehicleWeaponData();
        weaponDataMissileRight.setName("missile_right");
        weaponDataMissileRight.setMaxCapacity(8);
        weaponDataMissileRight.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_MISSILE.get())));
        VehicleMissile vehicleMissileRight = new VehicleMissile(this, missileRight, 2, weaponDataMissileRight);
        vehicleMissileRight.defineSyncData(missileRight.getSyncData());
        sightingSystem.weapons.add(vehicleMissileRight);
        this.partUnits.add(missileRight);
        // 火箭弹挂架左
        WeaponUnit rocketLeft = new WeaponUnit("rocket_left",
                4,
                this,
                new Vec3(1.5d, 1d, 1d),
                0f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        rocketLeft.setXRotSpeed(0);
        rocketLeft.setYRotSpeed(0);
        rocketLeft.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(rocketLeft);
        BaseVehicleWeaponData weaponDataRocketLeft = new BaseVehicleWeaponData();
        weaponDataRocketLeft.setName("rocket_left");
        weaponDataRocketLeft.setMaxCapacity(32);
        weaponDataRocketLeft.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_ROCKET.get())));
        VehicleRocket vehicleRocketLeft = new VehicleRocket(this, rocketLeft, 3, weaponDataRocketLeft);
        vehicleRocketLeft.defineSyncData(rocketLeft.getSyncData());
        sightingSystem.weapons.add(vehicleRocketLeft);
        this.partUnits.add(rocketLeft);
        // 火箭弹挂架右
        WeaponUnit rocketRight = new WeaponUnit("rocket_right",
                5,
                this,
                new Vec3(-1.5d, 1d, 1d),
                0f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        rocketRight.setXRotSpeed(0);
        rocketRight.setYRotSpeed(0);
        rocketRight.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(rocketRight);
        BaseVehicleWeaponData weaponDataRocketRight = new BaseVehicleWeaponData();
        weaponDataRocketRight.setName("rocket_right");
        weaponDataRocketRight.setMaxCapacity(32);
        weaponDataRocketRight.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_ROCKET.get())));
        VehicleRocket vehicleRocketRight = new VehicleRocket(this, rocketRight, 4, weaponDataRocketRight);
        vehicleRocketRight.defineSyncData(rocketRight.getSyncData());
        sightingSystem.weapons.add(vehicleRocketRight);
        this.partUnits.add(rocketRight);
    }

//    @Override
//    public void initData() {
//        VehicleDataManager.get().getVehicleData(YwzjVehicle.modLoc("z10")).ifPresent(data -> {
//            var struct = data.getVehicleStructObbs();
//            this.mainCubeOBB = struct.mainCubeOBB();
//            this.vehicleBodyOBBs = struct.obbs();
//            var weapons = data.createPartUnits(this);
//            this.operatorUnits.addAll(weapons.values());
//            this.partUnits.addAll(weapons.values());
//        });
//        this.spotterUnit = new SpotterUnit(this,
//                new Vec3(0, 4.54d, -0.375d),
//                new Vec3(0, 0d, -6d),
//                new Vec3(0, -2.2d, -1.2d),
//                null);
//
//    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
        float engineSpeed = getPower();
        int collectivePitch = getCollectivePitch();
        if ((!this.getPassengers().isEmpty() && engineSpeed > 0 && tickCount % Mth.clamp(10 - collectivePitch / 10, 3, 10) == 0) && hasPower()) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePosLeft = this.position().add(this.getLookAngle().normalize().scale(-1f)).add(v2.scale(-0.9)).add(0, 2.5, 0);
            Vec3 engineSmokePosRight = this.position().add(this.getLookAngle().normalize().scale(-1f)).add(v2.scale(0.9)).add(0, 2.5, 0);
            Vec3 vSmoke = v1.scale(-0.3);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosLeft.x, engineSmokePosLeft.y, engineSmokePosLeft.z, vSmoke.x, vSmoke.y, vSmoke.z);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosRight.x, engineSmokePosRight.y, engineSmokePosRight.z, vSmoke.x, vSmoke.y, vSmoke.z);
        }
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (partUnits.get(weaponIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
        }
    }

}
