package org.ywzj.vehicle.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v2.event.RegisterV2BedrockResourcesEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = YwzjVehicle.MOD_ID)
public final class ParachuteModels {

    public static final ResourceLocation PARACHUTE_PACK = YwzjVehicle.modLocation("armor/parachute_pack");
    public static final ResourceLocation PARAGLIDER_CANOPY = YwzjVehicle.modLocation("entity/paraglider_canopy");
    public static final ResourceLocation PARACHUTE_PACK_TEXTURE = YwzjVehicle.modLocation("textures/bedrock/armor/parachute_pack.png");
    public static final ResourceLocation PARAGLIDER_CANOPY_TEXTURE = YwzjVehicle.modLocation("textures/bedrock/entity/paraglider_canopy.png");

    private ParachuteModels() {}

    @SubscribeEvent
    public static void registerModels(RegisterV2BedrockResourcesEvent event) {
        event.treeModel(PARACHUTE_PACK).register();
        event.bakedModel(PARAGLIDER_CANOPY).register();
    }

}
