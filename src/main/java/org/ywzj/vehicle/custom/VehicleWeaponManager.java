package org.ywzj.vehicle.custom;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.ModRegistries;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerSyncData;
import org.ywzj.vehicle.resource.Vec3Serializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.ywzj.vehicle.util.ResourceScanner.scanDirectory;

@Mod.EventBusSubscriber
@ParametersAreNonnullByDefault
public class VehicleWeaponManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>>
        implements IVehicleWeaponManager {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(Vec3.class, new Vec3Serializer())
            .create();

    public static final Marker MARKER = MarkerManager.getMarker("VehicleWeaponTypeManager");

    private static VehicleWeaponManager INSTANCE;

    private enum ClientCache implements IVehicleWeaponManager {
        INSTANCE;
        private Map<ResourceLocation, VehicleWeaponIndex<?, ?>> indexes = Map.of();

        @Override
        public Map<ResourceLocation, VehicleWeaponIndex<?, ?>> getIndexes() {
            return indexes;
        }

        @Override
        public Optional<VehicleWeaponIndex<?, ?>> getIndex(ResourceLocation id) {
            return Optional.ofNullable(indexes.get(id));
        }

        public void fromNetwork(Map<ResourceLocation, String> map) {
            Map<ResourceLocation, JsonElement> jsonMap = new HashMap<>();
            for (var entry : map.entrySet()) {
                var ele = GSON.fromJson(entry.getValue(), JsonElement.class);
                if (ele != null) {
                    jsonMap.put(entry.getKey(), ele);
                }
            }

            indexes = parseIndexes(jsonMap);
        }
    }

    public static IVehicleWeaponManager get() {
        return INSTANCE != null ? INSTANCE : ClientCache.INSTANCE;
    }

    private Map<ResourceLocation, String> cache = Map.of();
    private Map<ResourceLocation, VehicleWeaponIndex<?, ?>> indexes = Map.of();

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
        indexes = parseIndexes(map);
    }

    public static void fromNetwork(Map<ResourceLocation, String> map) {
        ClientCache.INSTANCE.fromNetwork(map);
    }

    public Map<ResourceLocation, VehicleWeaponIndex<?, ?>> getIndexes() {
        return indexes;
    }

    public Optional<VehicleWeaponIndex<?, ?>> getIndex(ResourceLocation id) {
        return Optional.ofNullable(indexes.get(id));
    }

    protected static void clear() {
        INSTANCE = null;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        var commonAssetsManager = new VehicleWeaponManager();
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

    /**
     * 通用的武器数据解析方法，供apply和fromNetwork共用
     */
    private static Map<ResourceLocation, VehicleWeaponIndex<?, ?>> parseIndexes(
            Map<ResourceLocation, JsonElement> map
    ) {
        var registry = ModRegistries.WEAPON_UNIT_TYPE_SUPPLIER.get();
        if (registry == null) {
            YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle weapon data: registry is null. Is the game in a broken state?");
            return Map.of();
        }

        ImmutableMap.Builder<ResourceLocation, VehicleWeaponIndex<?, ?>> builder = ImmutableMap.builder();
        for (var entry : map.entrySet()) {
            try {
                if (!entry.getValue().isJsonObject()) {
                    YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle weapon data {}: Not a Json Object", entry.getKey());
                    continue;
                }

                ResourceLocation id = entry.getKey();
                JsonObject object = entry.getValue().getAsJsonObject();
                String rawType = GsonHelper.getAsString(object, "type");
                if (!rawType.contains(":")) {
                    rawType = YwzjVehicle.MOD_ID + ":" + rawType;
                }
                var typeId = ResourceLocation.tryParse(rawType);
                if (typeId == null) {
                    YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle weapon data {}: invalid weapon type id: {}", id, rawType);
                    continue;
                }
                var weaponType = registry.getValue(typeId);
                if (weaponType != null) {
                    var index = weaponType.parseAndLoad(id, object);
                    if (index != null) {
                        builder.put(id, index);
                    }
                } else {
                    YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle weapon data {}: unknown weapon type: {}", id, typeId);
                }
            } catch (Exception e) {
                YwzjVehicle.LOGGER.error(MARKER, "Error loading vehicle weapon data {}", entry.getKey(), e);
            }
        }
        return builder.build();
    }
}
