package org.ywzj.vehicle.mixin.common;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {

    @Accessor("entityMap")
    Int2ObjectMap<Object> ywzj$entityMap();

    /**
     * Recomputes which chunks the player is owed and queues the ones it is not already recorded as
     * having. Paired with resetting the tracking view to empty this is exactly what vanilla runs when
     * a player joins, so it re-sends the whole view through the normal path.
     */
    @Invoker("updateChunkTracking")
    void ywzj$updateChunkTracking(ServerPlayer player);

    @Invoker("getPlayerViewDistance")
    int ywzj$playerViewDistance(ServerPlayer player);

}
