package org.ywzj.vehicle.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockModelResourceSet;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.RawResourceLoaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllVehicles;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BedrockModelLoader {

    public static final ResourceLocation TEST_MODEL = YwzjVehicle.modLoc("block/test");

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(RegisterBedrockModelEvent event) {
        event.register(TEST_MODEL, RawResourceLoaders.COMMON_LOADER);

        //todo: 统一武器注册
        event.register(YwzjVehicle.modLoc("entity/missile_akd10"), RawResourceLoaders.COMMON_LOADER);
        event.register(YwzjVehicle.modLoc("entity/rocket_57mm"), RawResourceLoaders.COMMON_LOADER);
        event.register(YwzjVehicle.modLoc("entity/aerial_bomb"), RawResourceLoaders.COMMON_LOADER);

        AllVehicles.getVehicleTypes().forEach(vehicle -> {
            event.register(vehicle.getVisualBedrockModel(), RawResourceLoaders.COMMON_LOADER);
            event.register(vehicle.getStructureBedrockModel(), RawResourceLoaders.COMMON_LOADER);
        });
    }

    public static BedrockModel getModel(ResourceLocation location) {
        return BedrockModelResourceSet.getInstance().getModel(location);
    }

}
