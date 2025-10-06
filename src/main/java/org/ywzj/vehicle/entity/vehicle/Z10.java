package org.ywzj.vehicle.entity.vehicle;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.util.TaczHelper;
import org.ywzj.vehicle.vehicle.SpotterUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class Z10 extends HelicopterVehicle {

    public Z10(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.thirdPersonOffset = new Vec3(0, 6, -9);
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
        turret.setXRotSpeed(60f / 20);
        turret.setYRotSpeed(60f / 20);
        turret.setXRotMax(45f);
        turret.setXRotMin(-13f);
        turret.yRotMax = 90f;
        turret.yRotMin = -90f;
        this.partUnits.add(turret);
        this.operatorUnits.add(turret);
        this.spotterUnit = new SpotterUnit(this,
                new Vec3(0, 4.54d, -0.375d),
                new Vec3(0, 0d, -6d),
                new Vec3(0, -2.2d, -1.2d),
                null);
    }

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

    private final ItemStack gunRPG = GunItemBuilder.create()
            .setId(new ResourceLocation("tacz:rpg7"))
            .setFireMode(FireMode.AUTO)
            .setAmmoCount(1)
            .setAmmoInBarrel(true)
            .build();

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (operatorUnits.get(weaponIndex) instanceof WeaponUnit weaponUnit) {
//            weaponUnit.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
//            this.level().playSound(null, this, AllSounds.LAV150_SHOOT.get(), SoundSource.PLAYERS, 16f, 1f);

            IGunOperator.fromLivingEntity(this).draw(() -> gunRPG);
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            TaczHelper.shoot(this.position().add(v2.scale(2)), gunRPG, () -> this.getXRot() - 10, this::getYRot, false, this, null);
            TaczHelper.shoot(this.position().add(v2.scale(-2)), gunRPG, () -> this.getXRot() - 10, this::getYRot, false, this, null);

        }
    }

}
