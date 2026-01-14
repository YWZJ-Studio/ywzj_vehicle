package org.ywzj.vehicle.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SummonCommand {

    private static final String SUMMON_NAME = "summon";
    private static final String VEHICLE_ID_NAME = "vehicle_id";
    private static final String VEHICLE_DISPLAY_ID_NAME = "vehicle_display_id";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal(SUMMON_NAME)
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.argument(VEHICLE_ID_NAME, ResourceLocationArgument.id())
                        .suggests(SummonCommand::suggestVehicleIds)
                        // 分支 1: 仅输入 vehicle_id
                        .executes(SummonCommand::setValue)
                        // 分支 2: 输入 vehicle_id + vehicle_display_id
                        .then(Commands.argument(VEHICLE_DISPLAY_ID_NAME, ResourceLocationArgument.id())
                                .suggests(SummonCommand::suggestVehicleDisplayIds)
                                .executes(SummonCommand::setValue)
                        )
                );
    }

    private static CompletableFuture<Suggestions> suggestVehicleIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(CommonAssetsManager.vehicleDataManager().getVehicleData().keySet(), builder);
    }

    private static CompletableFuture<Suggestions> suggestVehicleDisplayIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(ClientAssetsManager.INSTANCE.getVehicleDisplays().keySet(), builder);
    }

    private static int setValue(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            ResourceLocation vehicleId = context.getArgument(VEHICLE_ID_NAME, ResourceLocation.class);
            Level level = serverPlayer.level();
            Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId);
            if (vehicleDataOptional.isPresent()) {
                AbstractVehicle vehicle = vehicleDataOptional.get().construct(level, serverPlayer.position(), 0, serverPlayer.getYRot());
                try {
                    ResourceLocation displayId = context.getArgument(VEHICLE_DISPLAY_ID_NAME, ResourceLocation.class);
                    vehicle.setDisplayId(displayId);
                } catch (IllegalArgumentException ignore) {}
                level.addFreshEntity(vehicle);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

}
