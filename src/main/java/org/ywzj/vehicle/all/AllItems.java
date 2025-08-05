package org.ywzj.vehicle.all;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.Vehicle;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class AllItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Vehicle.MOD_ID);
    public static final LinkedHashMap<String, RegistryObject<Item>> ITEMS_LOOKUP = new LinkedHashMap<>();

    public static <T extends Item> RegistryObject<Item> registerItem(AllTabs.Category category, String name, Supplier<T> item, boolean withGeoModel) {
        return registerItem(category, name, item, withGeoModel, false);
    }

    public static <T extends Item> RegistryObject<Item> registerItem(AllTabs.Category category, String name, Supplier<T> item, boolean withGeoModel, boolean isArmor) {
        RegistryObject<Item> registryObject = ITEMS.register(name, item);
//        if (AllTabs.Category.MATERIAL.equals(category)) {
//            AllTabs.MATERIAL_ITEMS.add(registryObject);
//        } else if (AllTabs.Category.LOOT.equals(category)) {
//            AllTabs.LOOT_ITEMS.add(registryObject);
//        } else if (AllTabs.Category.EQUIPMENT.equals(category)) {
//            AllTabs.EQUIPMENT_ITEMS.add(registryObject);
//        } else if (AllTabs.Category.CONSUMABLE.equals(category)) {
//            AllTabs.CONSUMABLE_ITEMS.add(registryObject);
//        } else
            if (AllTabs.Category.MISC.equals(category)) {
            AllTabs.MISC_ITEMS.add(registryObject);
        }
//        if (withGeoModel) {
//            if (isArmor) {
//                AllGeoModels.registerArmor(name, registryObject);
//            } else {
//                AllGeoModels.registerCommon(name, registryObject);
//            }
//        }
        AllItems.ITEMS_LOOKUP.put(name, registryObject);
        return registryObject;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
