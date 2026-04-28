package org.ywzj.vehicle.all;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.block.MachineMaxBlock;

import java.util.function.Supplier;

public class AllBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK, YwzjVehicle.MOD_ID);

    public static final DeferredHolder<Block, Block> FIGURE_BOX_BLOCK = registerBlock(AllTabs.Category.MISC, "figure_box_block",
            () -> new FigureBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noOcclusion()), false);

    public static final DeferredHolder<Block, Block> MACHINE_MAX_BLOCK = registerBlock(AllTabs.Category.MISC, "machine_max_block",
            () -> new MachineMaxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noOcclusion()), false);

    public static <T extends Block> DeferredHolder<Block, T> registerBlock(AllTabs.Category category, String name, Supplier<T> block, boolean hasBlockItem) {
        DeferredHolder<Block, T> toReturn = BLOCKS.register(name, block);
        if (hasBlockItem) {
            registerBlockItem(category, name, toReturn);
        }
        return toReturn;
    }

    private static <T extends Block> DeferredHolder<Item, Item> registerBlockItem(AllTabs.Category category, String name, DeferredHolder<Block, T> block) {
        DeferredHolder<Item, Item> registryObject = AllItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
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
