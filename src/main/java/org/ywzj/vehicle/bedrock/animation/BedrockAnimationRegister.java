package org.ywzj.vehicle.bedrock.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.animation.BedrockAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;

import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BedrockAnimationRegister {
    public static BedrockAnimationRegister INSTANCE = null;
    private final BedrockAnimationSet animationSet;

    private BedrockAnimationRegister(BedrockAnimationSet animationSet) {
        this.animationSet = animationSet;
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListenersEvent(RegisterClientReloadListenersEvent event) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager instanceof ReloadableResourceManager manager) {
            INSTANCE = new BedrockAnimationRegister(new BedrockAnimationSet());
            ModLoader.get().postEvent(new BedrockAnimationRegisterEvent(INSTANCE.animationSet));
            // 将注册冻结
            INSTANCE.animationSet.immutableKnowLocations();
            manager.listeners.add(INSTANCE.animationSet);
        }
    }

    public Map<String, BedrockAnimation> getAnimations(ResourceLocation location) {
        return animationSet.getAnimations().get(location);
    }

    public Set<ResourceLocation> getAllAnimationKey() {
        return animationSet.getAnimations().keySet();
    }
}
