package org.ywzj.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ywzj.vehicle.all.*;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.resource.VehiclePackLoader;

@Mod(YwzjVehicle.MOD_ID)
public class YwzjVehicle {

    public static final String MOD_ID = "ywzj_vehicle";
    public static final String PROTOCOL = "1.0";
    public static final Logger LOGGER = LogManager.getLogger(YwzjVehicle.class);

    public YwzjVehicle(IEventBus modEventBus) {
        Dist side = FMLLoader.getDist();
        VehiclePackLoader.INSTANCE.packType = side.isClient() ? PackType.CLIENT_RESOURCES : PackType.SERVER_DATA;
        VehiclePackLoader.INSTANCE.scanVehiclePacks();
        AllConfigs.register(ModLoadingContext.get());
        AllBlocks.register(modEventBus);
        AllBlockEntities.register(modEventBus);
        AllItems.register(modEventBus);
        AllEntities.register(modEventBus);
        AllFluids.register(modEventBus);
        AllSounds.register(modEventBus);
        AllPartUnitType.register(modEventBus);
        AllVehicleWeaponTypes.register(modEventBus);
        AllVehicleDataTypes.register(modEventBus);
        AllParticleTypes.register(modEventBus);
        AllDisplayTypes.register(modEventBus);
        AllTabs.register(modEventBus);
        AllRecipe.register(modEventBus);
        AllDataComponents.register(modEventBus);
        initCompat();
        modEventBus.register(Channel.class);
    }

    public void initCompat() {
//        SuperbWarfareCompat.init();
//        CreateCompat.init();
    }

    public static ResourceLocation modLocation(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    public static ResourceLocation resourceLocation(String location) {
        return ResourceLocation.parse(location);
    }

}
