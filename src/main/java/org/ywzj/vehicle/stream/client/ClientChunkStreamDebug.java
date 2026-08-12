package org.ywzj.vehicle.stream.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.ywzj.vehicle.stream.ChunkStreamDebug;
import org.ywzj.vehicle.stream.PinnedChunkCache;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientChunkStreamDebug {

    private static long ticks;

    private ClientChunkStreamDebug() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ticks++;
        if (ticks % 20 == 0) {
            ChunkStreamDebug.refresh();
        }
        if (!ChunkStreamDebug.on(ChunkStreamDebug.Category.CLIENT)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Entity vehicle = ClientDetachedView.viewedVehicle();
        if (minecraft.level == null) {
            ChunkStreamDebug.state(ChunkStreamDebug.Category.CLIENT, "client view", "no level");
            return;
        }
        if (vehicle == null) {
            ChunkStreamDebug.state(ChunkStreamDebug.Category.CLIENT, "client view", "body view");
            return;
        }
        ChunkPos pos = vehicle.chunkPosition();
        boolean loaded = minecraft.level.hasChunk(pos.x, pos.z);
        ChunkStreamDebug.state(ChunkStreamDebug.Category.CLIENT, "client view",
                "detached view on " + pos + " chunkLoaded=" + loaded);
        if (ticks % ChunkStreamDebug.heartbeatTicks() == 0) {
            int pinned = minecraft.level.getChunkSource() instanceof PinnedChunkCache cache
                    ? cache.ywzj$pinnedCount() : -1;
            ChunkStreamDebug.log(ChunkStreamDebug.Category.CLIENT,
                    "heartbeat: vehicle {} chunkLoaded={} loadedChunks={} rescuedChunks={} camera={}",
                    pos, loaded, minecraft.level.getChunkSource().getLoadedChunksCount(), pinned,
                    minecraft.options.getCameraType());
        }
    }

}
