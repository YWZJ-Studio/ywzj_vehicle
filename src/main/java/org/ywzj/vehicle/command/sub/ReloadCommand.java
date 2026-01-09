package org.ywzj.vehicle.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand {

    private static final String RELOAD_NAME = "reload";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reloadCommand = Commands.literal(RELOAD_NAME);
        reloadCommand.executes(ReloadCommand::reload);
        return reloadCommand;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        CommonAssetsManager.INSTANCE.reload(server.getResourceManager());
        ClientAssetsManager.INSTANCE.reload(Minecraft.getInstance().getResourceManager());
        reloadAllVehicles(context.getSource());
        return Command.SINGLE_SUCCESS;
    }

    private static void reloadAllVehicles(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        record VehicleSnapshot(
                ResourceLocation type,
                CompoundTag nbt,
                ServerLevel level
        ) {}
        List<VehicleSnapshot> snapshots = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof AbstractVehicle vehicle)) {
                    continue;
                }
                CompoundTag nbt = new CompoundTag();
                vehicle.saveWithoutId(nbt);
                snapshots.add(new VehicleSnapshot(
                        EntityType.getKey(vehicle.getType()),
                        nbt,
                        level
                ));
                vehicle.discard();
            }
        }
        for (VehicleSnapshot snapshot : snapshots) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(snapshot.type());
            if (type == null) {
                continue;
            }
            Entity entity = type.create(snapshot.level());
            if (entity == null) {
                continue;
            }
            entity.load(snapshot.nbt());
            snapshot.level().addFreshEntity(entity);
        }
    }

}
