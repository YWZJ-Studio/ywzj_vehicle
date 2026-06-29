package org.ywzj.vehicle.entity.vehicle.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleMissileWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleRocketWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.Bolt;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;
import org.ywzj.vehicle.vehicle.weapon.VehicleMissile;
import org.ywzj.vehicle.vehicle.weapon.VehicleRocket;

public class Z10 extends RotaryWingVehicle {

    public Z10(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * 仅做示例: 如何硬编码构造载具部件
     */
    @Deprecated
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
        autoCannon.crosshairStyle = WeaponUnitData.CrosshairStyle.SQUARE;
        autoCannon.setXRotSpeed(60f / 20);
        autoCannon.setYRotSpeed(60f / 20);
        autoCannon.setXRotMax(45f);
        autoCannon.setXRotMin(-13f);
        autoCannon.setYRotMax(90f);
        autoCannon.setYRotMin(-90f);
        autoCannon.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(autoCannon);
        VehicleCannonWeaponData weaponDataAutoCannon = new VehicleCannonWeaponData();
        weaponDataAutoCannon.setName("auto_cannon");
        weaponDataAutoCannon.setMaxCapacity(120);
        weaponDataAutoCannon.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_AUTO_CANNON.get())));
        VehicleCannon vehicleCannon = new VehicleCannon(this, autoCannon, 0, weaponDataAutoCannon, "auto_cannon");
        vehicleCannon.defineSyncData(autoCannon.getSyncData());
        sightingSystem.weapons.add(vehicleCannon);
        this.partUnits.add(autoCannon);
        // 导弹挂架
        WeaponUnit missile = new WeaponUnit("missile",
                2,
                this,
                new Vec3(0, 1d, 0),
                0f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        missile.setParentWeaponUnitAim(true);
        missile.crosshairStyle = WeaponUnitData.CrosshairStyle.CIRCLE;
        missile.setFiringMode(WeaponUnitData.FiringMode.RIPPLE);
        missile.getBolts().clear();
        missile.getBolts().add(new Bolt(new Vec3(2.5d, 0, 1d), 0.1f, 0, 0));
        missile.getBolts().add(new Bolt(new Vec3(-2.5d, 0, 1d), 0.1f, 0, 0));
        missile.setXRotSpeed(0);
        missile.setYRotSpeed(0);
        missile.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(missile);
        VehicleMissileWeaponData weaponDataMissile = new VehicleMissileWeaponData();
        weaponDataMissile.setName("missile");
        weaponDataMissile.setMaxCapacity(8);
        weaponDataMissile.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_MISSILE.get())));
        VehicleMissile vehicleMissile = new VehicleMissile(this, missile, 1, weaponDataMissile, "missile");
        vehicleMissile.defineSyncData(missile.getSyncData());
        sightingSystem.weapons.add(vehicleMissile);
        this.partUnits.add(missile);
        // 火箭弹挂架
        WeaponUnit rocket = new WeaponUnit("rocket",
                3,
                this,
                new Vec3(0, 1d, 0),
                0f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        rocket.crosshairStyle = WeaponUnitData.CrosshairStyle.CROSS;
        rocket.setFiringMode(WeaponUnitData.FiringMode.RIPPLE);
        rocket.getBolts().clear();
        rocket.getBolts().add(new Bolt(new Vec3(1.5d, 0, 1d), 0.1f, 0, 0));
        rocket.getBolts().add(new Bolt(new Vec3(-1.5d, 0, 1d), 0.1f, 0, 0));
        rocket.setXRotSpeed(0);
        rocket.setYRotSpeed(0);
        rocket.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(rocket);
        VehicleRocketWeaponData weaponDataRocket = new VehicleRocketWeaponData();
        weaponDataRocket.setName("rocket");
        weaponDataRocket.setMaxCapacity(32);
        weaponDataRocket.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_ROCKET.get())));
        VehicleRocket vehicleRocket = new VehicleRocket(this, rocket, 2, weaponDataRocket, "rocket");
        vehicleRocket.defineSyncData(rocket.getSyncData());
        sightingSystem.weapons.add(vehicleRocket);
        this.partUnits.add(rocket);
    }

}
