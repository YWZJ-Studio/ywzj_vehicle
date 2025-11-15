package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.weapon.data.BaseVehicleWeaponData;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.weapon.VehicleAerialBomb;

import java.util.List;

public class Mi24 extends RotaryWingVehicle {

    public Mi24(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.physicsEngine.mass = 1.5f;
        this.mainRotorForce = 1.1f * physicsEngine.gravityA * physicsEngine.mass;
        this.xRotSpeedMax = 2f;
        this.yRotSpeedMax = 2f;
        this.zRotSpeedMax = 2f;
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
                new Vec3(0, 4.54d, -0.375d),
                1f,
                null,
                new Vec3(0, 0d, -6d),
                new Vec3(0, 2.2d, 2d),
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
        //todo: 测试航弹
        WeaponUnit bomb = new WeaponUnit("bomb",
                1,
                this,
                new Vec3(2.5d, 1d, 1d),
                1f,
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 0.6d, 1.2d),
                new Vec3(0, 2d, 0d),
                null);
        bomb.setXRotSpeed(0);
        bomb.setYRotSpeed(0);
        bomb.setParentWeaponUnit(sightingSystem);
        sightingSystem.addSubWeaponUnit(bomb);
        BaseVehicleWeaponData weaponDataBomb = new BaseVehicleWeaponData();
        weaponDataBomb.setDisplayName("bomb");
        weaponDataBomb.setMaxCapacity(4);
        weaponDataBomb.setReload(new BaseVehicleWeaponData.Reload(20, Ingredient.of(AllItems.AMMO_AERIAL_BOMB.get())));
        VehicleAerialBomb vehicleBomb = new VehicleAerialBomb(this, bomb, 0, weaponDataBomb, "bomb");
        vehicleBomb.defineSyncData(bomb.getSyncData());
        sightingSystem.weapons.add(vehicleBomb);
        this.partUnits.add(bomb);
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
        float engineSpeed = getPower();
        int collectivePitch = getCollectivePitch();
        if ((!this.getPassengers().isEmpty() && engineSpeed > 0 && tickCount % Mth.clamp(10 - collectivePitch / 10, 3, 10) == 0) && hasPower()) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePosLeft = this.position().add(this.getLookAngle().normalize().scale(3f)).add(v2.scale(-1)).add(0, 2.5, 0);
            Vec3 engineSmokePosRight = this.position().add(this.getLookAngle().normalize().scale(3f)).add(v2.scale(1)).add(0, 2.5, 0);
            Vec3 vSmoke = v1.scale(-0.3);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosLeft.x, engineSmokePosLeft.y, engineSmokePosLeft.z, vSmoke.x, vSmoke.y, vSmoke.z);
            level().addParticle(ParticleTypes.LARGE_SMOKE, true, engineSmokePosRight.x, engineSmokePosRight.y, engineSmokePosRight.z, vSmoke.x, vSmoke.y, vSmoke.z);
        }
    }

    @Override
    public void shoot(int partUnitIndex, List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot, @Nullable LivingEntity operator) {
        if (partUnits.get(partUnitIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(ammoSpawnPositions, ammoXRot, ammoYRot, operator);
        }
    }

}
