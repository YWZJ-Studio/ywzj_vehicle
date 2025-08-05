package org.ywzj.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.all.AllBlocks;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllTabs;

@Mod(Vehicle.MOD_ID)
public class Vehicle {

    public static final String MOD_ID = "ywzj_vehicle";

    public Vehicle(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        AllBlocks.register(modEventBus);
        AllItems.register(modEventBus);
        AllBlockEntities.register(modEventBus);
        AllTabs.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation modLoc(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

}
