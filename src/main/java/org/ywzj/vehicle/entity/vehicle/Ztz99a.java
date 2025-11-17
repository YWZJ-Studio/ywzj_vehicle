package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.client.render.animation.TrackAnimationInstance;
import org.ywzj.vehicle.custom.pojo.Bolt;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.custom.weapon.data.VehicleGrenadeWeaponData;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.VehicleCannon;
import org.ywzj.vehicle.vehicle.weapon.VehicleGrenade;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class Ztz99a extends TrackedVehicle {

    private TrackAnimationInstance trackAnimationInstance;

    public record ScheduleTask(int tickCount, Runnable task) {
    }

    private final Queue<ScheduleTask> scheduledTasks = new PriorityQueue<>(Comparator.comparingInt(task -> task.tickCount));

    public Ztz99a(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

//    @Override
//    public void initData() {
//        VehicleDataManager.get().getVehicleData(YwzjVehicle.modLoc("ztz99a")).ifPresent(data -> {
//            var struct = data.getVehicleStructObbs();
//            this.mainCubeOBB = struct.mainCubeOBB();
//            this.vehicleOBBs = struct.obbs();
//            var weapons = data.createPartUnits(this);
//            List<PartUnit> weaponUnits = new ArrayList<>(weapons.values());
//            for (int index = 0; index < weapons.size(); index++) {
//                this.seats.add(new Seat(index, weaponUnits.get(index)));
//            }
//            this.partUnits.addAll(weapons.values());
//        });
//    }

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
        turret.crosshairStyle = WeaponUnit.CrosshairStyle.CIRCLE;
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
        weaponDataCannon.setExplosion(true);
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
        smokeGrenade.crosshairStyle = WeaponUnit.CrosshairStyle.NONE;
        smokeGrenade.setFiringMode(WeaponUnit.FiringMode.RIPPLE);
        smokeGrenade.getBolts().clear();
        smokeGrenade.getBolts().add(new Bolt(new Vec3(1.5d, 0, 0.5d), 0.1f));
        smokeGrenade.getBolts().add(new Bolt(new Vec3(-1.5d, 0, 0.5d), 0.1f));
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
        commanderMachineGun.crosshairStyle = WeaponUnit.CrosshairStyle.CIRCLE;
        commanderMachineGun.opticalSightType = WeaponUnit.OpticalSightType.OPERATOR;
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
    public void tick() {
        super.tick();
        while (!scheduledTasks.isEmpty()) {
            ScheduleTask scheduleTask = scheduledTasks.peek();
            if (scheduleTask.tickCount <= this.tickCount) {
                scheduleTask.task.run();
                scheduledTasks.poll();
            } else {
                break;
            }
        }
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
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
    public void shoot(int partUnitIndex, List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(ammoSpawnPositions, ammoXRot, ammoYRot, operator);
            int currentWeaponIndex = weaponUnit.getCurrentWeaponIndex();
            if (partUnitIndex == 0 && currentWeaponIndex == 0) {
                // todo: 测试音效
                this.level().playSound(null, this, AllSounds.CANNON_125_MM_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
                this.scheduledTasks.add(new ScheduleTask(this.tickCount + 20, () -> {
                    this.level().playSound(null, this, AllSounds.CANNON_SHELL_DROP.get(), SoundSource.PLAYERS, 16f, 1f);
                }));
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
            } else if (partUnitIndex == 2) {
                // todo: 测试音效
                this.level().playSound(null, this, AllSounds.AUTO_CANNON_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
            }
        }
    }

}
