package org.ywzj.vehicle.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.ywzj.vehicle.stream.ChunkStreamDebug;
import org.ywzj.vehicle.stream.PinnedChunkCache;
import org.ywzj.vehicle.stream.client.ClientDetachedView;
import org.ywzj.vehicle.stream.client.SodiumChunkProbe;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ChunkMapCommand {

    private static final String ROOT = "ywzj_chunkmap";
    private static final String RADIUS = "radius";

    private ChunkMapCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal(ROOT)
                .executes(context -> dump(context, 12))
                .then(Commands.argument(RADIUS, IntegerArgumentType.integer(1, 32))
                        .executes(context -> dump(context, IntegerArgumentType.getInteger(context, RADIUS)))));
    }

    private static int dump(CommandContext<CommandSourceStack> context, int radius) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return 0;
        }
        ChunkSource source = level.getChunkSource();
        PinnedChunkCache cache = source instanceof PinnedChunkCache pinned ? pinned : null;
        LongSet rescued = cache == null ? new LongOpenHashSet() : cache.ywzj$pinnedPositions();
        LongOpenHashSet ready = SodiumChunkProbe.readySet(level);

        Entity vehicle = ClientDetachedView.viewedVehicle();
        ChunkPos body = player.chunkPosition();
        ChunkPos camera = minecraft.cameraEntity == null ? body : minecraft.cameraEntity.chunkPosition();
        ChunkPos origin = vehicle == null ? body : vehicle.chunkPosition();

        header(level, cache, player, vehicle, body, camera, origin, ready, rescued, radius);

        int held = 0;
        int missing = 0;
        int untracked = 0;
        int ghost = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -radius; dx <= radius; dx++) {
                int x = origin.x + dx;
                int z = origin.z + dz;
                ChunkAccess chunk = source.getChunk(x, z, ChunkStatus.FULL, false);
                boolean rescuedHere = rescued.contains(ChunkPos.asLong(x, z));
                boolean drawable = ready == null || SodiumChunkProbe.isReady(ready, x, z);
                char mark;
                if (x == body.x && z == body.z) {
                    mark = 'B';
                } else if (x == camera.x && z == camera.z) {
                    mark = 'C';
                } else if (chunk == null) {
                    mark = drawable ? '!' : '.';
                } else if (rescuedHere) {
                    mark = drawable ? 'r' : 'R';
                } else {
                    mark = drawable ? '#' : 'X';
                }
                if (chunk != null) {
                    held++;
                    if (!drawable) {
                        untracked++;
                    }
                } else {
                    missing++;
                    if (drawable) {
                        ghost++;
                    }
                }
                row.append(mark);
            }
            ChunkStreamDebug.report("map | {}", row);
        }

        String summary = String.format(
                "%d held, %d missing, %d held-but-untracked, %d tracked-but-absent",
                held, missing, untracked, ghost);
        ChunkStreamDebug.report("map | {}", summary);
        context.getSource().sendSuccess(() -> Component.literal("chunk map -> logs (" + summary + ")"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void header(ClientLevel level, PinnedChunkCache cache, LocalPlayer player, Entity vehicle,
                               ChunkPos body, ChunkPos camera, ChunkPos origin, LongOpenHashSet ready,
                               LongSet rescued, int radius) {
        ChunkStreamDebug.report(
                "map | origin {} radius {} | body {} camera {} | vehicle {} | riding {}",
                origin, radius, body, camera,
                vehicle == null ? "none" : vehicle.chunkPosition(),
                player.getVehicle() == null ? "nothing" : player.getVehicle().getType().toShortString());
        ChunkStreamDebug.report(
                "map | cache centre {} storage radius {} | loaded {} rescued {} | renderer {} ready {}",
                cache == null ? "?" : cache.ywzj$viewCentre(),
                cache == null ? -1 : cache.ywzj$storageRadius(),
                level.getChunkSource().getLoadedChunksCount(), rescued.size(),
                SodiumChunkProbe.flavour(), ready == null ? -1 : ready.size());
        ChunkStreamDebug.report(
                "map | # held+drawable  X held+untracked  r rescued+drawable  R rescued+untracked"
                        + "  . absent  ! absent+tracked  B body  C camera");
        ChunkStreamDebug.report(
                "map | the outermost ring reads as untracked by design: a sodium-family renderer only"
                        + " marks a chunk drawable once all eight neighbours have arrived too");
    }

}
