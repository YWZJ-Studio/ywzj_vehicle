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
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.resource.VehiclePackLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AllTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YwzjVehicle.MOD_ID);
    public static final List<Supplier<? extends ItemLike>> MISC_ITEMS = new ArrayList<>();

    public static final RegistryObject<CreativeModeTab> TAB_VEHICLE = TABS.register("tab_vehicle", () ->
            CreativeModeTab
                    .builder()
                    .title(Component.translatable("tab.vehicle"))
                    .icon(AllItems.FIGURE_BOX.get()::getDefaultInstance)
                    .displayItems((displayParams, output) ->
                            CommonAssetsManager.vehicleDataManager().getVehicleData().keySet().stream()
                                    .filter(vehicleId -> vehicleId.getNamespace().equals(YwzjVehicle.MOD_ID))
                                    .sorted()
                                    .forEach(vehicleId -> output.accept(AllItems.VEHICLE_SPAWN_ITEM.get().createInstance(vehicleId))))
                    .build());

    public static final RegistryObject<CreativeModeTab> TAB_MISC = TABS.register("tab_misc", () ->
            CreativeModeTab
                    .builder()
                    .title(Component.translatable("tab.misc"))
                    .icon(AllItems.FUEL_TANK.get()::getDefaultInstance)
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
                                CommonAssetsManager.vehicleDataManager().getVehicleData().keySet().stream()
                                        .filter(vehicleId -> vehicleId.getNamespace().equals(namespace))
                                        .sorted()
                                        .forEach(vehicleId -> output.accept(AllItems.VEHICLE_SPAWN_ITEM.get().createInstance(vehicleId))))
                        .build());
    }

    public static void register(IEventBus eventBus) {
        VehiclePackLoader.INSTANCE.getVehiclePacks().forEach(vehiclePack ->
                addVehicleTab("tab_" + vehiclePack.namespace(),
                        Component.translatable(vehiclePack.title()),
                        () -> AllItems.PLAIN_TEXTURE_ITEM.get().createInstance(YwzjVehicle.resourceLocation(vehiclePack.namespace() + ":textures/tab.png")),
                        vehiclePack.namespace()));
        TABS.register(eventBus);
    }

    public enum Category {
        VEHICLE,
        MISC,
        NONE
    }

}
