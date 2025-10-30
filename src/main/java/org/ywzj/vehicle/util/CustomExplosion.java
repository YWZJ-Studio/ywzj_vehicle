package org.ywzj.vehicle.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import org.ywzj.vehicle.all.AllConfigs;

public class CustomExplosion {

    public static void explode(ServerLevel level, Entity source, Vec3 pos, float radius, float maxDamage) {
        RandomSource random = level.random;

        Explosion vanillaExplosion = new Explosion(level, source, pos.x, pos.y, pos.z, radius, false, Explosion.BlockInteraction.KEEP);
        ExplosionEvent.Start startEvent = new ExplosionEvent.Start(level, vanillaExplosion);
        if (MinecraftForge.EVENT_BUS.post(startEvent)) return;

        AABB box = new AABB(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius, pos.z + radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            double distance = entity.position().distanceTo(pos);
            if (distance > radius) continue;
            // 距离衰减
            double attenuation = 1.0 - (distance / radius);
            double damage = maxDamage * attenuation * attenuation;
            // 盔甲减伤
            damage = applyArmorReduction(entity, (float) damage);
            DamageSource dmgSource = level.damageSources().explosion(source, source);
            LivingHurtEvent hurtEvent = new LivingHurtEvent(entity, dmgSource, (float) damage);
            if (!MinecraftForge.EVENT_BUS.post(hurtEvent)) {
                entity.hurt(dmgSource, hurtEvent.getAmount());
            }
        }

        if (AllConfigs.common.explosionBreakBlocks.get()) {
            int minX = (int) Math.floor(pos.x - radius);
            int maxX = (int) Math.ceil(pos.x + radius);
            int minY = (int) Math.floor(pos.y - radius);
            int maxY = (int) Math.ceil(pos.y + radius);
            int minZ = (int) Math.floor(pos.z - radius);
            int maxZ = (int) Math.ceil(pos.z + radius);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos bpos = new BlockPos(x, y, z);
                        double dist = Math.sqrt(bpos.distSqr(BlockPos.containing(pos)));
                        if (dist > radius) continue;
                        // 距离越远，破坏概率指数衰减
                        double p = Math.exp(-dist * 0.1);
                        if (random.nextDouble() < p) {
                            BlockState state = level.getBlockState(bpos);
                            float destroySpeed = state.getDestroySpeed(level, bpos);
                            if (!state.isAir() && destroySpeed > 0 && destroySpeed < 50) {
                                level.destroyBlock(bpos, false);
                            }
                        }
                    }
                }
            }
        }

        level.playSound(source, BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 8f, 1f);
        ExplosionEvent.Detonate detonateEvent = new ExplosionEvent.Detonate(level, vanillaExplosion, level.getEntitiesOfClass(Entity.class, box));
        MinecraftForge.EVENT_BUS.post(detonateEvent);
    }

    private static double applyArmorReduction(LivingEntity entity, float damage) {
        // 获取总护甲与韧性
        float armor = entity.getArmorValue();
        float toughness = (float) entity.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue();

        float reduction = 2f + toughness / 4f;
        float effectiveArmor = Math.min(armor, 20f);
        float armorReduction = effectiveArmor / 25f;

        // 按 Minecraft 1.20.1 的公式：最终伤害 = damage * (1 - armor / (armor + 100))
        float finalDamage = damage * (1 - (armor / (armor + 100f)));
        return Math.max(finalDamage, 0f);
    }

}
