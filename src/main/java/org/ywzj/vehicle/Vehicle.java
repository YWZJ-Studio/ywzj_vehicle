package org.ywzj.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ywzj.vehicle.all.*;
import org.ywzj.vehicle.network.Channel;

@Mod(Vehicle.MOD_ID)
public class Vehicle {

    public static final String MOD_ID = "ywzj_vehicle";
    public static final String PROTOCOL = "1.0";
    public static final String CHANNEL = "ywzj_vehicle_channel";
    public static final Logger LOGGER = LogManager.getLogger(Vehicle.class);

    public Vehicle() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        IEventBus modEventBus = context.getModEventBus();
        AllBlocks.register(modEventBus);
        AllItems.register(modEventBus);
        AllEntities.register(modEventBus);
        AllBlockEntities.register(modEventBus);
        AllSounds.register(modEventBus);
        AllTabs.register(modEventBus);
        AllWeaponUnitType.register(modEventBus);
        modEventBus.register(Channel.class);
//        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation modLoc(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

}
