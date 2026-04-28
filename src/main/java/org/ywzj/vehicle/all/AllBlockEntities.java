package org.ywzj.vehicle.all;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;

public class AllBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YwzjVehicle.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FigureBoxBlockEntity>> FIGURE_BOX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("figure_box_block_entity", () ->
                    BlockEntityType.Builder.of(
                                    FigureBoxBlockEntity::new,
                                    AllBlocks.FIGURE_BOX_BLOCK.get(),
                                    Blocks.AIR)
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineMaxBlockEntity>> MACHINE_MAX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("machine_max_block_entity", () ->
                    BlockEntityType.Builder.of(MachineMaxBlockEntity::new,
                                    AllBlocks.MACHINE_MAX_BLOCK.get(),
                                    Blocks.AIR)
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

}
