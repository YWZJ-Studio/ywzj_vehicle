package org.ywzj.vehicle.bedrock.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.BedrockAnimationFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.Map;
import java.util.function.Function;

public class BedrockAnimationRegisterEvent extends Event implements IModBusEvent {
    private final BedrockAnimationSet animationSet;

    public BedrockAnimationRegisterEvent(BedrockAnimationSet animationSet) {
        this.animationSet = animationSet;
    }

    public void register(ResourceLocation location, Function<BedrockAnimationFile, Map<String, BedrockAnimation>> function) {
        animationSet.addAnimation(location, function);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }
}
