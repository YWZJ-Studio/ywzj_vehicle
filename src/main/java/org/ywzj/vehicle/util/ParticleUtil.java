package org.ywzj.vehicle.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllParticleTypes;
import org.ywzj.vehicle.particle.DustSmokeOption;
import org.ywzj.vehicle.particle.SmokeCloudOption;

import java.util.function.Function;

public final class ParticleUtil {

    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 3.0F);

    public static void spawnDestroyedVehicleCloud(Level level, Vec3 center, float vehicleRadius, double vehicleHeight) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        float radius = (float) (level.random.nextDouble() * vehicleRadius * 0.3);
        double x = center.x + Math.cos(angle) * radius;
        double y = center.y + (level.random.nextDouble() - 0.5) * Math.max(1, vehicleHeight * 0.25);
        double z = center.z + Math.sin(angle) * radius;
        float heat = level.random.nextFloat();
        float startR = 0.45f + heat * 0.45f;
        float startG = 0.24f + heat * 0.18f;
        float startB = 0.12f + heat * 0.04f;
        float end = 0.06f + heat * 0.08f;
        float size = Math.max(1.5f, vehicleRadius * (0.3f + heat * 0.2f));
        double vx = Math.cos(angle) * (0.1 + level.random.nextDouble() * 0.08);
        double vz = Math.sin(angle) * (0.1 + level.random.nextDouble() * 0.08);
        double vy = vehicleRadius / 30 + level.random.nextDouble() * 0.12;
        level.addParticle(new SmokeCloudOption(startR, startG, startB, end, end, end,
                        (int) (60 + level.random.nextDouble() * 30), size, -0.002f),
                true, x, y, z, vx, vy, vz);
    }

    public static void spawnDestroyedPartCloud(Level level, Vec3 center, double partDepth) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        float radius = (float) (level.random.nextDouble() * partDepth / 2 * 0.5);
        double x = center.x + Math.cos(angle) * radius;
        double y = center.y + (level.random.nextDouble() - 0.5) * 0.5;
        double z = center.z + Math.sin(angle) * radius;
        float heat = level.random.nextFloat();
        float startR = 0.45f + heat * 0.45f;
        float startG = 0.24f + heat * 0.18f;
        float startB = 0.12f + heat * 0.04f;
        float end = 0.06f + heat * 0.08f;
        double vx = Math.cos(angle) * (0.02 + level.random.nextDouble() * 0.06);
        double vz = Math.sin(angle) * (0.02 + level.random.nextDouble() * 0.06);
        double vy = 0.05 + level.random.nextDouble() * 0.12;
        level.addParticle(new SmokeCloudOption(startR, startG, startB, end, end, end,
                        (int) (40 + level.random.nextDouble() * 20), radius + heat, -0.002f),
                true, x, y, z, vx, vy, vz);
    }

    public static void spawnWreckageSmoke(Level level, AABB boundingBox, int count) {
        for (int i = 0; i < count; i++) {
            double x = Mth.nextDouble(RandomSource.create(), boundingBox.minX, boundingBox.maxX);
            double y = Mth.nextDouble(RandomSource.create(), boundingBox.minY, boundingBox.maxY);
            double z = Mth.nextDouble(RandomSource.create(), boundingBox.minZ, boundingBox.maxZ);
            double vx = (level.random.nextDouble() - 0.5D) * 0.02D;
            double vy = level.random.nextDouble() * 0.05D + 0.02D;
            double vz = (level.random.nextDouble() - 0.5D) * 0.02D;
            level.addParticle(ParticleTypes.LARGE_SMOKE, true, x, y, z, vx, vy, vz);
        }
    }

    public static void spawnTracks(Level level, Entity vehicle, float trackSize, float yaw, Vec3... positions) {
        for (Vec3 position : positions) {
            if (EntityUtil.isOnBlockSurface(vehicle, position)) {
                level.addParticle(AllParticleTypes.TRACK.get(), true,
                        position.x, position.y, position.z, trackSize, yaw, 0);
            }
        }
    }

    public static void spawnEngineSmoke(Level level, Iterable<Vec3> offsets, Vec3 vehiclePosition,
                                        Function<Vec3, Vec3> positionTransform, Vec3 velocity,
                                        int count, int lifetime, float startSize, float endSize) {
        for (Vec3 offset : offsets) {
            Vec3 smokePosition = positionTransform.apply(vehiclePosition.add(offset));
            for (int i = 0; i < count; i++) {
                level.addParticle(new SmokeCloudOption(0.3f, 0.3f, 0.3f,
                                0.0f, 0.0f, 0.0f, 0.7f,
                                lifetime, startSize, endSize, 0.005f), true,
                        smokePosition.x, smokePosition.y, smokePosition.z,
                        velocity.x + (level.random.nextDouble() - 0.5) * 0.05,
                        velocity.y + (level.random.nextDouble() - 0.5) * 0.05,
                        velocity.z + (level.random.nextDouble() - 0.5) * 0.05);
            }
        }
    }

    public static void spawnWingVortices(Level level, Iterable<Vec3> offsets, Vec3 vehiclePosition, Function<Vec3, Vec3> positionTransform) {
        for (Vec3 offset : offsets) {
            Vec3 position = positionTransform.apply(vehiclePosition.add(offset));
            level.addParticle(WHITE_DUST, true, position.x, position.y, position.z, 0, 0, 0);
        }
    }

    public static void spawnRotorDownwash(Level level, RandomSource random, BlockPos vehiclePosition, double radius) {
        BlockPos surface = null;
        for (int y = 1; y <= 32; y++) {
            BlockPos checkPosition = vehiclePosition.below(y);
            if (!level.getBlockState(checkPosition).isAir()) {
                surface = checkPosition;
            }
        }
        if (surface == null) {
            return;
        }
        int pointCount = 8;
        int particleCount = 2;
        for (int i = 0; i < pointCount; i++) {
            for (int j = 0; j < particleCount; j++) {
                double bias = ((2 * Math.PI) / pointCount) * random.nextDouble();
                double angle = (i * 2 * Math.PI) / pointCount;
                double x = surface.getX() + radius * Math.cos(angle + bias) + random.nextDouble() * 0.5;
                double y = surface.getY() + 1 + random.nextDouble();
                double z = surface.getZ() + radius * Math.sin(angle + bias) + random.nextDouble() * 0.5;
                level.addParticle(WHITE_DUST, true, x, y, z, 0, 0, 0);
            }
        }
    }

    public static void spawnAerobaticSmoke(Level level, RandomSource random, Iterable<Vec3> offsets,
                                           Vec3 vehiclePosition, Vec3 previousPosition, Vec3 movement,
                                           Function<Vec3, Vec3> positionTransform,
                                           float red, float green, float blue) {
        Vec3 step = vehiclePosition.subtract(previousPosition);
        int segments = (int) step.length();
        Vec3 direction = step.normalize();
        for (Vec3 offset : offsets) {
            for (int i = 0; i <= segments; i++) {
                Vec3 position = positionTransform.apply(vehiclePosition.add(offset))
                        .subtract(direction.scale(i))
                        .subtract(movement);
                level.addParticle(new SmokeCloudOption(false, red, green, blue, red, green, blue,
                                0.5f, 0.1f, 1200, 0.3f, 5f, 0.01f), true,
                        position.x, position.y, position.z,
                        random.triangle(0, 0.1f),
                        random.triangle(0, 0.1f),
                        random.triangle(0, 0.1f));
            }
        }
    }

    public static void spawnMotorcycleDust(Level level, RandomSource random, Vec3 wheelPosition, Vec3 movement, boolean frontWheel) {
        level.addParticle(new DustSmokeOption(2.5f),
                wheelPosition.x - movement.x,
                wheelPosition.y + 0.1,
                wheelPosition.z - movement.z,
                movement.x * 0.1,
                0.02,
                movement.z * 0.1);
        if (random.nextFloat() < 0.5f) {
            return;
        }
        double horizontalSpeedScale = frontWheel ? 0.1 : 0.25;
        double verticalSpeed = frontWheel ? 0.02 : 0.1;
        level.addParticle(AllParticleTypes.DUST_STONE.get(),
                wheelPosition.x + random.nextDouble() * 0.1 - movement.x,
                wheelPosition.y + 0.1,
                wheelPosition.z + random.nextDouble() * 0.1 - movement.z,
                movement.x * horizontalSpeedScale + random.nextDouble() * 0.1,
                verticalSpeed,
                movement.z * horizontalSpeedScale + random.nextDouble() * 0.1);
    }

}
