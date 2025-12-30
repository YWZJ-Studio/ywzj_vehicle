package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.Bolt;
import org.ywzj.vehicle.vehicle.pojo.Explosion;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;
import org.ywzj.vehicle.vehicle.weapon.VehicleGrenade;

public class Ztz99a extends TrackedVehicle {

    public Ztz99a(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * 仅做示例: 如何硬编码构造载具部件
     */
    @Deprecated
    @Override
    public void initPartUnits() {
        // 炮塔位
        WeaponUnit turret = new WeaponUnit("turret",
                0,
                this,
                new Vec3(0, 1.875, 0.5),
                8.233f,
                new Vec3(0.75, 0.5, 0.5),
                new Vec3(0, 1.5d, 0),
                new Vec3(0, 2, 0),
                null);
        turret.crosshairStyle = WeaponUnitData.CrosshairStyle.CIRCLE;
        turret.setXRotSpeed(30f / 20);
        turret.setYRotSpeed(30f / 20);
        turret.setXRotMax(5f);
        turret.setXRotMin(-13f);
        turret.currentWeaponIndexHolder = turret.getSyncData().define(
                SyncDataSerializers.INT,
                turret::setCurrentWeaponIndex,
                turret::getCurrentWeaponIndex,
                0
        );
        // 炮塔-主炮
        VehicleCannonWeaponData weaponDataCannon = new VehicleCannonWeaponData();
        weaponDataCannon.setName("cannon");
        weaponDataCannon.setMaxCapacity(1);
        weaponDataCannon.setDamage(99);
        Explosion explosion = new Explosion();
        explosion.explode = true;
        explosion.damage = 30;
        explosion.radius = 8;
        weaponDataCannon.setExplosion(explosion);
        weaponDataCannon.setReload(new BaseVehicleWeaponData.Reload(100, Ingredient.of(AllItems.AMMO_ARTILLERY.get())));
        VehicleCannon vehicleCannon = new VehicleCannon(this, turret, 0, weaponDataCannon, "cannon");
        vehicleCannon.defineSyncData(turret.getSyncData());
        turret.weapons.add(vehicleCannon);
        this.partUnits.add(turret);
        this.seats.add(new Seat(0, turret));
        // 炮塔-烟雾弹
        WeaponUnit smokeGrenade = new WeaponUnit("smoke_grenade",
                1,
                this,
                new Vec3(0, 1.875, 0.5),
                1f,
                new Vec3(0.75, 0.5, 0.5),
                new Vec3(0, 1.5d, 0),
                new Vec3(0, 2, 0),
                turret);
        smokeGrenade.crosshairStyle = WeaponUnitData.CrosshairStyle.NONE;
        smokeGrenade.setFiringMode(WeaponUnitData.FiringMode.RIPPLE);
        smokeGrenade.getBolts().clear();
        smokeGrenade.getBolts().add(new Bolt(new Vec3(1.5d, 0, 0.5d), 0.1f, 0, 0));
        smokeGrenade.getBolts().add(new Bolt(new Vec3(-1.5d, 0, 0.5d), 0.1f, 0, 0));
        smokeGrenade.setXRot(-30);
        smokeGrenade.setXRotSpeed(0);
        smokeGrenade.setYRotSpeed(0);
        smokeGrenade.setParentWeaponUnit(turret);
        turret.addSubWeaponUnit(smokeGrenade);
        VehicleGrenadeWeaponData vehicleGrenadeWeaponData = new VehicleGrenadeWeaponData();
        vehicleGrenadeWeaponData.setName("smoke_grenade");
        vehicleGrenadeWeaponData.setMaxCapacity(8);
        vehicleGrenadeWeaponData.setReload(new BaseVehicleWeaponData.Reload(100, Ingredient.of(AllItems.AMMO_GRENADE.get())));
        VehicleGrenade vehicleGrenade = new VehicleGrenade(this, smokeGrenade, 1, vehicleGrenadeWeaponData, "smoke_grenade");
        vehicleGrenade.defineSyncData(smokeGrenade.getSyncData());
        turret.weapons.add(vehicleGrenade);
        this.partUnits.add(smokeGrenade);
        // 车长位
        WeaponUnit commanderMachineGun = new WeaponUnit("commander_machine_gun",
                2,
                this,
                new Vec3(-0.736d, 3.056d, -0.595),
                1.7f,
                new Vec3(-1d, 0d, -1),
                new Vec3(0.5d, 0.5d, -1d),
                new Vec3(0, 2d, 0d),
                turret);
        commanderMachineGun.needPower = false;
        commanderMachineGun.crosshairStyle = WeaponUnitData.CrosshairStyle.CIRCLE;
        commanderMachineGun.opticalSightType = WeaponUnitData.OpticalSightType.OPERATOR;
        commanderMachineGun.setOperatorOnWeaponUnit(false);
        commanderMachineGun.setXRotSpeed(60f / 20);
        commanderMachineGun.setYRotSpeed(60f / 20);
        commanderMachineGun.setXRotMax(15f);
        commanderMachineGun.setXRotMin(-18f);
        // 车长位-机枪
        VehicleCannonWeaponData weaponDataMachineGun = new VehicleCannonWeaponData();
        weaponDataMachineGun.setName("machine_gun");
        weaponDataMachineGun.setVelocity(8);
        weaponDataMachineGun.setMaxCapacity(120);
        weaponDataMachineGun.setDamage(4);
        weaponDataMachineGun.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_MACHINE_GUN.get())));
        VehicleCannon vehicleMachineGun = new VehicleCannon(this, commanderMachineGun, 0, weaponDataMachineGun, "machine_gun");
        vehicleMachineGun.defineSyncData(commanderMachineGun.getSyncData());
        commanderMachineGun.weapons.add(vehicleMachineGun);
        this.partUnits.add(commanderMachineGun);
        this.seats.add(new Seat(1, commanderMachineGun));
        // 乘员位
        PartUnit<?> seat = new PartUnit<>("seat", 2, this);
        seat.setOwnerViewOffset(new Vec3(-0.236, 3.556d, -1.595));
        seat.setSeatOffset(new Vec3(0, 2d, 0d));
        this.partUnits.add(seat);
        this.seats.add(new Seat(2, seat));
    }

}
