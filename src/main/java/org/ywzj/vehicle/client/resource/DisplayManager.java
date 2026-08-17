package org.ywzj.vehicle.client.resource;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.ModRegistries;
import org.ywzj.vehicle.api.scripts.ScriptContextFactory;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleDisplay;
import org.ywzj.vehicle.custom.serialize.GsonUtil;
import org.ywzj.vehicle.util.ResourceScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class DisplayManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {

    public final Marker marker;
    private final String path;
    private Map<ResourceLocation, BaseDisplay> displayMap = Map.of();
    private final Map<ResourceLocation, List<BaseDisplay>> modelWithVariableDisplay = new ConcurrentHashMap<>();
    private final ScriptManager scriptManager;
    private final ScriptContextFactory scriptContextFactory;

    public DisplayManager(String path,
                         ScriptManager scriptManager,
                         ScriptContextFactory scriptContextFactory) {
        this.path = path;
        this.marker = MarkerManager.getMarker("DisplayManager$" + path);
        this.scriptManager = scriptManager;
        this.scriptContextFactory = scriptContextFactory;
    }

    @NotNull
    @Override
    public Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller pProfiler) {
        return ResourceScanner.scanDirectory(manager, "display/" + path, GsonUtil.GSON);
    }

    @Override
    public void apply(@NotNull Map<ResourceLocation, JsonElement> resources, @NotNull ResourceManager manager, @NotNull ProfilerFiller pProfiler) {
        ImmutableMap.Builder<ResourceLocation, BaseDisplay> displayMapBuilder = ImmutableMap.builder();
        
        for (var displayIdAndDataJson : resources.entrySet()) {
            try {
                var obj = GsonHelper.convertToJsonObject(displayIdAndDataJson.getValue(), "vehicle display");
                String type = GsonHelper.getAsString(obj, "type", "ywzj_vehicle:generic");
                ResourceLocation typeId = ResourceLocation.tryParse(type);
                if (typeId == null) {
                    YwzjVehicle.LOGGER.warn(marker, "Failed to load vehicle display: {}, invalid type id {}", displayIdAndDataJson.getKey(), type);
                    continue;
                }

                var dataType = ModRegistries.VEHICLE_DISPLAY_TYPE.get(typeId);
                if (dataType == null) {
                    YwzjVehicle.LOGGER.warn(marker, "Failed to load vehicle display: {}, unknown type {}", displayIdAndDataJson.getKey(), typeId);
                    continue;
                }

                var display = dataType.parse(displayIdAndDataJson.getValue());
                if (display == null) {
                    YwzjVehicle.LOGGER.warn(marker, "Failed to parse vehicle display: {}", displayIdAndDataJson.getKey());
                    continue;
                }

                if (display instanceof VehicleDisplay<?, ?> vehicleDisplay) {
                    vehicleDisplay.initializeAnimationController(scriptManager, scriptContextFactory);
                    if (vehicleDisplay.getCabinDisplay() != null) {
                        vehicleDisplay.getCabinDisplay().initializeAnimationController(scriptManager, scriptContextFactory);
                    }
                }

                displayMapBuilder.put(displayIdAndDataJson.getKey(), display);
                display.setDisplayId(displayIdAndDataJson.getKey());
                if (display.getModelPath() != null) {
                    modelWithVariableDisplay.computeIfAbsent(display.getModelPath(), k -> new ArrayList<>()).add(display);
                }
            } catch (Exception e) {
                YwzjVehicle.LOGGER.error(marker, "Failed to load vehicle display: {}", displayIdAndDataJson.getKey(), e);
            }
        }
        displayMap = displayMapBuilder.build();
    }

    @UnmodifiableView
    public Map<ResourceLocation, BaseDisplay> getDisplayMap() {
        return displayMap;
    }

    public Map<ResourceLocation, List<BaseDisplay>> getModelWithVariableDisplay() {
        return modelWithVariableDisplay;
    }

}
