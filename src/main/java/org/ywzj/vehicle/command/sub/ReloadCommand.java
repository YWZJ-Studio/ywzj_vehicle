package org.ywzj.vehicle.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.time.StopWatch;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReloadCommand {

    private static final String RELOAD_NAME = "reload";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> reloadCommand = Commands.literal(RELOAD_NAME);
        reloadCommand.executes(ReloadCommand::reload);
        return reloadCommand;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        StopWatch watch = StopWatch.createStarted();
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                LocalPlayer player = Minecraft.getInstance().player;
                ClientAssetsManager.INSTANCE.reload(Minecraft.getInstance().getResourceManager());
                CreativeModeTabs.tryRebuildTabContents(player.connection.enabledFeatures(), true, player.level().registryAccess());
            });
            MinecraftServer server = context.getSource().getServer();
            CommonAssetsManager.INSTANCE.reload(server.getResourceManager());
            reloadAllVehicles(context.getSource());
        }
        watch.stop();
        double time = watch.getTime(TimeUnit.MICROSECONDS) / 1000.0;
        context.getSource().sendSystemMessage(Component.translatable("commands.vehicle.reload.success", time));
        AllConfigs.loadExternal();
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
