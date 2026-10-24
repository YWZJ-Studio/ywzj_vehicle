package org.ywzj.vehicle.all;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.resource.VehiclePackLoader;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AllTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YwzjVehicle.MOD_ID);
    public static final List<Supplier<? extends ItemLike>> MISC_ITEMS = new ArrayList<>();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_MISC = TABS.register("tab_misc", () ->
            CreativeModeTab
                    .builder()
                    .title(Component.translatable("tab.misc"))
                    .icon(AllItems.FIGURE_BOX.get()::getDefaultInstance)
                    .displayItems((displayParams, output) ->
                            MISC_ITEMS.forEach(itemLike -> output.accept(itemLike.get())))
                    .build());

    private static void addVehicleTab(String name, Component title, Supplier<ItemStack> iconSupplier,
                                      String namespace, Predicate<ResourceLocation> filter) {
        TABS.register(name, () ->
                CreativeModeTab
                        .builder()
                        .title(title)
                        .icon(iconSupplier)
                        .displayItems((displayParams, output) ->
                                CommonAssetsManager.vehicleDataManager().getVehicleData().values().stream()
                                        .filter(vehicleData -> vehicleData.getVehicleId().getNamespace().equals(namespace))
                                        .filter(vehicleData -> filter.test(vehicleData.getVehicleId()))
                                        .sorted(Comparator.comparingInt(vehicleData ->
                                                ClientAssetsManager.INSTANCE
                                                        .getVehicleDisplay(vehicleData.getVehicleId())
                                                        .map(baseDisplay -> baseDisplay.getTabIndex()
                                                                + (vehicleData.isExperimental()
                                                                && (baseDisplay.getTabName() == null || baseDisplay.getTabName().isBlank())
                                                                ? 1_000_000 : 0))
                                                        .orElse(0)))
                                        .forEach(vehicleData -> output.accept(AllItems.VEHICLE_SPAWN_ITEM.get().createInstance(vehicleData.getVehicleId()))))
                        .build());
    }

    public static void register(IEventBus eventBus) {
        var displays = VehiclePackLoader.INSTANCE.scanVehicleDisplayDefinitions();
        VehiclePackLoader.INSTANCE.getVehiclePacks().forEach(vehiclePack -> {
            String namespace = vehiclePack.meta().getNamespace();
            ResourceLocation defaultIcon = YwzjVehicle.resourceLocation(namespace + ":textures/tab.png");
            Map<String, ResourceLocation> groups = new TreeMap<>();
            displays.entrySet().stream()
                    .filter(entry -> entry.getKey().getNamespace().equals(namespace))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        var display = entry.getValue();
                        if (display != null && display.tabName != null && !display.tabName.isBlank()) {
                            groups.putIfAbsent(display.tabName, display.tabIcon);
                        }
                    });

            addVehicleTab("tab_" + namespace,
                        Component.translatable(vehiclePack.meta().getTitle()),
                        () -> AllItems.PLAIN_TEXTURE_ITEM.get().createInstance(defaultIcon),
                        namespace, id -> !groups.containsKey(getTabName(id)));
            groups.forEach((tabName, icon) -> {
                String suffix = UUID.nameUUIDFromBytes(tabName.getBytes(StandardCharsets.UTF_8)).toString();
                addVehicleTab("tab_" + namespace + "/" + suffix,
                        Component.translatable(tabName),
                        () -> AllItems.PLAIN_TEXTURE_ITEM.get().createInstance(icon != null ? icon : defaultIcon),
                        namespace, id -> tabName.equals(getTabName(id)));
            });
        });
        TABS.register(eventBus);
    }

    private static String getTabName(ResourceLocation vehicleId) {
        return ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicleId)
                .map(BaseDisplay::getTabName)
                .orElse("");
    }

    public enum Category {
        VEHICLE,
        MISC,
        NONE
    }

}
