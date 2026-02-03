package org.ywzj.vehicle.client.render.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.Animation;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.Tickable;
import com.maydaymemory.mae.control.runner.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.ywzj.vehicle.client.render.animation.MultiAnimationRunner.BLENDER;

/**
 * 一个简单的实体bedrock动画上下文，除了提供实体信息，还承担轨道分配的功能<br/>
 * 包含单个AnimationRunner和基岩音效播放实现，以及一个简单的事件处理器<br/>
 * @param <T>
 */
public class EntityContext<T extends Entity> implements Tickable {

    private final Map<String, BedrockAnimation> animations;
    private final T entity;
    private final MultiAnimationRunner<BedrockAnimation> multiAnimationRunner =
            new MultiAnimationRunner<>(animation -> new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS())));
    private final ConcurrentHashMap<Animation, AnimationRunner> runners = new ConcurrentHashMap<>();

    public EntityContext(T entity, Map<String, BedrockAnimation> animations) {
        this.entity = entity;
        this.animations = animations;
    }

    public T getEntity() {
        return entity;
    }

    public BedrockAnimation getAnimation(String name) {
        return animations.get(name);
    }

    public AnimationRunner getAnimationRunner(String name) {
        BedrockAnimation animation = getAnimation(name);
        if (animation != null) {
            return runners.get(animation);
        }
        return null;
    }

    public AnimationRunner addAnimationRunner(String name, @NotNull IAnimationState state) {
        BedrockAnimation animation = getAnimation(name);
        if (animation != null) {
            AnimationRunner runner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
            runner.setState(state);
            runners.put(animation, runner);
            return runner;
        }
        return null;
    }

    public void playMultiAnimation(String name, @NotNull IAnimationState state) {
        BedrockAnimation animation = getAnimation(name);
        if (animation != null) {
            multiAnimationRunner.play(animation, state);
        }
    }

    public void playMultiAnimation(String name) {
        this.playMultiAnimation(name, new PlayingState(System::nanoTime, StopState::new));
    }

    public Pose evaluatePose() {
        Pose multiAnimationPose = multiAnimationRunner.evaluatePose();
        for (AnimationRunner runner : runners.values()) {
            Pose pose = runner.evaluate();
            if (multiAnimationPose == null) {
                multiAnimationPose = pose;
            } else {
                multiAnimationPose = BLENDER.blend(multiAnimationPose, pose);
            }
        }
        return multiAnimationPose;
    }

    @Override
    public void tick() {
        multiAnimationRunner.tick();
        runners.values().forEach(AnimationRunner::tick);
        Level level = entity.level();
        if (!level.isClientSide()) {
            return;
        }
        // 基岩音效
        Iterable<Keyframe<ResourceLocation>> sounds = multiAnimationRunner.clip(BedrockAnimation.SOUND_CHANNEL_NAME);
        if (sounds != null) {
            for (Keyframe<ResourceLocation> keyframe : sounds) {
                BlockPos pos = entity.getOnPos();
                SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(keyframe.getValue());
                level.playSound(null, pos, soundEvent, SoundSource.PLAYERS);
            }
        }
    }

}
