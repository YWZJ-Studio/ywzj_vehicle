package org.ywzj.vehicle.custom;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.serialize.Vec3Serializer;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleDataPojo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;

import static org.ywzj.vehicle.util.ResourceScanner.scanDirectory;

@Mod.EventBusSubscriber
@ParametersAreNonnullByDefault
public class VehicleDataManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(Vec3.class, new Vec3Serializer())
            .create();

    public static final Marker MARKER = MarkerManager.getMarker("VehicleDataManager");

    private static VehicleDataManager INSTANCE;

    public static VehicleDataManager get() {
        return INSTANCE;
    }

    private Map<ResourceLocation, String> cache = Map.of();
    private Map<ResourceLocation, BaseVehicleData> indexes = Map.of();

    @NotNull
    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var map = scanDirectory(manager, "vehicle", GSON);
        ImmutableMap.Builder<ResourceLocation, String> builder = ImmutableMap.builder();
        for (var entry : map.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().toString());
        }
        cache = builder.build();
        return map;
    }

    @Override
    public void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        ImmutableMap.Builder<ResourceLocation, BaseVehicleData> builder = ImmutableMap.builder();
        for (var ele : map.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(ele.getValue(), "vehicle data");
                var pojo = GSON.fromJson(obj, BaseVehicleDataPojo.class);
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
        indexes = builder.build();
    }


    protected static void clear() {
        INSTANCE = null;
    }

    public Optional<BaseVehicleData> getVehicleData(ResourceLocation id) {
        return Optional.ofNullable(indexes.get(id));
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

//    @SubscribeEvent
//    public static void OnDatapackSync(OnDatapackSyncEvent event) {
//        if (INSTANCE == null) {
//            return;
//        }
//        var msg = new ServerSyncData(INSTANCE.cache);
//        if (event.getPlayer() != null) {
//            Channel.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), msg);
//        } else {
//            Channel.CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
//        }
//    }

}
