package org.ywzj.vehicle.all;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.ywzj.vehicle.YwzjVehicle;

public class AllDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, YwzjVehicle.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID = DATA_COMPONENTS
            .register("fluid", () -> DataComponentType.<SimpleFluidContent>builder().persistent(SimpleFluidContent.CODEC).build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }

}
