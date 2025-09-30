package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.vehicle.SpotterUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

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
        WeaponUnit turret = new WeaponUnit("z10",
                0,
                this,
                new Vec3(0, 4.54d, -0.375d),
                1f,
                new Vec3(0, 0d, -6d),
                new Vec3(0, -2.2d, -1.2d),
                null);
        turret.xRotSpeed = 60f / 20;
        turret.yRotSpeed = 60f / 20;
        turret.xRotMax = 45f;
        turret.xRotMin = -13f;
        this.partUnits.add(turret);
        this.operatorUnits.add(turret);
        this.spotterUnit = new SpotterUnit(this,
                new Vec3(0, 4.54d, -0.375d),
                new Vec3(0, 0d, -6d),
                new Vec3(0, -2.2d, -1.2d),
                null);
    }

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (operatorUnits.get(weaponIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
            this.level().playSound(null, this, AllSounds.LAV150_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);
        }
    }

}
