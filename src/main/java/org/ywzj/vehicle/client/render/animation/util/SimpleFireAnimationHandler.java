package org.ywzj.vehicle.client.render.animation.util;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.ywzj.vehicle.client.render.animation.MultiAnimationRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleFireAnimationHandler {
    private final MultiAnimationRunner<BedrockAnimation> multiAnimationRunner = new MultiAnimationRunner<>(animation -> new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS())));
    private final Function<String, BedrockAnimation> animationProvider;
    private final Map<String, List<String>> fireAnimations;
    private Consumer<Iterable<Keyframe<ResourceLocation>>> soundProcessor;
    protected final RandomSource random = RandomSource.create();

    public SimpleFireAnimationHandler(Function<String, BedrockAnimation> animationProvider, Map<String, List<String>> fireAnimations) {
        this.animationProvider = animationProvider;
        this.fireAnimations = fireAnimations;
    }

    public void setSoundProcessor(Consumer<Iterable<Keyframe<ResourceLocation>>> soundProcessor) {
        this.soundProcessor = soundProcessor;
    }

    public void processFireAnimation(Set<String> events) {
        for (String event : events) {
            var animations = fireAnimations.get(event);
            if (animations != null && !animations.isEmpty()) {
                String animationName = animations.get(random.nextInt(animations.size()));
                BedrockAnimation animation = animationProvider.apply(animationName);
                if (animation != null) {
                    multiAnimationRunner.play(animation, AnimationPlayType.PLAY_ONCE_STOP.state());
                }
            }
        }
    }

    public void tick(Set<String> events) {
        processFireAnimation(events);
        multiAnimationRunner.tick();
        if (soundProcessor != null) {
            multiAnimationRunner.clip(BedrockAnimation.SOUND_CHANNEL_NAME, soundProcessor);
        }
    }

    public Pose evaluate() {
        return multiAnimationRunner.evaluatePose();
    }
}
