package org.ywzj.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ywzj.vehicle.all.*;
import org.ywzj.vehicle.network.Channel;

@Mod(YwzjVehicle.MOD_ID)
public class YwzjVehicle {

    public static final String MOD_ID = "ywzj_vehicle";
    public static final String PROTOCOL = "1.0";
    public static final String CHANNEL = "ywzj_vehicle_channel";
    public static final Logger LOGGER = LogManager.getLogger(YwzjVehicle.class);

    @SuppressWarnings("removal")
    public YwzjVehicle() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        AllConfigs.register(ModLoadingContext.get());
        AllVehicles.register();
        IEventBus modEventBus = context.getModEventBus();
        AllBlocks.register(modEventBus);
        AllItems.register(modEventBus);
        AllEntities.register(modEventBus);
        AllBlockEntities.register(modEventBus);
        AllSounds.register(modEventBus);
        AllTabs.register(modEventBus);
        AllPartUnitType.register(modEventBus);
        AllVehicleWeaponTypes.register(modEventBus);
        AllVehicleDataTypes.register(modEventBus);
        AllParticleTypes.register(modEventBus);
        modEventBus.register(Channel.class);
    }

    public static ResourceLocation modLoc(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

}
