package org.ywzj.vehicle.entity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.PlayMessages;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.util.BulletHitResult;

public class FireEntity extends AbstractSprayEntity {

    private static final int BURN_SECONDS = 5;

    public FireEntity(EntityType<? extends FireEntity> type, Level level, ResourceLocation weaponId) {
        super(type, level, weaponId, ParticleTypes.FLAME);
        name = Component.translatable("weapon.ywzj_vehicle.flamethrower");
        life = 60;
    }

    public FireEntity(EntityType<? extends FireEntity> type, Level level) {
        this(type, level, YwzjVehicle.modLocation("flamethrower"));
    }

    public FireEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(AllEntities.FIRE.get(), level);
    }

    @Override
    protected ClipContext createClipContext(Vec3 start, Vec3 end) {
        return new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this);
    }

    @Override
    protected void onSprayHitEntity(ServerLevel level, BulletHitResult hit) {
        Entity target = hit.getEntity();
        if (!target.fireImmune()) {
            target.setSecondsOnFire(BURN_SECONDS);
        }
        impact(level, hit.getLocation());
    }

    @Override
    protected void onSprayHitBlock(ServerLevel level, BlockHitResult hit) {
        if (level.getFluidState(hit.getBlockPos()).isEmpty()) {
            igniteBlock(level, hit);
            impact(level, hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.05)));
        }
    }

    private void igniteBlock(ServerLevel level, BlockHitResult hit) {
        Entity owner = getOwner();
        if (owner instanceof Mob && !ForgeEventFactory.getMobGriefingEvent(level, owner)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        GameEvent event;
        if (CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state)) {
            if (!level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, true))) {
                return;
            }
            event = GameEvent.BLOCK_CHANGE;
        } else {
            pos = pos.relative(hit.getDirection());
            if (!level.hasChunkAt(pos) || !BaseFireBlock.canBePlacedAt(level, pos, hit.getDirection())
                    || !level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos))) {
                return;
            }
            event = GameEvent.BLOCK_PLACE;
        }
        level.gameEvent(owner, event, pos);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    private static void impact(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 16, 0.2, 0.2, 0.2, 0.04);
    }

}
