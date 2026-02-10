package org.ywzj.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ywzj.vehicle.all.*;
import org.ywzj.vehicle.compat.SuperbWarfareCompat;
import org.ywzj.vehicle.compat.ValkyrienSkiesCompat;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.resource.VehiclePackLoader;

@Mod(YwzjVehicle.MOD_ID)
public class YwzjVehicle {

    public static final String MOD_ID = "ywzj_vehicle";
    public static final String PROTOCOL = "1.0";
    public static final String CHANNEL = "ywzj_vehicle_channel";
    public static final Logger LOGGER = LogManager.getLogger(YwzjVehicle.class);

    @SuppressWarnings("removal")
    public YwzjVehicle() {
        Dist side = FMLLoader.getDist();
        VehiclePackLoader.INSTANCE.packType = side.isClient() ? PackType.CLIENT_RESOURCES : PackType.SERVER_DATA;
        VehiclePackLoader.INSTANCE.scanVehiclePacks();
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        AllConfigs.register(ModLoadingContext.get());
        IEventBus modEventBus = context.getModEventBus();
        AllBlocks.register(modEventBus);
        AllItems.register(modEventBus);
        AllEntities.register(modEventBus);
        AllBlockEntities.register(modEventBus);
        AllFluids.register(modEventBus);
        AllSounds.register(modEventBus);
        AllPartUnitType.register(modEventBus);
        AllVehicleWeaponTypes.register(modEventBus);
        AllVehicleDataTypes.register(modEventBus);
        AllParticleTypes.register(modEventBus);
        AllDisplayTypes.register(modEventBus);
        AllTabs.register(modEventBus);
        AllRecipe.register(modEventBus);
        initCompat();
        modEventBus.register(Channel.class);
    }

    public void initCompat() {
        SuperbWarfareCompat.init();
        ValkyrienSkiesCompat.init();
    }

    @SuppressWarnings("removal")
    public static ResourceLocation modLocation(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

    @SuppressWarnings("removal")
    public static ResourceLocation resourceLocation(String location) {
        return new ResourceLocation(location);
    }

}
