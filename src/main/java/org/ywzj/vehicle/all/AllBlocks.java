package org.ywzj.vehicle.all;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.block.TestBlock;

import java.util.function.Supplier;

public class AllBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, YwzjVehicle.MOD_ID);

    public static final RegistryObject<Block> TEST_BLOCK = registerBlock(AllTabs.Category.MISC, "test_block", TestBlock::new);

    public static <T extends Block> RegistryObject<Block> registerBlock(AllTabs.Category category, String name, Supplier<T> block) {
        RegistryObject<Block> toReturn = BLOCKS.register(name, block);
        registerBlockItem(category, name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(AllTabs.Category category, String name, RegistryObject<T> block) {
        RegistryObject<Item> registryObject = AllItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        if (category.equals(AllTabs.Category.MISC)) {
            AllTabs.MISC_ITEMS.add(registryObject);
        }
        AllItems.ITEMS_LOOKUP.put(name, registryObject);
        return registryObject;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
