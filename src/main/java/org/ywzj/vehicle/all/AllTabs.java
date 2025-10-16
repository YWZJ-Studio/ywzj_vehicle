package org.ywzj.vehicle.all;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AllTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YwzjVehicle.MOD_ID);

    public static final List<Supplier<? extends ItemLike>> MISC_ITEMS = new ArrayList<>();

    public static final RegistryObject<CreativeModeTab> TAB_MISC = TABS.register("tab_misc", () ->
            CreativeModeTab
                    .builder()
                    .title(Component.translatable("tab.misc"))
                    .icon(AllItems.FUEL_TANK.get()::getDefaultInstance)
                    .displayItems((displayParams, output) -> MISC_ITEMS.forEach(itemLike -> output.accept(itemLike.get())))
                    .build());

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }

    public enum Category {
        MISC
    }

}
