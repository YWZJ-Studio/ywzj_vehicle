package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.ywzj.vehicle.custom.weapon.data.VehicleCannonWeaponData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientScreenShake;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.List;

public class VehicleCannon extends AbstractVehicleWeapon<VehicleCannonWeaponData> {

    public VehicleCannon(AbstractVehicle vehicle, WeaponUnit unit, int index, VehicleCannonWeaponData data, String serializeId) {
        super(vehicle, unit, index, data, serializeId);
    }

    @Override
    public boolean shoot(List<AimContext> aimContexts, LivingEntity shooter) {
        if (isCoolingDown() || isReloading() || !consumeAmmo(aimContexts)) {
            return false;
        }
        this.lastShootTime = System.currentTimeMillis();

        var vehicle = getVehicle();
        var data = this.getData();

        for (AimContext aimContext : aimContexts) {
            BulletEntity bulletEntity = new BulletEntity(vehicle.level(), vehicle, shooter, aimContext.position, getData().getExplosion(), data.getWeaponId());
            bulletEntity.shootFromRotation(vehicle, aimContext.direction.x, aimContext.direction.y, 0, data.getVelocity(), data.getInaccuracy());
            bulletEntity.setDamage(data.getDamage());
            bulletEntity.setHeadShot(data.getHeadshotMultiplier());
            bulletEntity.setWeaponData(data); // Enable distance-based damage falloff
            vehicle.level().addFreshEntity(bulletEntity);
            vehicle.physicsEngine.recoil(getWeaponUnit(), data.getRecoil());
            
            // Muzzle flash and dust effects for large caliber weapons
            if (data.getCaliber() >= 100 && vehicle.level() instanceof ServerLevel serverLevel) {
                // Convert Vec2 direction to Vec3
                Vec3 direction3D = new Vec3(
                    Math.sin(aimContext.direction.y * Math.PI / 180.0) * Math.cos(aimContext.direction.x * Math.PI / 180.0),
                    -Math.sin(aimContext.direction.x * Math.PI / 180.0),
                    Math.cos(aimContext.direction.y * Math.PI / 180.0) * Math.cos(aimContext.direction.x * Math.PI / 180.0)
                );
                
                spawnMuzzleFlash(serverLevel, aimContext.position, direction3D);
                spawnRecoilDust(serverLevel, vehicle);
                
                // Screen shake for passengers
                float shakeIntensity = calculateShakeIntensity(data.getCaliber());
                vehicle.getPassengers().forEach(passenger -> {
                    if (passenger instanceof ServerPlayer serverPlayer) {
                        Channel.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new ClientScreenShake(shakeIntensity, 300)
                        );
                    }
                });
            }
        }
        return true;
    }

    /**
     * Calculates screen shake intensity based on weapon caliber.
     */
    private float calculateShakeIntensity(float caliber) {
        // 105mm = 3.0, 120mm = 4.0, 125mm = 4.5
        return Math.min(caliber / 25.0f, 6.0f);
    }

    /**
     * Spawns epic muzzle flash effect with fire, smoke and sparks.
     */
    private void spawnMuzzleFlash(ServerLevel level, Vec3 muzzlePos, Vec3 direction) {
        // Large fire burst
        for (int i = 0; i < 30; i++) {
            double spreadX = (level.random.nextDouble() - 0.5) * 0.5;
            double spreadY = (level.random.nextDouble() - 0.5) * 0.5;
            double spreadZ = (level.random.nextDouble() - 0.5) * 0.5;
            
            level.sendParticles(
                ParticleTypes.FLAME,
                muzzlePos.x, muzzlePos.y, muzzlePos.z,
                1,
                direction.x * 2 + spreadX,
                direction.y * 2 + spreadY,
                direction.z * 2 + spreadZ,
                0.3
            );
        }
        
        // Smoke cloud
        for (int i = 0; i < 40; i++) {
            double spreadX = (level.random.nextDouble() - 0.5) * 0.8;
            double spreadY = (level.random.nextDouble() - 0.5) * 0.8;
            double spreadZ = (level.random.nextDouble() - 0.5) * 0.8;
            
            level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                muzzlePos.x, muzzlePos.y, muzzlePos.z,
                1,
                direction.x + spreadX,
                direction.y + spreadY,
                direction.z + spreadZ,
                0.15
            );
        }
        
        // Sparks/embers
        for (int i = 0; i < 20; i++) {
            double spreadX = (level.random.nextDouble() - 0.5) * 1.0;
            double spreadY = (level.random.nextDouble() - 0.5) * 1.0;
            double spreadZ = (level.random.nextDouble() - 0.5) * 1.0;
            
            level.sendParticles(
                ParticleTypes.LAVA,
                muzzlePos.x, muzzlePos.y, muzzlePos.z,
                1,
                direction.x * 1.5 + spreadX,
                direction.y * 1.5 + spreadY,
                direction.z * 1.5 + spreadZ,
                0.2
            );
        }
        
        // Explosion particle for flash
        level.sendParticles(
            ParticleTypes.EXPLOSION,
            muzzlePos.x, muzzlePos.y, muzzlePos.z,
            3,
            0.3, 0.3, 0.3,
            0.0
        );
    }

    /**
     * Spawns dust cloud around vehicle from recoil force.
     */
    private void spawnRecoilDust(ServerLevel level, AbstractVehicle vehicle) {
        Vec3 vehiclePos = vehicle.position();
        
        // Dust cloud around vehicle base
        for (int i = 0; i < 50; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 1.5 + level.random.nextDouble() * 2.0;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            level.sendParticles(
                ParticleTypes.POOF,
                vehiclePos.x + offsetX,
                vehiclePos.y + 0.1,
                vehiclePos.z + offsetZ,
                1,
                (level.random.nextDouble() - 0.5) * 0.3,
                level.random.nextDouble() * 0.3,
                (level.random.nextDouble() - 0.5) * 0.3,
                0.05
            );
        }
        
        // Additional smoke from sides
        for (int i = 0; i < 15; i++) {
            double sideOffset = (level.random.nextDouble() - 0.5) * 3.0;
            
            level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                vehiclePos.x + sideOffset,
                vehiclePos.y + 0.5,
                vehiclePos.z + sideOffset,
                1,
                (level.random.nextDouble() - 0.5) * 0.2,
                level.random.nextDouble() * 0.4,
                (level.random.nextDouble() - 0.5) * 0.2,
                0.03
            );
        }
    }

}
