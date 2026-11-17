package org.ywzj.vehicle.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.resource.VehiclePackLoader;
import org.ywzj.vehicle.resource.VehicleTabOrder;
import org.ywzj.vehicle.vehicle.structure.OBB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SummonCommand {

    private static final String SUMMON_NAME = "summon";
    private static final String VEHICLE_ID_NAME = "vehicle_id";
    private static final String VEHICLE_DISPLAY_ID_NAME = "vehicle_display_id";
    private static final int COLUMNS = 9;
    private static final double GAP = 8;

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        var command = Commands.literal(SUMMON_NAME)
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.argument(VEHICLE_ID_NAME, ResourceLocationArgument.id())
                        .suggests(SummonCommand::suggestVehicleIds)
                        .executes(SummonCommand::setValue)
                        .then(Commands.argument(VEHICLE_DISPLAY_ID_NAME, ResourceLocationArgument.id())
                                .suggests(SummonCommand::suggestVehicleDisplayIds)
                                .executes(SummonCommand::setValue)
                        )
                );
        VehiclePackLoader.INSTANCE.getVehiclePacks().stream()
                .map(pack -> pack.meta().getNamespace()).distinct()
                .forEach(namespace -> command.then(Commands.literal(namespace + ":*")
                        .executes(context -> summonPack(context, namespace))));
        return command;
    }

    private static int summonPack(CommandContext<CommandSourceStack> context, String namespace) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        var displays = VehiclePackLoader.INSTANCE.scanVehicleDisplayDefinitions();
        var data = CommonAssetsManager.vehicleDataManager().getVehicleData().values().stream()
                .filter(vehicle -> vehicle.getVehicleId().getNamespace().equals(namespace))
                .sorted(VehicleTabOrder.comparator(
                        id -> displays.containsKey(id) ? displays.get(id).tabName : "",
                        id -> displays.containsKey(id) ? displays.get(id).tabIndex : 0))
                .toList();
        if (data.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("commands.vehicle.summon.empty", namespace));
            return 0;
        }
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        List<AbstractVehicle> vehicles = new ArrayList<>();
        List<AABB> bounds = new ArrayList<>();
        for (BaseVehicleData vehicleData : data) {
            AbstractVehicle vehicle = vehicleData.construct(player.level(), Vec3.ZERO, 0, player.getYRot());
            vehicle.initData();
            AABB bound = placementBounds(vehicle, right, forward);
            vehicles.add(vehicle);
            bounds.add(bound);
        }
        int spawned = 0;
        double rowNear = GAP + player.getBbWidth() / 2.0;
        for (int rowStart = 0; rowStart < vehicles.size(); rowStart += COLUMNS) {
            int rowEnd = Math.min(rowStart + COLUMNS, vehicles.size());
            double rowWidth = (rowEnd - rowStart - 1) * GAP;
            double rowDepth = 0;
            for (int i = rowStart; i < rowEnd; i++) {
                AABB bound = bounds.get(i);
                rowWidth += bound.getXsize();
                rowDepth = Math.max(rowDepth, bound.getZsize());
            }
            double left = -rowWidth / 2.0;
            double ahead = rowNear + rowDepth / 2.0;
            for (int i = rowStart; i < rowEnd; i++) {
                AbstractVehicle vehicle = vehicles.get(i);
                AABB bound = bounds.get(i);
                Vec3 position = player.position()
                        .add(right.scale(left - bound.minX))
                        .add(forward.scale(ahead - bound.getCenter().z))
                        .add(0, -bound.minY, 0);
                vehicle.setPos(position);
                vehicle.updateOBBs();
                vehicle.setBoundingBox(vehicle.makeBoundingBox());
                if (player.level().addFreshEntity(vehicle)) {
                    spawned++;
                }
                left += bound.getXsize() + GAP;
            }
            rowNear += rowDepth + GAP;
        }
        int count = spawned;
        context.getSource().sendSuccess(() -> Component.translatable("commands.vehicle.summon.pack", count, namespace), true);
        return spawned;
    }

    private static AABB placementBounds(AbstractVehicle vehicle, Vec3 right, Vec3 forward) {
        List<OBB> obbs = vehicle.getOBBs();
        obbs.add(vehicle.getMainCubeOBB().obb());
        AABB bounds = null;
        for (OBB obb : obbs) {
            for (var vertex : obb.getVertices()) {
                Vec3 point = new Vec3(vertex);
                Vec3 projected = new Vec3(point.dot(right), point.y, point.dot(forward));
                AABB pointBounds = new AABB(projected, projected);
                bounds = bounds == null ? pointBounds : bounds.minmax(pointBounds);
            }
        }
        return bounds;
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
