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
        for (var customIdAndVehicleDataJson : jsonMap.entrySet()) {
            ResourceLocation customId = customIdAndVehicleDataJson.getKey();
            JsonElement vehicleDataJson = customIdAndVehicleDataJson.getValue();
            try {
                var obj = GsonHelper.convertToJsonObject(vehicleDataJson, "vehicle data");
                String type = GsonHelper.getAsString(obj, "type", "ywzj_vehicle:generic");
                ResourceLocation typeId = ResourceLocation.tryParse(type);
                if (typeId == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to load vehicle data: {}, invalid type id {}", customId, type);
                    continue;
                }

                var dataType = ModRegistries.VEHICLE_DATA_TYPE_SUPPLIER.get().getValue(typeId);
                if (dataType == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to load vehicle data: {}, unknown type {}", customId, typeId);
                    continue;
                }

                var data = dataType.parse(vehicleDataJson);
                if (data == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to parse vehicle data: {}", customId);
                    continue;
                }
                data.setCustomId(customId);
                data.setName(Component.translatable("entity." + customId.getNamespace() + "." + customId.getPath()));
                builder.put(customId, data);
            } catch (Exception e) {
                YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle data: {}", customId, e);
            }
        }
        return builder.build();
    }

    private Map<ResourceLocation, String> cache = Map.of();
    private Map<ResourceLocation, BaseVehicleData> indexes = Map.of();

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

}
