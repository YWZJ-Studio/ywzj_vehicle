package org.ywzj.vehicle.all;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.item.AmmoItem;
import org.ywzj.vehicle.item.FuelTankItem;
import org.ywzj.vehicle.item.UavControllerItem;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class AllItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YwzjVehicle.MOD_ID);
    public static final LinkedHashMap<String, RegistryObject<Item>> ITEMS_LOOKUP = new LinkedHashMap<>();

    public static final RegistryObject<Item> FUEL_TANK = registerItem(AllTabs.Category.MISC, "fuel_tank", () -> new FuelTankItem(new Item.Properties().durability(125)));
    public static final RegistryObject<Item> UAV_CONTROLLER = registerItem(AllTabs.Category.MISC, "uav_controller", () -> new UavControllerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> AMMO_MACHINE_GUN = registerItem(AllTabs.Category.MISC, "ammo_machine_gun", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.MACHINE_GUN));
    public static final RegistryObject<Item> AMMO_AUTO_CANNON = registerItem(AllTabs.Category.MISC, "ammo_auto_cannon", () -> new AmmoItem(new Item.Properties().stacksTo(32), AmmoItem.AmmoType.AUTO_CANNON));
    public static final RegistryObject<Item> AMMO_ARTILLERY = registerItem(AllTabs.Category.MISC, "ammo_artillery", () -> new AmmoItem(new Item.Properties().stacksTo(16), AmmoItem.AmmoType.ARTILLERY));
    public static final RegistryObject<Item> AMMO_MISSILE = registerItem(AllTabs.Category.MISC, "ammo_missile", () -> new AmmoItem(new Item.Properties().stacksTo(8), AmmoItem.AmmoType.MISSILE));

    public static <T extends Item> RegistryObject<Item> registerItem(AllTabs.Category category, String name, Supplier<T> item) {
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
