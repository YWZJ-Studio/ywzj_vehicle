package org.ywzj.vehicle.client.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.Vehicle;
import org.ywzj.vehicle.client.manager.BedrockModelRegister;
import org.ywzj.vehicle.client.manager.BedrockModelRegisterEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BedrockModelLoader {

    public static final ResourceLocation TEST_MODEL = Vehicle.modLoc("bedrock/block/test");
    public static final ResourceLocation LAV150 = Vehicle.modLoc("bedrock/entity/lav150");
    public static final ResourceLocation ZTL11 = Vehicle.modLoc("bedrock/entity/ztl11");
    public static final ResourceLocation MOTORCYCLE = Vehicle.modLoc("bedrock/entity/motorcycle");

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(BedrockModelRegisterEvent event) {
        event.register(TEST_MODEL, BedrockModel::new);
        event.register(LAV150, BedrockModel::new);
        event.register(ZTL11, BedrockModel::new);
        event.register(MOTORCYCLE, BedrockModel::new);
    }

    public static BedrockModel getModel(ResourceLocation location) {
        return BedrockModelRegister.INSTANCE.getModel(location);
    }

}
