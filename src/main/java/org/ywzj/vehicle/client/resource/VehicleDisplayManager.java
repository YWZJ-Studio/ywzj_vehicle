package org.ywzj.vehicle.client.resource;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.ModRegistries;
import org.ywzj.vehicle.client.resource.vehicle.BaseVehicleDisplay;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.util.ResourceScanner;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class VehicleDisplayManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {

    public static final Marker MARKER = MarkerManager.getMarker("VehicleDisplayManager");

    private Map<ResourceLocation, BaseVehicleDisplay> displayMap = Map.of();

    @NotNull
    @Override
    protected Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller pProfiler) {
        return ResourceScanner.scanDirectory(manager, "display/vehicle", GsonUtil.GSON);
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> resources, @NotNull ResourceManager manager, @NotNull ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, BaseVehicleDisplay> builder = ImmutableMap.builder();
        for (var entry : resources.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(entry.getValue(), "vehicle display");
                String type = GsonHelper.getAsString(obj, "type", "ywzj_vehicle:generic");
                ResourceLocation typeId = ResourceLocation.tryParse(type);
                if (typeId == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to load vehicle display: {}, invalid type id {}", entry.getKey(), type);
                    continue;
                }

                var dataType = ModRegistries.VEHICLE_DISPLAY_TYPE_SUPPLIER.get().getValue(typeId);
                if (dataType == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to load vehicle display: {}, unknown type {}", entry.getKey(), typeId);
                    continue;
                }

                var data = dataType.parse(entry.getValue());
                if (data == null) {
                    YwzjVehicle.LOGGER.warn(MARKER, "Failed to parse vehicle display: {}", entry.getKey());
                    continue;
                }
                builder.put(entry.getKey(), data);
            } catch (Exception e) {
                YwzjVehicle.LOGGER.error(MARKER, "Failed to load vehicle display: {}", entry.getKey(), e);
            }
        }
        displayMap = builder.build();
    }

    @UnmodifiableView
    public Map<ResourceLocation, BaseVehicleDisplay> getDisplayMap() {
        return displayMap;
    }
}
