package org.ywzj.vehicle.client.render.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.DummyPose;
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
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 一个简单的实体bedrock动画上下文，除了提供实体信息，还承担轨道分配的功能<br/>
 * 包含单个AnimationRunner和基岩音效播放实现，以及一个简单的事件处理器<br/>
 * @param <T>
 */
public class EntityContext<T extends Entity> implements Tickable {
    private final Map<String, BedrockAnimation> animations;
    private final T entity;
    private AnimationRunner runner;

    public EntityContext(T entity, Map<String, BedrockAnimation> animations) {
        this.entity = entity;
        this.animations = animations;
    }

    @Nullable
    public AnimationRunner getRunner() {
        return runner;
    }

    public void setRunner(AnimationRunner runner) {
        this.runner = runner;
    }

    public BedrockAnimation getAnimation(String name) {
        return animations.get(name);
    }

    public AnimationRunner createRunner(String name) {
        BedrockAnimation animation = getAnimation(name);
        if (animation == null) {
            return null;
        }
        return new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
    }

    public void playAnimation(String name, @NotNull IAnimationState state) {
        BedrockAnimation animation = getAnimation(name);
        if (animation != null) {
            this.runner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
            this.runner.setState(state);
        }
    }

    public void playAnimation(String name) {
        this.playAnimation(name, new PlayingState(System::nanoTime, StopState::new));
    }

    public Pose evaluatePose() {
        if (runner != null) {
            runner.getAnimationContext().isEnd();
            return runner.evaluate();
        }
        return DummyPose.INSTANCE;
    }

    public T getEntity() {
        return entity;
    }

    @Override
    public void tick() {
        if (runner != null) {
            runner.tick();
            Level level = entity.level();
            if (!level.isClientSide()) {
                return;
            }
            // 基岩音效
            Iterable<Keyframe<ResourceLocation>> sounds = runner.clip(BedrockAnimation.SOUND_CHANNEL_NAME);
            if (sounds != null) {
                for (Keyframe<ResourceLocation> keyframe : sounds) {
                    BlockPos pos = entity.getOnPos();
                    SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(keyframe.getValue());
                    level.playSound(null, pos, soundEvent, SoundSource.PLAYERS);
                }
            }
        }
    }
}
