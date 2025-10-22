package org.ywzj.vehicle.entity.vehicle;

import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.vehicle.SpotterUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class Ka50 extends HelicopterVehicle {

    public Ka50(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.thirdPersonOffset = new Vec3(0, 6, -12);
    }

    @Override
    public int getSeats() {
        return 1;
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
        WeaponUnit sight = new WeaponUnit("sight",
                0,
                this,
                new Vec3(-0.2d, 1d, 5.3d),
                3f,
                new Vec3(0, 0, 0),
                new Vec3(0, 1.1d, -3d),
                new Vec3(-0.2d, 2.1d, 3d),
                null);
        sight.setXRotSpeed(60f / 20);
        sight.setYRotSpeed(60f / 20);
        sight.setXRotMax(45f);
        sight.setXRotMin(-13f);
        sight.setYRotMax(60f);
        sight.setYRotMin(-60f);
        sight.setOperatorOnWeaponUnit(false);
        this.partUnits.add(sight);
        this.operatorUnits.add(sight);
        WeaponUnit autoCannon = new WeaponUnit("auto_cannon",
                1,
                this,
                new Vec3(-1.2, 1d, 0d),
                1f,
                new Vec3(1, 0d, 5.3d),
                new Vec3(1, 1.3d, 2.2d),
                new Vec3(1, 1d, 2.2d),
                null);
        autoCannon.setXRotSpeed(60f / 20);
        autoCannon.setYRotSpeed(60f / 20);
        autoCannon.setXRotMax(45f);
        autoCannon.setXRotMin(0f);
        autoCannon.setYRotMax(20f);
        autoCannon.setYRotMin(0f);
        autoCannon.setParentWeaponUnit(sight);
        sight.addSubWeaponUnit(autoCannon);
        this.partUnits.add(autoCannon);
        this.spotterUnit = new SpotterUnit(this,
                new Vec3(0, 4.54d, -0.375d),
                new Vec3(0, 0d, -6d),
                new Vec3(0, -2.2d, -1.2d),
                null);
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

    private final ItemStack gunRPG = GunItemBuilder.create()
            .setId(new ResourceLocation("tacz:rpg7"))
            .setFireMode(FireMode.AUTO)
            .setAmmoCount(1)
            .setAmmoInBarrel(true)
            .build();
    private int count;

    @Override
    public void shoot(int weaponIndex, Vec3 ammoSpawnPosition, float ammoXRot, float ammoYRot) {
        if (operatorUnits.get(weaponIndex) instanceof WeaponUnit weaponUnit) {
            weaponUnit.shoot(ammoSpawnPosition, ammoXRot, ammoYRot);
            this.level().playSound(null, this, AllSounds.AUTO_CANNON_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);

//            IGunOperator.fromLivingEntity(this).draw(() -> gunRPG);
//            Vec3 v1 = this.getLookAngle();
//            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
//            TaczHelper.shoot(this.position().add(v2.scale(2)), gunRPG, () -> this.getXRot() - 10, this::getYRot, false, this, null);
//            TaczHelper.shoot(this.position().add(v2.scale(-2)), gunRPG, () -> this.getXRot() - 10, this::getYRot, false, this, null);

            //todo: 测试导弹
            //todo: 导弹name从武器站当前武器的配置名取
//            Vec3 v1 = this.getLookAngle();
//            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
//            Vec3 missilePosLeft = this.position().add(v2.scale(2));
//            Vec3 missilePosRight = this.position().add(v2.scale(-2));
//            count += 1;
//            weaponUnit.shoot(count % 2 ==0 ? missilePosLeft : missilePosRight, ammoXRot, ammoYRot);

        }

    }

}
