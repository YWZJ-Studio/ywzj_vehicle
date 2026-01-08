package org.ywzj.vehicle.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.ywzj.vehicle.command.sub.DebugCommand;
import org.ywzj.vehicle.command.sub.SummonCommand;

public class RootCommand {

    private static final String ROOT_NAME = "ywzj_vehicle";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME)
                .requires((source -> source.hasPermission(2)));
        root.then(DebugCommand.get());
        root.then(SummonCommand.get());
        dispatcher.register(root);
    }

}
