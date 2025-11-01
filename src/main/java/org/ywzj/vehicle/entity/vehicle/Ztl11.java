package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.custom.VehicleDataManager;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.ArrayList;
import java.util.List;

public class Ztl11 extends WheeledVehicle {

    public Ztl11(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void initData() {
        VehicleDataManager.get().getVehicleData(YwzjVehicle.modLoc("ztl11")).ifPresent(data -> {
            var struct = data.getVehicleStructObbs();
            this.mainCubeOBB = struct.mainCubeOBB();
            this.vehicleOBBs = struct.obbs();
            var weapons = data.createPartUnits(this);
//            this.operatorUnits.addAll(weapons.values());
            List<PartUnit<?>> weaponUnits = new ArrayList<>(weapons.values());
            for (int index = 0; index < weapons.size(); index++) {
                this.seats.add(new Seat(index, weaponUnits.get(index)));
            }
            this.partUnits.addAll(weapons.values());
        });
    }

    @Override
    public SoundEvent getEngineStartSound() {
        return AllSounds.LAV150_ENGINE_START.get();
    }

    @Override
    public SoundEvent getEngineIdleSound() {
        return AllSounds.LAV150_ENGINE_IDLE.get();
    }

    @Override
    public SoundEvent getEngineRunSound() {
        return AllSounds.LAV150_ENGINE_RUN.get();
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
    public void shoot(int weaponIndex, List<Vec3> ammoSpawnPositions, float ammoXRot, float ammoYRot) {
        if (weaponIndex < this.partUnits.size()) {
            this.getPartUnit(weaponIndex).ifPresent(partUnit -> {
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    weaponUnit.shoot(ammoSpawnPositions, ammoXRot, ammoYRot);
                    this.level().playSound(null, this, AllSounds.AUTO_CANNON_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
                }
            });
        }
    }

}
