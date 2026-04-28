package org.ywzj.vehicle.all;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.item.*;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class AllItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ITEM, YwzjVehicle.MOD_ID);
    public static final LinkedHashMap<String, DeferredHolder<Item, ? extends Item>> ITEMS_LOOKUP = new LinkedHashMap<>();

    public static final DeferredHolder<Item, VehicleSpawnItem> VEHICLE_SPAWN_ITEM = registerItem(AllTabs.Category.VEHICLE, "vehicle_spawn_item", () -> new VehicleSpawnItem(new Item.Properties().durability(1)));
    public static final DeferredHolder<Item, PlainTextureItem> PLAIN_TEXTURE_ITEM = registerItem(AllTabs.Category.NONE, "plain_texture_item", () -> new PlainTextureItem(new Item.Properties().durability(1)));
    public static final DeferredHolder<Item, Item> FUEL_TANK = registerItem(AllTabs.Category.MISC, "fuel_tank", () -> new FuelTankItem(new Item.Properties().durability(125)));
    public static final DeferredHolder<Item, Item> UAV_CONTROLLER = registerItem(AllTabs.Category.MISC, "uav_controller", () -> new UavControllerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> FIGURE_BOX = registerItem(AllTabs.Category.MISC, "figure_box", () -> new FigureBoxItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> MACHINE_MAX_BLOCK = registerItem(AllTabs.Category.MISC, "machine_max_block", () -> new MachineMaxBlockItem(AllBlocks.MACHINE_MAX_BLOCK.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, RepairToolItem> REPAIR_TOOL = registerItem(AllTabs.Category.MISC, "repair_tool", RepairToolItem::new);
    public static final DeferredHolder<Item, ModdingToolItem> MODDING_TOOL = registerItem(AllTabs.Category.MISC, "modding_tool", () -> new ModdingToolItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, DecorationItem> DECORATION_ITEM = registerItem(AllTabs.Category.MISC, "decoration_item", DecorationItem::new);
    public static final DeferredHolder<Item, Item> AMMO_CREATIVE = registerItem(AllTabs.Category.MISC, "ammo_creative", () -> new AmmoItem(new Item.Properties().stacksTo(1), AmmoItem.AmmoType.MACHINE_GUN));
    public static final DeferredHolder<Item, Item> AMMO_MACHINE_GUN = registerItem(AllTabs.Category.MISC, "ammo_machine_gun", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.MACHINE_GUN));
    public static final DeferredHolder<Item, Item> AMMO_AUTO_CANNON = registerItem(AllTabs.Category.MISC, "ammo_auto_cannon", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.AUTO_CANNON));
    public static final DeferredHolder<Item, Item> AMMO_GRENADE = registerItem(AllTabs.Category.MISC, "ammo_grenade", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.GRENADE));
    public static final DeferredHolder<Item, Item> AMMO_ARTILLERY = registerItem(AllTabs.Category.MISC, "ammo_artillery", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.ARTILLERY));
    public static final DeferredHolder<Item, Item> AMMO_ROCKET = registerItem(AllTabs.Category.MISC, "ammo_rocket", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.ROCKET));
    public static final DeferredHolder<Item, Item> AMMO_AERIAL_BOMB = registerItem(AllTabs.Category.MISC, "ammo_aerial_bomb", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.AERIAL_BOMB));
    public static final DeferredHolder<Item, Item> AMMO_MISSILE = registerItem(AllTabs.Category.MISC, "ammo_missile", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.MISSILE));
    public static final DeferredHolder<Item, Item> AMMO_DECOY_FLARE = registerItem(AllTabs.Category.MISC, "ammo_decoy_flare", () -> new AmmoItem(new Item.Properties().stacksTo(64), AmmoItem.AmmoType.MISC));

    public static <T extends Item> DeferredHolder<Item, T> registerItem(AllTabs.Category category, String name, Supplier<T> item) {
        DeferredHolder<Item, T> registryObject = ITEMS.register(name, item);
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
