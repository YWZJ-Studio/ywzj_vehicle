package org.ywzj.vehicle.all;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.resource.VehiclePackLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class AllTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YwzjVehicle.MOD_ID);
    public static final List<Supplier<? extends ItemLike>> MISC_ITEMS = new ArrayList<>();

    public static final RegistryObject<CreativeModeTab> TAB_MISC = TABS.register("tab_misc", () ->
            CreativeModeTab
                    .builder()
                    .title(Component.translatable("tab.misc"))
                    .icon(AllItems.FIGURE_BOX.get()::getDefaultInstance)
                    .displayItems((displayParams, output) ->
                            MISC_ITEMS.forEach(itemLike -> output.accept(itemLike.get())))
                    .build());

    public static void addVehicleTab(String name, Component title, Supplier<ItemStack> iconSupplier, String namespace) {
        TABS.register(name, () ->
                CreativeModeTab
                        .builder()
                        .title(title)
                        .icon(iconSupplier)
                        .displayItems((displayParams, output) ->
                                CommonAssetsManager.vehicleDataManager().getVehicleData().values().stream()
                                        .filter(vehicleData -> vehicleData.getVehicleId().getNamespace().equals(namespace))
                                        .sorted(Comparator.comparingInt(vehicleData ->
                                                ClientAssetsManager.INSTANCE
                                                        .getVehicleDisplay(vehicleData.getVehicleId())
                                                        .map(baseDisplay -> baseDisplay.getTabIndex() + (vehicleData.isExperimental() ? 1_000_000 : 0))
                                                        .orElse(0)))
                                        .forEach(vehicleData -> output.accept(AllItems.VEHICLE_SPAWN_ITEM.get().createInstance(vehicleData.getVehicleId()))))
                        .build());
    }

    public static void register(IEventBus eventBus) {
        VehiclePackLoader.INSTANCE.getVehiclePacks().forEach(vehiclePack ->
                addVehicleTab("tab_" + vehiclePack.meta().getNamespace(),
                        Component.translatable(vehiclePack.meta().getTitle()),
                        () -> AllItems.PLAIN_TEXTURE_ITEM.get().createInstance(YwzjVehicle.resourceLocation(vehiclePack.meta().getNamespace() + ":textures/tab.png")),
                        vehiclePack.meta().getNamespace()));
        TABS.register(eventBus);
    }

    public enum Category {
        VEHICLE,
        MISC,
        NONE
    }

}
