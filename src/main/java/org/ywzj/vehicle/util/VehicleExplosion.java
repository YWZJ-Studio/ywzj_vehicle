package org.ywzj.vehicle.util;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.network.PacketDistributor;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllDamageTypes;
import org.ywzj.vehicle.client.handler.FirstPersonHandler;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerVehicleExplosion;
import org.ywzj.vehicle.particle.SmokeCloudOption;

import java.util.*;

public class VehicleExplosion {

    private static final float BATCHED_DESTRUCTION_RADIUS_THRESHOLD = 32.0F;
    private static final int MAX_RAYS_PER_TICK = 1024;
    private static final int RAYS_PER_EXPLOSION_SLICE = 256;
    private static final int MAX_BLOCKS_PER_TICK = 1024;
    private static final int BLOCKS_PER_EXPLOSION_SLICE = 256;
    private static final Map<ServerLevel, Deque<QueuedExplosion>> PENDING_DESTRUCTIONS = new HashMap<>();
    private final Level level;
    private final Entity source;
    private final double x;
    private final double y;
    private final double z;
    private final float radius;
    private final float damage;
    private final boolean destroyBlocks;
    private final boolean dropBlocks;
    private final DamageSource damageSource;
    private final ExplosionDamageCalculator damageCalculator;

    public VehicleExplosion(Level level, Entity source, Entity entity, Vec3 position, float radius, float damage) {
        this(level, source, entity, position, radius, damage, AllConfigs.common.canDestroyBlock.get(), AllConfigs.common.explosionDropBlock.get());
    }

    public VehicleExplosion(Level level, Entity source, Entity entity, Vec3 position, float radius, float damage, boolean destroyBlocks) {
        this(level, source, entity, position, radius, damage, destroyBlocks && AllConfigs.common.canDestroyBlock.get(), AllConfigs.common.explosionDropBlock.get());
    }

    public VehicleExplosion(Level level, Entity source, Entity entity, Vec3 position, float radius, float damage, boolean destroyBlocks, boolean dropBlocks) {
        this.level = level;
        this.source = source;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        this.radius = radius;
        this.damage = damage;
        this.destroyBlocks = destroyBlocks && AllConfigs.common.canDestroyBlock.get();
        this.dropBlocks = dropBlocks && AllConfigs.common.explosionDropBlock.get();
        this.damageSource = AllDamageTypes.Sources.explosion(level.registryAccess(), entity, source, position);
        this.damageCalculator = new EntityBasedExplosionDamageCalculator(source);
    }

    public void explode() {
        explode(null);
    }

    public void explode(List<Entity> excludedEntities) {
        Explosion vanillaExplosion = new Explosion(level, source, x, y, z, radius, false, Explosion.BlockInteraction.KEEP);
        ExplosionEvent.Start startEvent = new ExplosionEvent.Start(level, vanillaExplosion);
        if (MinecraftForge.EVENT_BUS.post(startEvent)) {
            return;
        }
        try {
            hurt(excludedEntities);
            if (!level.isClientSide()) {
                ServerVehicleExplosion serverVehicleExplosion = new ServerVehicleExplosion(source == null ? -1 : source.getId(), x, y, z, radius);
                ServerLevel serverLevel = (ServerLevel) level;
                for (ServerPlayer player : serverLevel.getPlayers(player -> player.distanceToSqr(x, y, z) < 1024 * 1024)) {
                    Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), serverVehicleExplosion);
                }
                if (destroyBlocks) {
                    if (shouldBatchBlockDestruction()) {
                        enqueueExplosionWork(serverLevel, vanillaExplosion);
                    } else {
                        ObjectArrayList<BlockPos> affectedBlocks = collectAffectedBlocks(vanillaExplosion);
                        if (!affectedBlocks.isEmpty()) {
                            destroyBlocksImmediately(vanillaExplosion, affectedBlocks);
                        }
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private boolean shouldBatchBlockDestruction() {
        return destroyBlocks && radius > BATCHED_DESTRUCTION_RADIUS_THRESHOLD && level instanceof ServerLevel;
    }

    private ObjectArrayList<BlockPos> collectAffectedBlocks(Explosion vanillaExplosion) {
        ExplosionCollectionTask collectionTask = new ExplosionCollectionTask(level, vanillaExplosion, damageCalculator, x, y, z, radius);
        while (!collectionTask.isFinished()) {
            collectionTask.processRays(RAYS_PER_EXPLOSION_SLICE);
        }
        return collectionTask.finish(level.random);
    }

    private void enqueueExplosionWork(ServerLevel serverLevel, Explosion vanillaExplosion) {
        PENDING_DESTRUCTIONS.computeIfAbsent(serverLevel, ignored -> new ArrayDeque<>())
                .addLast(QueuedExplosion.collecting(serverLevel, vanillaExplosion, source, radius, dropBlocks,
                        new ExplosionCollectionTask(level, vanillaExplosion, damageCalculator, x, y, z, radius)));
    }

    private void destroyBlocksImmediately(Explosion vanillaExplosion, ObjectArrayList<BlockPos> affectedBlocks) {
        ObjectArrayList<Pair<ItemStack, BlockPos>> dropsBuffer = new ObjectArrayList<>();
        for (BlockPos pos : affectedBlocks) {
            destroyOneBlock(level, source, radius, dropBlocks, vanillaExplosion, pos, dropsBuffer);
        }
        flushDrops(level, dropsBuffer);
    }

    public static void tick(ServerLevel level) {
        Deque<QueuedExplosion> queue = PENDING_DESTRUCTIONS.get(level);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        int remainingRayBudget = MAX_RAYS_PER_TICK;
        int remainingBlockBudget = MAX_BLOCKS_PER_TICK;
        while (!queue.isEmpty()) {
            QueuedExplosion queuedExplosion = queue.pollFirst();
            if (queuedExplosion.isCollecting()) {
                int processedRays = queuedExplosion.processCollection(Math.min(RAYS_PER_EXPLOSION_SLICE, remainingRayBudget));
                remainingRayBudget -= processedRays;
            } else {
                int processedBlocks = queuedExplosion.processDestruction(Math.min(BLOCKS_PER_EXPLOSION_SLICE, remainingBlockBudget));
                remainingBlockBudget -= processedBlocks;
            }
            if (queuedExplosion.isFinished()) {
                queuedExplosion.flushDrops();
            } else {
                queue.addLast(queuedExplosion);
            }
            if ((remainingRayBudget <= 0 && remainingBlockBudget <= 0) || queuedExplosion.madeNoProgressThisTick()) {
                break;
            }
        }
        if (queue.isEmpty()) {
            PENDING_DESTRUCTIONS.remove(level);
        }
    }

    private static int destroyOneBlock(Level level, Entity source, float radius, boolean dropBlocks, Explosion vanillaExplosion,
                                       BlockPos pos, ObjectArrayList<Pair<ItemStack, BlockPos>> dropsBuffer) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) {
            return 0;
        }
        BlockPos immutablePos = pos.immutable();
        level.getProfiler().push("explosion_blocks");
        try {
            if (dropBlocks && blockState.canDropFromExplosion(level, pos, vanillaExplosion) && level instanceof ServerLevel serverLevel) {
                BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                LootParams.Builder lootParams = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, source)
                        .withParameter(LootContextParams.EXPLOSION_RADIUS, radius);
                blockState.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, false);
                blockState.getDrops(lootParams).forEach(stack -> addBlockDrops(dropsBuffer, stack, immutablePos));
            }
            blockState.onBlockExploded(level, pos, vanillaExplosion);
            return 1;
        } finally {
            level.getProfiler().pop();
        }
    }

    private static void flushDrops(Level level, ObjectArrayList<Pair<ItemStack, BlockPos>> dropsBuffer) {
        for (Pair<ItemStack, BlockPos> entry : dropsBuffer) {
            Block.popResource(level, entry.getSecond(), entry.getFirst());
        }
        dropsBuffer.clear();
    }

    private void hurt(List<Entity> excludedEntities) {
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            if (excludedEntities != null && excludedEntities.contains(entity)) {
                continue;
            }
            if (entity instanceof ItemEntity) {
                continue;
            }
            if (entity instanceof Projectile projectile && projectile.getOwner() == damageSource.getEntity()) {
                continue;
            }
            Vec3 position = new Vec3(x, y, z);
            double distance = entity.position().distanceTo(position);
            if (distance > radius) {
                continue;
            }
            double attenuation = 1.0 - 0.5 * (distance / radius);
            double damage = this.damage * attenuation;
            if (entity instanceof LivingEntity livingEntity) {
                LivingHurtEvent hurtEvent = new LivingHurtEvent(livingEntity, damageSource, (float) damage);
                if (!MinecraftForge.EVENT_BUS.post(hurtEvent)) {
                    EntityUtil.hurt(damageSource, entity, hurtEvent.getAmount());
                }
            } else {
                EntityUtil.hurt(damageSource, entity, (float) damage);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void effect(ServerVehicleExplosion serverVehicleExplosion) {
        if (serverVehicleExplosion == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        Player player = minecraft.player;
        double x = serverVehicleExplosion.x();
        double y = serverVehicleExplosion.y();
        double z = serverVehicleExplosion.z();
        float radius = serverVehicleExplosion.radius();

        if (radius <= 2) {
            smallExplosionEffect(level, player, x, y, z);
        } else if (radius <= 8) {
            mediumExplosionEffect(level, player, x, y, z);
        } else if (radius <= 32) {
            largeExplosionEffect(level, player, x, y, z, radius);
        } else {
            nuclearExplosionEffect(level, player, x, y, z, radius);
        }

        FirstPersonHandler.addExplosionShake(new Vec3(x, y, z), radius);
    }

    @OnlyIn(Dist.CLIENT)
    private static void smallExplosionEffect(Level level, Player player, double x, double y, double z) {
        level.playSound(player, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 1.0f);
        level.addParticle(ParticleTypes.FLASH, true, x, y, z, 0, 0, 0);
        addParticles(level, ParticleTypes.EXPLOSION, x, y, z, 2, 0, 0.02, 0, 0);
        addParticlesOnSolid(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 2, 0.1, 0.1, 0.1, 0.02);
        addParticles(level, ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.02);
    }

    @OnlyIn(Dist.CLIENT)
    private static void mediumExplosionEffect(Level level, Player player, double x, double y, double z) {
        level.playSound(player, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0f, 0.7f);
        addParticles(level, ParticleTypes.FLASH, x, y + 0.5, z, 8, 2, 2, 2, 0);
        addParticles(level, ParticleTypes.LARGE_SMOKE, x, y + 1, z, 10, 16, 1, 16, 0.01);
        addParticlesOnSolid(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 40, 16, 0.05, 16, 0);
        for (int i = 0; i < 40; i++) {
            Vec3 v = randomHemisphereDir(level).scale(0.2);
            level.addParticle(new SmokeCloudOption(1f, 0.35f + level.random.nextFloat() * 0.15f, 0f, 6, 2.0f, -0.01f),
                    true, x, y + 0.5, z, v.x, v.y, v.z);
        }
        if (level.getBlockState(BlockPos.containing(x, y, z)).is(net.minecraft.world.level.block.Blocks.WATER)) {
            addParticles(level, ParticleTypes.CLOUD, x, y + 3, z, 20, 1, 3, 1, 0.01);
            addParticles(level, ParticleTypes.FALLING_WATER, x, y + 3, z, 50, 1.5, 4, 1.5, 1);
            addParticles(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 60, 3, 0.5, 3, 0.1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void largeExplosionEffect(Level level, Player player, double x, double y, double z, double radius) {
        level.playSound(player, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 8.0f, 0.5f);
        addParticles(level, ParticleTypes.FLASH, x, y + 3, z, 60, radius / 2, 5, radius / 2, 0);
        addParticlesOnSolid(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.25, z, 120, radius, 0.05, radius, 0);
        for (int i = 0; i < 200; i++) {
            Vec3 v = randomHemisphereDir(level).scale(1.0 + level.random.nextDouble() * 0.5);
            float heat = level.random.nextFloat();
            level.addParticle(new SmokeCloudOption(1f, 0.25f + heat * 0.25f, 0f, 100, 4.5f + heat * 2, -0.003f),
                    true, x, y + 1, z, v.x * 2, v.y, v.z * 2);
        }
        spawnExplosionRing(level, x, y, z, 4, 200, 4.0, 0.5, 0.4, 1.0f, 16, 3.0f);
        if (level.getBlockState(BlockPos.containing(x, y, z)).is(net.minecraft.world.level.block.Blocks.WATER)) {
            addParticles(level, ParticleTypes.CLOUD, x, y + 3, z, 200, 4, 8, 4, 0.01);
            addParticles(level, ParticleTypes.FALLING_WATER, x, y + 3, z, 500, 5, 10, 5, 1);
            addParticles(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 350, 8, 1, 8, 0.1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void nuclearExplosionEffect(Level level, Player player, double x, double y, double z, double radius) {
        double cappedRadius = Math.min(radius, 160.0);
        double stemHeight = Math.min(28.0 + cappedRadius * 0.35, 72.0);
        double capRadius = Math.min(12.0 + cappedRadius * 0.45, 60.0);
        int flashCount = Math.min(180, 80 + (int) (cappedRadius * 1.2));
        int stemCount = Math.min(420, 180 + (int) (cappedRadius * 1.4));
        int capCount = Math.min(720, 320 + (int) (cappedRadius * 2.3));
        int falloutCount = Math.min(260, 120 + (int) cappedRadius);

        level.playSound(player, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 14.0f, 0.35f);
        addParticles(level, ParticleTypes.FLASH, x, y + 4, z, flashCount, cappedRadius * 0.45, 8, cappedRadius * 0.45, 0);
        addParticlesOnSolid(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, 180, cappedRadius * 1.2, 0.2, cappedRadius * 1.2, 0.01);
        addParticles(level, ParticleTypes.LARGE_SMOKE, x, y + 1.0, z, 90, cappedRadius * 0.9, 1.2, cappedRadius * 0.9, 0.02);

        spawnExplosionRing(level, x, y, z, 5, 260, 8, 0.8, 0.55, 0.95f, 100, 4.5f);
        spawnMushroomStem(level, x, y - 16, z, stemHeight, capRadius * 0.22, stemCount);
        spawnMushroomCap(level, x, y + stemHeight, z, capRadius, capCount);
        spawnMushroomFallout(level, x, y + stemHeight * 0.82, z, capRadius, falloutCount);

        if (level.getBlockState(BlockPos.containing(x, y, z)).is(net.minecraft.world.level.block.Blocks.WATER)) {
            addParticles(level, ParticleTypes.CLOUD, x, y + 8, z, 260, 8, 12, 8, 0.02);
            addParticles(level, ParticleTypes.FALLING_WATER, x, y + 5, z, 700, 8, 16, 8, 1);
            addParticles(level, ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 500, 10, 2, 10, 0.15);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnExplosionRing(Level level, double x, double y, double z, int layers, int segments,
                                           double baseSpeed, double baseYOffset, double yStep,
                                           float baseBrightness, int baseLife, float baseSize) {
        for (int h = 0; h < layers; h++) {
            double waveSpeed = Math.max(1.2, baseSpeed - h * 0.35);
            double yOff = baseYOffset + h * yStep;
            float brightness = Math.max(0.35f, baseBrightness - h * 0.08f);
            int life = Math.max(8, baseLife - h * 2);
            float size = Math.max(1.5f, baseSize - h * 0.35f);
            for (int i = 0; i < segments; i++) {
                double angle = 2 * Math.PI * i / segments;
                level.addParticle(new SmokeCloudOption(brightness, brightness, brightness, 0.12f, 0.12f, 0.12f, life, size, 0f),
                        true, x, y + yOff, z, Math.cos(angle) * waveSpeed, 0.02, Math.sin(angle) * waveSpeed);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnMushroomStem(Level level, double x, double y, double z, double stemHeight, double stemRadius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radial = stemRadius * Math.sqrt(level.random.nextDouble());
            double px = x + Math.cos(angle) * radial;
            double pz = z + Math.sin(angle) * radial;
            double py = y + stemHeight * level.random.nextDouble();
            double vx = Math.cos(angle) * (0.03 + level.random.nextDouble() * 0.08) + (level.random.nextDouble() - 0.5) * 0.02;
            double vz = Math.sin(angle) * (0.03 + level.random.nextDouble() * 0.08) + (level.random.nextDouble() - 0.5) * 0.02;
            double vy = 0.18 + (1.0 - (py - y) / stemHeight) * 0.45 + level.random.nextDouble() * 0.08;
            float heat = level.random.nextFloat();
            level.addParticle(new SmokeCloudOption(
                            0.45f + heat * 0.35f,
                            0.24f + heat * 0.12f,
                            0.12f,
                            0.08f + heat * 0.12f,
                            0.08f + heat * 0.10f,
                            0.08f + heat * 0.10f,
                            70 + level.random.nextInt(40),
                            5.0f + heat * 2.8f,
                            -0.004f),
                    true, px, py, pz, vx, vy, vz);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnMushroomCap(Level level, double x, double y, double z, double capRadius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radial = capRadius * Math.sqrt(level.random.nextDouble());
            double rimBias = 0.45 + 0.55 * level.random.nextDouble();
            double px = x + Math.cos(angle) * radial;
            double pz = z + Math.sin(angle) * radial;
            double py = y + (level.random.nextDouble() - 0.5) * capRadius * 0.35 + Math.max(0, 1.0 - radial / capRadius) * capRadius * 0.22;
            double outward = 0.12 + rimBias * 0.22;
            double vx = Math.cos(angle) * outward + (level.random.nextDouble() - 0.5) * 0.04;
            double vz = Math.sin(angle) * outward + (level.random.nextDouble() - 0.5) * 0.04;
            double vy = 0.06 + Math.max(0, 1.0 - radial / capRadius) * 0.18 + (level.random.nextDouble() - 0.5) * 0.04;
            float heat = level.random.nextFloat();
            float startR = 0.45f + heat * 0.45f;
            float startG = 0.24f + heat * 0.18f;
            float startB = 0.12f + heat * 0.04f;
            float end = 0.06f + heat * 0.08f;
            level.addParticle(new SmokeCloudOption(startR, startG, startB, end, end, end, 110 + level.random.nextInt(50), 7.0f + heat * 4.5f, -0.002f),
                    true, px, py, pz, vx, vy, vz);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnMushroomFallout(Level level, double x, double y, double z, double capRadius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radial = capRadius * (0.6 + level.random.nextDouble() * 0.55);
            double px = x + Math.cos(angle) * radial;
            double pz = z + Math.sin(angle) * radial;
            double py = y + (level.random.nextDouble() - 0.5) * capRadius * 0.3;
            double vx = Math.cos(angle) * (0.03 + level.random.nextDouble() * 0.06);
            double vz = Math.sin(angle) * (0.03 + level.random.nextDouble() * 0.06);
            double vy = -0.02 - level.random.nextDouble() * 0.05;
            float shade = 0.14f + level.random.nextFloat() * 0.16f;
            level.addParticle(new SmokeCloudOption(shade, shade, shade, 0.02f, 0.02f, 0.02f, 90 + level.random.nextInt(40), 4.5f + level.random.nextFloat() * 2.5f, 0.002f),
                    true, px, py, pz, vx, vy, vz);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static Vec3 randomHemisphereDir(Level level) {
        return new Vec3(
                level.random.nextDouble() - 0.5,
                level.random.nextDouble() * 0.6,
                level.random.nextDouble() - 0.5
        ).normalize();
    }

    @OnlyIn(Dist.CLIENT)
    private static void addParticles(Level level, net.minecraft.core.particles.ParticleOptions particle,
                                     double x, double y, double z,
                                     int count, double xSpread, double ySpread, double zSpread, double speed) {
        for (int i = 0; i < count; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 2 * xSpread;
            double oy = (level.random.nextDouble() - 0.5) * 2 * ySpread;
            double oz = (level.random.nextDouble() - 0.5) * 2 * zSpread;
            level.addParticle(particle, true, x + ox, y + oy, z + oz, ox * speed, oy * speed + 0.02, oz * speed);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void addParticlesOnSolid(Level level, net.minecraft.core.particles.ParticleOptions particle,
                                            double x, double y, double z,
                                            int count, double xSpread, double ySpread, double zSpread, double speed) {
        for (int i = 0; i < count; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 2 * xSpread;
            double oy = (level.random.nextDouble() - 0.5) * 2 * ySpread;
            double oz = (level.random.nextDouble() - 0.5) * 2 * zSpread;
            double px = x + ox;
            double py = y + oy;
            double pz = z + oz;
            if (level.getBlockState(BlockPos.containing(px, py, pz).below()).isSolid()) {
                level.addParticle(particle, true, px, py, pz, ox * speed, oy * speed + 0.02, oz * speed);
            }
        }
    }

    private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> pDropPositionArray, ItemStack pStack, BlockPos pPos) {
        int i = pDropPositionArray.size();
        for (int j = 0; j < i; ++j) {
            Pair<ItemStack, BlockPos> pair = pDropPositionArray.get(j);
            ItemStack itemstack = pair.getFirst();
            if (ItemEntity.areMergable(itemstack, pStack)) {
                ItemStack itemstack1 = ItemEntity.merge(itemstack, pStack, 16);
                pDropPositionArray.set(j, Pair.of(itemstack1, pair.getSecond()));
                if (pStack.isEmpty()) {
                    return;
                }
            }
        }
        pDropPositionArray.add(Pair.of(pStack, pPos));
    }

    private static class ExplosionCollectionTask {

        private final Level level;
        private final Explosion vanillaExplosion;
        private final ExplosionDamageCalculator damageCalculator;
        private final double x;
        private final double y;
        private final double z;
        private final float radius;
        private final int resolution;
        private final double edgeDivisor;
        private final int totalSteps;
        private final Set<BlockPos> affectedBlocks = new HashSet<>();
        private int currentStep;

        private ExplosionCollectionTask(Level level, Explosion vanillaExplosion, ExplosionDamageCalculator damageCalculator,
                                        double x, double y, double z, float radius) {
            this.level = level;
            this.vanillaExplosion = vanillaExplosion;
            this.damageCalculator = damageCalculator;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.resolution = Math.max(16, Math.min(48, (int) Math.ceil(radius / 2.0F)));
            this.edgeDivisor = this.resolution - 1.0;
            this.totalSteps = this.resolution * this.resolution * this.resolution;
        }

        private int processRays(int limit) {
            int processed = 0;
            while (processed < limit && currentStep < totalSteps) {
                int xEdge = currentStep / (resolution * resolution);
                int yEdge = (currentStep / resolution) % resolution;
                int zEdge = currentStep % resolution;
                currentStep++;
                if (xEdge != 0 && xEdge != resolution - 1
                        && yEdge != 0 && yEdge != resolution - 1
                        && zEdge != 0 && zEdge != resolution - 1) {
                    continue;
                }
                processRay(xEdge, yEdge, zEdge);
                processed++;
            }
            return processed;
        }

        private void processRay(int xEdge, int yEdge, int zEdge) {
            double dirX = (xEdge / edgeDivisor) * 2.0 - 1.0;
            double dirY = (yEdge / edgeDivisor) * 2.0 - 1.0;
            double dirZ = (zEdge / edgeDivisor) * 2.0 - 1.0;
            double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (length < 1.0E-6) {
                return;
            }
            dirX /= length;
            dirY /= length;
            dirZ /= length;
            float energy = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
            double rayX = this.x;
            double rayY = this.y;
            double rayZ = this.z;
            while (energy > 0) {
                BlockPos currentPos = BlockPos.containing(rayX, rayY, rayZ);
                if (!this.level.isInWorldBounds(currentPos)) {
                    break;
                }
                BlockState state = this.level.getBlockState(currentPos);
                FluidState fluid = this.level.getFluidState(currentPos);
                Optional<Float> resistance = this.damageCalculator.getBlockExplosionResistance(vanillaExplosion, this.level, currentPos, state, fluid);
                if (resistance.isPresent()) {
                    energy -= (resistance.get() + 0.3F) * 0.3F;
                }
                if (energy > 0 && this.damageCalculator.shouldBlockExplode(vanillaExplosion, this.level, currentPos, state, energy)) {
                    affectedBlocks.add(currentPos);
                }
                rayX += dirX * 0.3F;
                rayY += dirY * 0.3F;
                rayZ += dirZ * 0.3F;
                energy -= 0.225F;
            }
        }

        private boolean isFinished() {
            return currentStep >= totalSteps;
        }

        private ObjectArrayList<BlockPos> finish(net.minecraft.util.RandomSource random) {
            ObjectArrayList<BlockPos> affected = new ObjectArrayList<>();
            affected.addAll(affectedBlocks);
            Util.shuffle(affected, random);
            return affected;
        }
    }

    private static class QueuedExplosion {

        private final ServerLevel level;
        private final Explosion vanillaExplosion;
        private final ObjectArrayList<Pair<ItemStack, BlockPos>> dropsBuffer = new ObjectArrayList<>();
        private final boolean dropBlocks;
        private final float radius;
        private final Entity source;
        private ExplosionCollectionTask collectionTask;
        private ObjectArrayList<BlockPos> remainingBlocks;
        private int lastProgress;

        private QueuedExplosion(ServerLevel level, Explosion vanillaExplosion, Entity source, float radius, boolean dropBlocks,
                                ExplosionCollectionTask collectionTask, ObjectArrayList<BlockPos> remainingBlocks) {
            this.level = level;
            this.vanillaExplosion = vanillaExplosion;
            this.dropBlocks = dropBlocks;
            this.radius = radius;
            this.source = source;
            this.collectionTask = collectionTask;
            this.remainingBlocks = remainingBlocks;
        }

        private static QueuedExplosion collecting(ServerLevel level, Explosion vanillaExplosion, Entity source, float radius,
                                                  boolean dropBlocks, ExplosionCollectionTask collectionTask) {
            return new QueuedExplosion(level, vanillaExplosion, source, radius, dropBlocks, collectionTask, null);
        }

        private boolean isCollecting() {
            return collectionTask != null;
        }

        private int processCollection(int limit) {
            if (collectionTask == null || limit <= 0) {
                lastProgress = 0;
                return 0;
            }
            int processed = collectionTask.processRays(limit);
            if (collectionTask.isFinished()) {
                remainingBlocks = collectionTask.finish(level.random);
                collectionTask = null;
            }
            lastProgress = processed;
            return processed;
        }

        private int processDestruction(int limit) {
            if (remainingBlocks == null || limit <= 0) {
                lastProgress = 0;
                return 0;
            }
            int processed = 0;
            while (processed < limit && !remainingBlocks.isEmpty()) {
                BlockPos pos = remainingBlocks.remove(remainingBlocks.size() - 1);
                processed += destroyOneBlock(level, source, radius, dropBlocks, vanillaExplosion, pos, dropsBuffer);
            }
            lastProgress = processed;
            return processed;
        }

        private boolean madeNoProgressThisTick() {
            return lastProgress <= 0;
        }

        private boolean isFinished() {
            return collectionTask == null && (remainingBlocks == null || remainingBlocks.isEmpty());
        }

        private void flushDrops() {
            VehicleExplosion.flushDrops(level, dropsBuffer);
        }

    }

}
