package org.ywzj.vehicle.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SummonCommand {

    private static final String SUMMON_NAME = "summon";
    private static final String CUSTOM_ID_NAME = "custom_id";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> summonCommand = Commands.literal(SUMMON_NAME);
        RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> summon = Commands.argument(CUSTOM_ID_NAME, ResourceLocationArgument.id()).suggests(SummonCommand::suggestCustomIds);
        summonCommand.then(summon.executes(SummonCommand::setValue));
        return summonCommand;
    }

    private static CompletableFuture<Suggestions> suggestCustomIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(CommonAssetsManager.vehicleDataManager().getVehicleData().keySet(), builder);
    }

    private static int setValue(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            ResourceLocation customId = context.getArgument(CUSTOM_ID_NAME, ResourceLocation.class);
            Level level = serverPlayer.level();
            Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(customId);
            if (vehicleDataOptional.isPresent()) {
                Entity vehicle = vehicleDataOptional.get().construct(level, serverPlayer.position(), 0, serverPlayer.getYRot());
                level.addFreshEntity(vehicle);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

}
