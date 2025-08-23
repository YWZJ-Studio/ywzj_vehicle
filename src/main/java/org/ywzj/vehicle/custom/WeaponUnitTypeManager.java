package org.ywzj.vehicle.custom;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.Vehicle;
import org.ywzj.vehicle.all.ModRegistries;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerSyncData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static org.ywzj.vehicle.util.ResourceScanner.scanDirectory;

@Mod.EventBusSubscriber
@ParametersAreNonnullByDefault
public class WeaponUnitTypeManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .create();

    private static WeaponUnitTypeManager INSTANCE;

    private Map<ResourceLocation, String> cache = Map.of();

    public static WeaponUnitTypeManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var map = scanDirectory(manager, "weapon_units", GSON);
        ImmutableMap.Builder<ResourceLocation, String> builder = ImmutableMap.builder();
        for (var entry : map.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().toString());
        }
        cache = builder.build();
        return map;
    }

    @Override
    public void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        var registry = ModRegistries.WEAPON_UNIT_TYPE_SUPPLIER.get();
        if (registry != null) {
            registry.forEach(weaponUnitType -> {
                try {
                    ResourceLocation id = weaponUnitType.getId();
                    if (map.containsKey(id)) {
                        weaponUnitType.parseAndLoad(map.get(id));
                    } else {
                        Vehicle.LOGGER.error("failed to load weapon unit type data: {}", id);
                    }
                } catch (Exception e) {
                    Vehicle.LOGGER.error("error loading weapon unit type data: {}", weaponUnitType.getId(), e);
                }
            });
        }
    }

    public static void fromNetwork(Map<ResourceLocation, String> map) {
        var registry = ModRegistries.WEAPON_UNIT_TYPE_SUPPLIER.get();
        if (registry != null) {
            registry.forEach(weaponUnitType -> {
                try {
                    ResourceLocation id = weaponUnitType.getId();
                    if (map.containsKey(id)) {
                        JsonElement element = GSON.fromJson(map.get(id), JsonElement.class);
                        if (element != null) {
                            weaponUnitType.parseAndLoad(element);
                            return;
                        }
                    }
                    Vehicle.LOGGER.error("failed to load weapon unit type data: {}", id);
                } catch (Exception e) {
                    Vehicle.LOGGER.error("error loading weapon unit type data: {}", weaponUnitType.getId(), e);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        var commonAssetsManager = new WeaponUnitTypeManager();
        event.addListener(commonAssetsManager);
        INSTANCE = commonAssetsManager;
    }

    @SubscribeEvent
    public static void OnDatapackSync(OnDatapackSyncEvent event) {
        if (INSTANCE == null) {
            return;
        }
        var msg = new ServerSyncData(INSTANCE.cache);
        if (event.getPlayer() != null) {
            Channel.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), msg);
        } else {
            Channel.CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
        }
    }
}
