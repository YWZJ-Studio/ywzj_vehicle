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
 * Hooked here rather than NeoForge block events because those only cover player-driven changes;
 * pistons, explosions, fluid spread, and world gen writes funnel through this method.
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
