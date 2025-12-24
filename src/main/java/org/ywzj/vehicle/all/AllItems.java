package org.ywzj.vehicle.all;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.item.*;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class AllItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YwzjVehicle.MOD_ID);
    public static final LinkedHashMap<String, RegistryObject<? extends Item>> ITEMS_LOOKUP = new LinkedHashMap<>();

    public static final RegistryObject<VehicleSpawnItem> VEHICLE_SPAWN_ITEM = registerItem(AllTabs.Category.VEHICLE, "vehicle_spawn_item", () -> new VehicleSpawnItem(new Item.Properties().durability(1)));
    public static final RegistryObject<Item> FUEL_TANK = registerItem(AllTabs.Category.MISC, "fuel_tank", () -> new FuelTankItem(new Item.Properties().durability(125)));
    public static final RegistryObject<Item> UAV_CONTROLLER = registerItem(AllTabs.Category.MISC, "uav_controller", () -> new UavControllerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FIGURE_BOX = registerItem(AllTabs.Category.MISC, "figure_box", () -> new FigureBoxItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> AMMO_MACHINE_GUN = registerItem(AllTabs.Category.MISC, "ammo_machine_gun", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.MACHINE_GUN));
    public static final RegistryObject<Item> AMMO_AUTO_CANNON = registerItem(AllTabs.Category.MISC, "ammo_auto_cannon", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.AUTO_CANNON));
    public static final RegistryObject<Item> AMMO_GRENADE = registerItem(AllTabs.Category.MISC, "ammo_grenade", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.GRENADE));
    public static final RegistryObject<Item> AMMO_ARTILLERY = registerItem(AllTabs.Category.MISC, "ammo_artillery", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.ARTILLERY));
    public static final RegistryObject<Item> AMMO_ROCKET = registerItem(AllTabs.Category.MISC, "ammo_rocket", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.ROCKET));
    public static final RegistryObject<Item> AMMO_AERIAL_BOMB = registerItem(AllTabs.Category.MISC, "ammo_aerial_bomb", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.AERIAL_BOMB));
    public static final RegistryObject<Item> AMMO_MISSILE = registerItem(AllTabs.Category.MISC, "ammo_missile", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.MISSILE));
    public static final RegistryObject<Item> AMMO_DECOY_FLARE = registerItem(AllTabs.Category.MISC, "ammo_decoy_flare", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.MISC));

    public static final RegistryObject<RepairToolItem> REPAIR_TOOL = registerItem(AllTabs.Category.MISC, "repair_tool", RepairToolItem::new);

    public static <T extends Item> RegistryObject<T> registerItem(AllTabs.Category category, String name, Supplier<T> item) {
        RegistryObject<T> registryObject = ITEMS.register(name, item);
        if (AllTabs.Category.MISC.equals(category)) {
            AllTabs.MISC_ITEMS.add(registryObject);
        }
        AllItems.ITEMS_LOOKUP.put(name, registryObject);
        return registryObject;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
