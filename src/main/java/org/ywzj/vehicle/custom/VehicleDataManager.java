package org.ywzj.vehicle.custom;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
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
import org.ywzj.vehicle.api.custom.IVehicleDataManager;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleDataPojo;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerSyncVehicleData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.ywzj.vehicle.util.ResourceScanner.scanDirectory;

@Mod.EventBusSubscriber
@ParametersAreNonnullByDefault
public class VehicleDataManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>>
        implements IVehicleDataManager {

    public static final Marker MARKER = MarkerManager.getMarker("VehicleDataManager");

    private static VehicleDataManager INSTANCE;

    private enum ClientCache implements IVehicleDataManager {
        INSTANCE;
        private Map<ResourceLocation, BaseVehicleData> indexes = Map.of();

        @Override
        public Optional<BaseVehicleData> getVehicleData(ResourceLocation id) {
            return Optional.ofNullable(indexes.get(id));
        }

        @Override
        public Map<ResourceLocation, BaseVehicleData> getVehicleData() {
            return indexes;
        }

        public void fromNetwork(Map<ResourceLocation, String> map) {
            Map<ResourceLocation, JsonElement> jsonMap = new HashMap<>();
            for (var entry : map.entrySet()) {
                var ele = GsonUtil.GSON.fromJson(entry.getValue(), JsonElement.class);
                if (ele != null) {
                    jsonMap.put(entry.getKey(), ele);
                }
            }

            indexes = parseIndexes(jsonMap);
        }
    }

    private static Map<ResourceLocation, BaseVehicleData> parseIndexes(Map<ResourceLocation, JsonElement> jsonMap) {
        ImmutableMap.Builder<ResourceLocation, BaseVehicleData> builder = ImmutableMap.builder();
        for (var ele : jsonMap.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(ele.getValue(), "vehicle data");
                var pojo = GsonUtil.GSON.fromJson(obj, BaseVehicleDataPojo.class);
                var data = BaseVehicleData.of(pojo);
                if (data == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to load vehicle data: {}, invalid structure model", ele.getKey());
                    continue;
                }
                builder.put(ele.getKey(), data);
            } catch (Exception e) {
                YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle data: {}", ele.getKey(), e);
            }
        }
        return builder.build();
    }

    public static IVehicleDataManager get() {
        return INSTANCE != null ? INSTANCE : ClientCache.INSTANCE;
    }

    private Map<ResourceLocation, String> cache = Map.of();
    private Map<ResourceLocation, BaseVehicleData> indexes = Map.of();

    @NotNull
    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var map = scanDirectory(manager, "vehicle", GsonUtil.GSON);
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

    protected static void clear() {
        INSTANCE = null;
    }

    public Optional<BaseVehicleData> getVehicleData(ResourceLocation id) {
        return Optional.ofNullable(indexes.get(id));
    }

    @Override
    public Map<ResourceLocation, BaseVehicleData> getVehicleData() {
        return indexes;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        var commonAssetsManager = new VehicleDataManager();
        event.addListener(commonAssetsManager);
        INSTANCE = commonAssetsManager;
    }

    @SubscribeEvent
    public static void OnDatapackSync(OnDatapackSyncEvent event) {
        if (INSTANCE == null) {
            return;
        }
        var msg = new ServerSyncVehicleData(INSTANCE.cache);
        if (event.getPlayer() != null) {
            Channel.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), msg);
        } else {
            Channel.CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
        }
    }

}
