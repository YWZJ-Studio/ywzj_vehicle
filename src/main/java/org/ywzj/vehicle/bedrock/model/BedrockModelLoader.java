package org.ywzj.vehicle.bedrock.model;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllVehicles;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BedrockModelLoader {

    public static final ResourceLocation TEST_MODEL = YwzjVehicle.modLoc("bedrock/block/test");

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(BedrockModelRegisterEvent event) {
        event.register(TEST_MODEL, BedrockModel::new);

        //todo: 统一武器注册
        event.register(YwzjVehicle.modLoc("bedrock/entity/missile_akd10"), BedrockModel::new);
        event.register(YwzjVehicle.modLoc("bedrock/entity/rocket_57mm"), BedrockModel::new);
        event.register(YwzjVehicle.modLoc("bedrock/entity/aerial_bomb"), BedrockModel::new);

        AllVehicles.getVehicleTypes().forEach(vehicle -> {
            event.register(vehicle.getVisualBedrockModel(), BedrockModel::new);
            event.register(vehicle.getStructureBedrockModel(), BedrockModel::new);
        });
    }

    public static BedrockModel getModel(ResourceLocation location) {
        return BedrockModelRegister.INSTANCE.getModel(location);
    }

}
