package org.ywzj.vehicle.util;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import org.ywzj.vehicle.all.AllConfigs;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VehicleExplosion {

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
    private final ObjectArrayList<BlockPos> toBlow = new ObjectArrayList<>();

    public VehicleExplosion(Level level, Entity source, Vec3 position, float radius, float damage) {
        this(level, source, position, radius, damage, AllConfigs.common.explosionDestroyBlocks.get(), AllConfigs.common.explosionDropBlocks.get());
    }

    public VehicleExplosion(Level level, Entity source, Vec3 position, float radius, float damage, boolean destroyBlocks) {
        this(level, source, position, radius, damage, destroyBlocks, AllConfigs.common.explosionDropBlocks.get());
    }

    public VehicleExplosion(Level level, Entity source, Vec3 position, float radius, float damage, boolean destroyBlocks, boolean dropBlocks) {
        this.level = level;
        this.source = source;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        this.radius = radius;
        this.damage = damage;
        this.destroyBlocks = destroyBlocks && AllConfigs.common.explosionDestroyBlocks.get();
        this.dropBlocks = dropBlocks && AllConfigs.common.explosionDropBlocks.get();
        this.damageSource = level.damageSources().explosion(source, null);
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
            ruin(vanillaExplosion);
            hurt(excludedEntities);
            effect();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void ruin(Explosion vanillaExplosion) {
        if (destroyBlocks) {
            Set<BlockPos> affectedBlocks = new HashSet<>();
            int resolution = 16;
            for (int xEdge = 0; xEdge < resolution; xEdge++) {
                for (int yEdge = 0; yEdge < resolution; yEdge++) {
                    for (int zEdge = 0; zEdge < resolution; zEdge++) {
                        if (xEdge == 0 || xEdge == resolution - 1 ||
                                yEdge == 0 || yEdge == resolution - 1 ||
                                zEdge == 0 || zEdge == resolution - 1) {
                            double dirX = (xEdge / 15F * 2 - 1);
                            double dirY = (yEdge / 15F * 2 - 1);
                            double dirZ = (zEdge / 15F * 2 - 1);
                            double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                            dirX /= length; dirY /= length; dirZ /= length;
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
                    }
                }
            }
            this.toBlow.addAll(affectedBlocks);
            ObjectArrayList<Pair<ItemStack, BlockPos>> dropsBuffer = new ObjectArrayList<>();
            Util.shuffle(this.toBlow, this.level.random);
            for (BlockPos pos : this.toBlow) {
                BlockState blockState = this.level.getBlockState(pos);
                if (blockState.isAir()) {
                    continue;
                }
                BlockPos immutablePos = pos.immutable();
                this.level.getProfiler().push("explosion_blocks");
                if (dropBlocks) {
                    if (blockState.canDropFromExplosion(this.level, pos, vanillaExplosion) && this.level instanceof ServerLevel serverLevel) {
                        BlockEntity blockEntity = blockState.hasBlockEntity() ? this.level.getBlockEntity(pos) : null;
                        LootParams.Builder lootParams = new LootParams.Builder(serverLevel)
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                                .withOptionalParameter(LootContextParams.THIS_ENTITY, this.source)
                                .withParameter(LootContextParams.EXPLOSION_RADIUS, this.radius);
                        blockState.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, false);
                        blockState.getDrops(lootParams).forEach(stack -> addBlockDrops(dropsBuffer, stack, immutablePos));
                    }
                }
                blockState.onBlockExploded(this.level, pos, vanillaExplosion);
                this.level.getProfiler().pop();
            }
            for (Pair<ItemStack, BlockPos> entry : dropsBuffer) {
                Block.popResource(this.level, entry.getSecond(), entry.getFirst());
            }
        }
    }

    private void hurt(List<Entity> excludedEntities) {
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            if (excludedEntities != null && excludedEntities.contains(entity)) {
                continue;
            }
            double distance = entity.position().distanceTo(new Vec3(x, y, z));
            if (distance > radius) {
                continue;
            }
            // 距离衰减
            double attenuation = 1.0 - (distance / radius);
            double damage = this.damage * attenuation * attenuation;
            if (entity instanceof LivingEntity livingEntity) {
                LivingHurtEvent hurtEvent = new LivingHurtEvent(livingEntity, damageSource, (float) damage);
                if (!MinecraftForge.EVENT_BUS.post(hurtEvent)) {
                    entity.hurt(damageSource, hurtEvent.getAmount());
                }
            } else {
                entity.hurt(damageSource, (float) damage);
            }
        }
    }

    private void effect() {
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            level.playSound(source, BlockPos.containing(new Vec3(x, y, z)), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 8f, 1f);
            for (ServerPlayer player : serverLevel.getPlayers(player -> player.distanceTo(source) < 256)) {
                serverLevel.sendParticles(
                        player,
                        ParticleTypes.EXPLOSION_EMITTER,
                        true,
                        x, y, z,
                        3,
                        0.5, 0.5, 0.5,
                        0.1
                );
            }
        }
    }

    private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> pDropPositionArray, ItemStack pStack, BlockPos pPos) {
        int i = pDropPositionArray.size();
        for(int j = 0; j < i; ++j) {
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

}
