package org.ywzj.vehicle.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.loading.FMLPaths;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.PhysicsTrace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Records and reports {@link PhysicsTrace}, for working out why a vehicle will not sit still.
 * <p>
 * Targets whatever the player is riding, or the nearest vehicle otherwise, so the usual sequence
 * is: park the thing, {@code start}, watch it misbehave for a few seconds, {@code report}.
 */
public class PhysicsTraceCommand {

    private static final String NAME = "physics";
    private static final String TICKS = "ticks";
    private static final double SEARCH_RADIUS = 32.0;

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal(NAME)
                .then(Commands.literal("start")
                        .executes(context -> start(context, 0))
                        .then(Commands.argument(TICKS, IntegerArgumentType.integer(1, PhysicsTrace.MAX_TICKS))
                                .executes(context -> start(context, IntegerArgumentType.getInteger(context, TICKS)))))
                .then(Commands.literal("stop").executes(PhysicsTraceCommand::stop))
                .then(Commands.literal("report").executes(PhysicsTraceCommand::report))
                .then(Commands.literal("dump").executes(PhysicsTraceCommand::dump));
    }

    private static int start(CommandContext<CommandSourceStack> context, int ticks)
            throws CommandSyntaxException {
        AbstractVehicle vehicle = target(context);
        if (vehicle == null) {
            return 0;
        }
        vehicle.setPhysicsTrace(new PhysicsTrace(vehicle, ticks));
        String limit = ticks > 0 ? " for " + ticks + " ticks" : " until stopped";
        context.getSource().sendSuccess(
                () -> Component.literal("tracing " + vehicle.getType().toShortString()
                        + " #" + vehicle.getId() + limit), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int stop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PhysicsTrace trace = traceOf(context);
        if (trace == null) {
            return 0;
        }
        trace.stop();
        context.getSource().sendSuccess(
                () -> Component.literal("trace stopped, " + trace.size() + " ticks kept"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int report(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PhysicsTrace trace = traceOf(context);
        if (trace == null) {
            return 0;
        }
        for (String line : trace.report()) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int dump(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PhysicsTrace trace = traceOf(context);
        if (trace == null) {
            return 0;
        }
        try {
            Path directory = FMLPaths.GAMEDIR.get().resolve("ywzj_vehicle_traces");
            Path ticks = trace.dump(directory);
            Path sweeps = trace.dumpSweeps(directory);
            context.getSource().sendSuccess(
                    () -> Component.literal("wrote " + trace.size() + " ticks to " + ticks), true);
            context.getSource().sendSuccess(
                    () -> Component.literal("wrote " + trace.sweepCount() + " substeps to " + sweeps),
                    true);
        } catch (IOException exception) {
            context.getSource().sendFailure(
                    Component.literal("could not write trace: " + exception.getMessage()));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static PhysicsTrace traceOf(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        AbstractVehicle vehicle = target(context);
        if (vehicle == null) {
            return null;
        }
        PhysicsTrace trace = vehicle.physicsTrace();
        if (trace == null) {
            context.getSource().sendFailure(Component.literal(
                    "that vehicle is not being traced — run /ywzj_vehicle physics start first"));
        }
        return trace;
    }

    /** The vehicle being ridden, or the nearest one, so this works whether driving or watching. */
    private static AbstractVehicle target(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (player.getVehicle() instanceof AbstractVehicle ridden) {
            return ridden;
        }
        AABB around = player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<AbstractVehicle> nearby = player.level()
                .getEntitiesOfClass(AbstractVehicle.class, around, Entity::isAlive);
        AbstractVehicle nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (AbstractVehicle vehicle : nearby) {
            double distance = vehicle.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = vehicle;
            }
        }
        if (nearest == null) {
            context.getSource().sendFailure(
                    Component.literal("no vehicle within " + (int) SEARCH_RADIUS + " blocks"));
        }
        return nearest;
    }

}
