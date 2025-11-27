package org.ywzj.vehicle.entity.vehicle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.client.render.animation.TrackAnimationInstance;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class M1a2 extends TrackedVehicle {

    private TrackAnimationInstance trackAnimationInstance;

    public record ScheduleTask(int tickCount, Runnable task) {
    }

    private final Queue<ScheduleTask> scheduledTasks = new PriorityQueue<>(Comparator.comparingInt(task -> task.tickCount));

    public M1a2(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void tickParticle() {
        super.tickParticle();
        double velocity = Math.abs(entityData.get(FORWARD_SPEED)) * 20 + Math.abs(entityData.get(TURN_SPEED)) * 5;
        if ((!this.getPassengers().isEmpty() && velocity > 0 || tickCount % 10 == 0) && hasPower()) {
            Vec3 v1 = this.getLookAngle();
            Vec3 v2 = new Vec3(-v1.z, 0, v1.x).normalize();
            Vec3 engineSmokePos = this.position().add(this.getLookAngle().normalize().scale(-4.2)).add(v2.scale(0.2)).add(0, 1.7, 0);
            for (int count = 0; count < velocity / 32 + 1; count++) {
                Vec3 engineSmokeVelocity = this.getLookAngle().normalize().scale(-0.1);
                level().addParticle(ParticleTypes.LARGE_SMOKE, true,
                        engineSmokePos.x, engineSmokePos.y, engineSmokePos.z,
                        engineSmokeVelocity.x, engineSmokeVelocity.y, engineSmokeVelocity.z);
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
            } else if (partUnitIndex == 0 && currentWeaponIndex == 1) {
                // todo: 测试音效
                this.level().playSound(null, this, AllSounds.GUN_12_7MM_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
            } else if (partUnitIndex == 3) {
                // todo: 测试音效
                this.level().playSound(null, this, AllSounds.GUN_12_7MM_SHOT.get(), SoundSource.PLAYERS, 16f, 1f);
            }
        }
    }

}
