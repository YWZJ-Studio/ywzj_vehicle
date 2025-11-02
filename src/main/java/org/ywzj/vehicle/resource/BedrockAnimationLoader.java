package org.ywzj.vehicle.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockAnimationEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.BedrockAnimationResourceSet;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.RawResourceLoaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class BedrockAnimationLoader {

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(RegisterBedrockAnimationEvent event) {
        event.register(
                YwzjVehicle.modLoc("entity/ztz99a.animation"),
                YwzjVehicle.modLoc("entity/ztz99a.animation"),
                RawResourceLoaders.COMMON_LOADER
        );
    }

    public static List<BedrockAnimation> getAnimations(ResourceLocation location) {
        return BedrockAnimationResourceSet.getInstance().getAnimations(location);
    }

}
