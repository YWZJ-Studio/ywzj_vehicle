package org.ywzj.vehicle.custom;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.ModRegistries;
import org.ywzj.vehicle.api.custom.IVehicleWeaponManager;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.custom.weapon.VehicleWeaponIndex;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.ywzj.vehicle.util.ResourceScanner.scanDirectory;

@ParametersAreNonnullByDefault
public class VehicleWeaponManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> implements IVehicleWeaponManager {

    public static final Marker MARKER = MarkerManager.getMarker("VehicleWeaponTypeManager");

    enum ClientCache implements IVehicleWeaponManager {
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
                var ele = GsonUtil.GSON.fromJson(entry.getValue(), JsonElement.class);
                if (ele != null) {
                    jsonMap.put(entry.getKey(), ele);
                }
            }

            indexes = parseIndexes(jsonMap);
        }
    }

    private Map<ResourceLocation, String> cache = Map.of();
    private Map<ResourceLocation, VehicleWeaponIndex<?, ?>> indexes = Map.of();

    @NotNull
    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        var map = scanDirectory(manager, "weapons", GsonUtil.GSON);
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

    public Map<ResourceLocation, String> getCache() {
        return cache;
    }

    /**
     * 通用的武器数据解析方法，供apply和fromNetwork共用
     */
    private static Map<ResourceLocation, VehicleWeaponIndex<?, ?>> parseIndexes(
            Map<ResourceLocation, JsonElement> map
    ) {
        var registry = ModRegistries.VEHICLE_WEAPON_TYPE;
        if (registry == null) {
            YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle weapon data: registry is null. Is the game in a broken state?");
            return Map.of();
        }

        ImmutableMap.Builder<ResourceLocation, VehicleWeaponIndex<?, ?>> builder = ImmutableMap.builder();
        for (var weaponIdAndWeaponDataJson : map.entrySet()) {
            try {
                if (!weaponIdAndWeaponDataJson.getValue().isJsonObject()) {
                    YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle weapon data {}: Not a Json Object", weaponIdAndWeaponDataJson.getKey());
                    continue;
                }

                ResourceLocation weaponId = weaponIdAndWeaponDataJson.getKey();
                JsonObject weaponData = weaponIdAndWeaponDataJson.getValue().getAsJsonObject();
                String rawType = GsonHelper.getAsString(weaponData, "type");
                if (!rawType.contains(":")) {
                    rawType = YwzjVehicle.MOD_ID + ":" + rawType;
                }
                var typeId = ResourceLocation.tryParse(rawType);
                if (typeId == null) {
                    YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle weapon data {}: invalid weapon type weaponId: {}", weaponId, rawType);
                    continue;
                }
                var weaponType = registry.get(typeId);
                if (weaponType != null) {
                    var index = weaponType.parseAndLoad(weaponId, weaponData);
                    if (index != null) {
                        if (index.data().getWeaponId() == null) {
                            index.data().setWeaponId(weaponId);
                        }
                        builder.put(weaponId, index);
                    }
                } else {
                    YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle weapon data {}: unknown weapon type: {}", weaponId, typeId);
                }
            } catch (Exception e) {
                YwzjVehicle.LOGGER.error(MARKER, "Error loading vehicle weapon data {}", weaponIdAndWeaponDataJson.getKey(), e);
            }
        }
        return builder.build();
    }

}
