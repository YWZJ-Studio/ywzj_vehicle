package org.ywzj.vehicle.vehicle.collision;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Lifecycle hooks for ChunkCollisionCache. Per-block invalidation is handled by a
 * LevelChunk.setBlockState mixin; these cover chunk and level load/unload events.
 */
@EventBusSubscriber
public final class ChunkCollisionCacheEvents {

    private ChunkCollisionCacheEvents() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Reloaded chunks reuse their section keys, so stale snapshots must be cleared.
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
            // Also clean up CarrierDecks; level lists holding vehicles keep everything alive.
            org.ywzj.vehicle.vehicle.parenting.CarrierDecks.forget(level);
        }
    }

}
