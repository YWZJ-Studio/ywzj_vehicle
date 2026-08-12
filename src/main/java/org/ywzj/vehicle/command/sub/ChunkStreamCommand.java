package org.ywzj.vehicle.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import org.ywzj.vehicle.mixin.common.ChunkMapAccessor;
import org.ywzj.vehicle.stream.ChunkStreamDebug;
import org.ywzj.vehicle.stream.wakeup.VehicleWakeup;
import org.ywzj.vehicle.stream.wakeup.VehicleWakeupData;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class ChunkStreamCommand {

    private static final String NAME = "chunkstream";
    private static final String CATEGORY = "category";
    private static final String ENABLE = "enable";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal(NAME)
                .then(Commands.literal("status").executes(ChunkStreamCommand::status))
                .then(Commands.literal("sent")
                        .executes(context -> sent(context, 12))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 32))
                                .executes(context -> sent(context, IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("sleeping").executes(ChunkStreamCommand::sleeping))
                .then(Commands.literal("reset").executes(ChunkStreamCommand::reset))
                .then(Commands.literal("debug")
                        .then(Commands.argument(CATEGORY, StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    List<String> names = new ArrayList<>();
                                    names.add("all");
                                    for (ChunkStreamDebug.Category category : ChunkStreamDebug.Category.values()) {
                                        names.add(category.name().toLowerCase(Locale.ROOT));
                                    }
                                    return SharedSuggestionProvider.suggest(names, builder);
                                })
                                .then(Commands.argument(ENABLE, BoolArgumentType.bool())
                                        .executes(ChunkStreamCommand::debug))));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        for (String line : ChunkStreamDebug.status()) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * what ChunkMap believes the operator already has. A chunk it
     * records as delivered is never sent again, so anything marked tracked here and absent there is a
     * hole nothing will repair on its own.
     */
    private static int sent(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        PlayerChunkSender sender = player.connection.chunkSender;
        ChunkTrackingView view = player.getChunkTrackingView();
        ChunkPos body = player.chunkPosition();

        ChunkStreamDebug.report("sent | body {} radius {} | server view distance {} | tracking view {}",
                body, radius, ((ChunkMapAccessor) chunkMap).ywzj$playerViewDistance(player),
                view instanceof ChunkTrackingView.Positioned(ChunkPos center, int viewDistance)
                        ? center + " r" + viewDistance : "empty");
        ChunkStreamDebug.report("sent | # tracked+loaded  t tracked+not loaded  p queued to send"
                + "  . not tracked  B body");

        int tracked = 0;
        int stale = 0;
        int queued = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -radius; dx <= radius; dx++) {
                int x = body.x + dx;
                int z = body.z + dz;
                boolean inView = view.contains(x, z);
                boolean loaded = level.getChunkSource().getChunkNow(x, z) != null;
                boolean pending = sender.isPending(ChunkPos.asLong(x, z));
                if (inView) {
                    tracked++;
                    if (!loaded) {
                        stale++;
                    }
                }
                if (pending) {
                    queued++;
                }
                if (x == body.x && z == body.z) {
                    row.append('B');
                } else if (pending) {
                    row.append('p');
                } else if (!inView) {
                    row.append('.');
                } else {
                    row.append(loaded ? '#' : 't');
                }
            }
            ChunkStreamDebug.report("sent | {}", row);
        }
        String summary = tracked + " tracked, " + stale + " tracked-but-unloaded, " + queued + " queued to send";
        ChunkStreamDebug.report("sent | {}", summary);
        context.getSource().sendSuccess(() -> Component.literal("server chunk view -> logs (" + summary + ")"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int sleeping(CommandContext<CommandSourceStack> context) {
        VehicleWakeupData data = VehicleWakeupData.get(context.getSource().getServer());
        if (data.all().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("no vehicles registered for wakeup"), false);
            return Command.SINGLE_SUCCESS;
        }
        for (VehicleWakeupData.Entry entry : data.all()) {
            boolean loaded = VehicleWakeup.lookup(context.getSource().getServer(), entry.vehicleId()) != null
                    && context.getSource().getServer().getLevel(entry.dimension()) != null
                    && context.getSource().getServer().getLevel(entry.dimension()).getEntity(entry.vehicleId()) != null;
            String line = String.format(Locale.ROOT, "%s %s @ %.1f %.1f %.1f chunk %s [%s]",
                    ChunkStreamDebug.shortId(entry.vehicleId()), entry.dimension().location(),
                    entry.position().x, entry.position().y, entry.position().z, entry.chunk(),
                    loaded ? "loaded" : "sleeping");
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        ChunkStreamDebug.setOverride(null);
        context.getSource().sendSuccess(() -> Component.literal("chunk stream debug follows the config again"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int debug(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, CATEGORY).toUpperCase(Locale.ROOT);
        boolean enable = BoolArgumentType.getBool(context, ENABLE);
        EnumSet<ChunkStreamDebug.Category> categories = EnumSet.copyOf(ChunkStreamDebug.active());
        if (name.equals("ALL")) {
            categories = enable ? EnumSet.allOf(ChunkStreamDebug.Category.class)
                    : EnumSet.noneOf(ChunkStreamDebug.Category.class);
        } else {
            ChunkStreamDebug.Category category;
            try {
                category = ChunkStreamDebug.Category.valueOf(name);
            } catch (IllegalArgumentException e) {
                context.getSource().sendFailure(Component.literal("unknown category " + name));
                return 0;
            }
            if (enable) {
                categories.add(category);
            } else {
                categories.remove(category);
            }
        }
        ChunkStreamDebug.setOverride(categories);
        EnumSet<ChunkStreamDebug.Category> result = categories;
        context.getSource().sendSuccess(() -> Component.literal("chunk stream debug -> "
                + (result.isEmpty() ? "off" : result.toString())), true);
        return Command.SINGLE_SUCCESS;
    }

}
