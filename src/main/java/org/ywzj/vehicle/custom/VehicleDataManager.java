package org.ywzj.vehicle.custom;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.ModRegistries;
import org.ywzj.vehicle.api.custom.IVehicleDataManager;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.ywzj.vehicle.util.ResourceScanner.scanDirectory;

@Mod.EventBusSubscriber
@ParametersAreNonnullByDefault
public class VehicleDataManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> implements IVehicleDataManager {

    public static final Marker MARKER = MarkerManager.getMarker("VehicleDataManager");
    private Map<ResourceLocation, BaseVehicleData> indexes = Map.of();
    private Map<ResourceLocation, String> cache = Map.of();

    enum ClientCache implements IVehicleDataManager {

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

        public void fromNetwork(Map<ResourceLocation, String> idAndData) {
            Map<ResourceLocation, JsonElement> jsonMap = new HashMap<>();
            for (var entry : idAndData.entrySet()) {
                var element = GsonUtil.GSON.fromJson(entry.getValue(), JsonElement.class);
                if (element != null) {
                    jsonMap.put(entry.getKey(), element);
                }
            }
            indexes = parseIndexes(jsonMap);
        }

    }

    @NotNull
    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var map = scanDirectory(manager, "vehicles", GsonUtil.GSON);
        ImmutableMap.Builder<ResourceLocation, String> builder = ImmutableMap.builder();
        for (var entry : map.entrySet()) {
            builder.put(entry.getKey(), entry.getValue().toString());
        }
        cache = builder.build();
        return map;
    }

    @Override
    public void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        indexes = parseIndexes(resources);
    }

    public static void fromNetwork(Map<ResourceLocation, String> map) {
        ClientCache.INSTANCE.fromNetwork(map);
    }

    @Override
    public Optional<BaseVehicleData> getVehicleData(ResourceLocation id) {
        return Optional.ofNullable(indexes.get(id));
    }

    @Override
    public Map<ResourceLocation, BaseVehicleData> getVehicleData() {
        return indexes;
    }

    public Map<ResourceLocation, String> getCache() {
        return cache;
    }

    private static Map<ResourceLocation, BaseVehicleData> parseIndexes(Map<ResourceLocation, JsonElement> jsonMap) {
        ImmutableMap.Builder<ResourceLocation, BaseVehicleData> builder = ImmutableMap.builder();
        for (var vehicleIdAndVehicleDataJson : jsonMap.entrySet()) {
            ResourceLocation vehicleId = vehicleIdAndVehicleDataJson.getKey();
            JsonElement vehicleDataJson = vehicleIdAndVehicleDataJson.getValue();
            try {
                var obj = GsonHelper.convertToJsonObject(vehicleDataJson, "vehicle data");
                String type = GsonHelper.getAsString(obj, "type", "ywzj_vehicle:generic");
                ResourceLocation typeId = ResourceLocation.tryParse(type);
                if (typeId == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to load vehicle data: {}, invalid type id {}", vehicleId, type);
                    continue;
                }

                var dataType = ModRegistries.VEHICLE_DATA_TYPE_SUPPLIER.get().getValue(typeId);
                if (dataType == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to load vehicle data: {}, unknown type {}", vehicleId, typeId);
                    continue;
                }

                var data = dataType.parse(vehicleDataJson);
                if (data == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to parse vehicle data: {}", vehicleId);
                    continue;
                }
                data.setVehicleId(vehicleId);
                data.setName(Component.translatable("entity." + vehicleId.getNamespace() + "." + vehicleId.getPath()));
                builder.put(vehicleId, data);
            } catch (Exception e) {
                YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle data: {}", vehicleId, e);
            }
        }
        return builder.build();
    }

}
