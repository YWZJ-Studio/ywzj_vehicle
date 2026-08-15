package org.ywzj.vehicle.vehicle.collision;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Lifecycle hooks for {@link ChunkCollisionCache}.
 * <p>
 * Per-block invalidation is handled by a {@code LevelChunk.setBlockState} mixin. These cover the
 * coarser events that mixin cannot see: a chunk whose storage is swapped out from under a
 * cached section key, and a level going away entirely.
 */
@EventBusSubscriber
public final class ChunkCollisionCacheEvents {

    private ChunkCollisionCacheEvents() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // A reloaded chunk reuses its section keys, so any snapshot left over from the previous
        // residency has to go even though no setBlockState was observed for it.
        if (event.getLevel() instanceof Level level) {
            ChunkCollisionCache.invalidateChunk(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            ChunkCollisionCache.invalidateChunk(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            ChunkCollisionCache.forget(level);
        }
    }

}
