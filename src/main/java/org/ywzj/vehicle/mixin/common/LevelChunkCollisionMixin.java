package org.ywzj.vehicle.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;

/**
 * Invalidates the vehicle collision snapshot for a section whenever its block data changes.
 * <p>
 * Hooked here rather than on the NeoForge block events because those only cover player-driven
 * placement and breaking. Everything that actually mutates chunk storage — pistons, explosions,
 * fluid spread, {@code /setblock}, world gen writes — funnels through this method.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkCollisionMixin {

    @Shadow
    @Final
    Level level;

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void ywzj$invalidateCollisionSnapshot(BlockPos pos, BlockState state, boolean isMoving,
                                                  CallbackInfoReturnable<BlockState> cir) {
        ChunkCollisionCache.invalidate(this.level, pos);
    }

}
