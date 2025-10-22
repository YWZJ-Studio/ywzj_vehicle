package org.ywzj.vehicle.bedrock.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.animation.BedrockAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;

import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class BedrockAnimationLoader {

    public static final ResourceLocation TEST_ANIMATION = YwzjVehicle.modLoc("bedrock/test");

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(BedrockAnimationRegisterEvent event) {
//        event.register(TEST_ANIMATION, pojo -> {
//            BedrockModel model = BedrockModelLoader.getModel(BedrockModelLoader.TEST_MODEL);
//            List<BedrockAnimation> animation = Animations.createAnimation(pojo, new BedrockModelBoneIndexProvider(model));
//            return ImmutableMap.copyOf(animation.stream().collect(Collectors.toMap(BedrockAnimation::getName, a -> a)));
//        });
    }

    public static Map<String, BedrockAnimation> getAnimations(ResourceLocation location) {
        return BedrockAnimationRegister.INSTANCE.getAnimations(location);
    }

}
