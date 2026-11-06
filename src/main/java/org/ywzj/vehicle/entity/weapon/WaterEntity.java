package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.util.BulletHitResult;

public class WaterEntity extends AbstractSprayEntity {

    private static final double KNOCKBACK = 0.3;

    public WaterEntity(EntityType<? extends WaterEntity> type, Level level, ResourceLocation weaponId) {
        super(type, level, weaponId, ParticleTypes.BUBBLE_POP);
        name = Component.translatable("weapon.ywzj_vehicle.water_cannon");
        life = 60;
    }

    public WaterEntity(EntityType<? extends WaterEntity> type, Level level) {
        this(type, level, YwzjVehicle.modLocation("water_cannon"));
    }

    public WaterEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(AllEntities.WATER.get(), level);
    }

    @Override
    protected ClipContext createClipContext(Vec3 start, Vec3 end) {
        return new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
                return state.is(BlockTags.FIRE) ? Shapes.block() : super.getBlockShape(state, level, pos);
            }
        };
    }

    @Override
    protected void onSprayHitEntity(ServerLevel level, BulletHitResult hit) {
        hitEntity(level, hit.getEntity());
        splash(level, hit.getLocation());
    }

    @Override
    protected void onSprayHitBlock(ServerLevel level, BlockHitResult hit) {
        extinguishArea(level, hit.getBlockPos());
        splash(level, hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.05)));
    }

    private void extinguishArea(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.FIRE)) {
                if (!level.removeBlock(pos, false)) {
                    continue;
                }
                level.levelEvent(LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
            } else if (CampfireBlock.isLitCampfire(state)) {
                CampfireBlock.dowse(getOwner(), level, pos, state);
                level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
                level.levelEvent(LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
            } else if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, level, pos);
            } else {
                continue;
            }
            smoke(level, Vec3.atCenterOf(pos));
        }
    }

    private void hitEntity(ServerLevel level, Entity target) {
        if (target.isOnFire()) {
            target.clearFire();
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 0.7f, 1.6f);
            smoke(level, target.getBoundingBox().getCenter());
        }
        Vec3 direction = new Vec3(getDeltaMovement().x, 0, getDeltaMovement().z).normalize();
        if (direction.lengthSqr() < 1.0E-6) {
            direction = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ()).normalize();
        }
        if (target instanceof LivingEntity livingEntity) {
            livingEntity.knockback(KNOCKBACK, -direction.x, -direction.z);
        }
        target.hurtMarked = true;
    }

    private static void splash(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.BUBBLE_POP, pos.x, pos.y, pos.z, 24, 0.25, 0.2, 0.25, 0.08);
    }

    private static void smoke(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 8, 0.25, 0.2, 0.25, 0.025);
    }

}
