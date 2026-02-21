package org.ywzj.vehicle.client.render.animation.runner;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.control.Tickable;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.IAnimationState;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.client.render.animation.util.AnimationPlayType;
import org.ywzj.vehicle.client.render.animation.util.PoseHelper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

// todo 枚举不方便脚本使用，后续可以改成int
// 一个简单的基岩播放动画管理器
public class AnimationRunnerHolder implements Tickable {

    public static final String DEFAULT_TRACK_NAME = "main";
    // 自动动画轨道
    private final MultiAnimationRunner<BedrockAnimation> multiAnimationRunner = new MultiAnimationRunner<>(animation -> new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS())));
    // 独立动画轨道
    private final ConcurrentHashMap<String, AnimationRunner> namedRunner = new ConcurrentHashMap<>();
    private final Function<String, BedrockAnimation> animationProvider;
    private Consumer<Iterable<Keyframe<ResourceLocation>>> soundProcessor;

    public AnimationRunnerHolder(Function<String, BedrockAnimation> animationProvider) {
        this.animationProvider = animationProvider;
    }

    // 推进动画状态
    @Override
    public void tick() {
        // 推进自动动画轨道
        multiAnimationRunner.tick();
        // 推进独立动画轨道
        namedRunner.values().forEach(AnimationRunner::tick);

        if (soundProcessor != null) {
            multiAnimationRunner.clip(BedrockAnimation.SOUND_CHANNEL_NAME, soundProcessor);

            for (AnimationRunner runner : namedRunner.values()) {
                Iterable<Keyframe<ResourceLocation>> namedSounds = runner.clip(BedrockAnimation.SOUND_CHANNEL_NAME);
                if (namedSounds != null) {
                    soundProcessor.accept(namedSounds);
                }
            }
        }
    }

    public void setSoundProcessor(Consumer<Iterable<Keyframe<ResourceLocation>>> soundProcessor) {
        this.soundProcessor = soundProcessor;
    }

    public void playAnimation(String track, String animationName, @NotNull IAnimationState state) {
        BedrockAnimation animation = animationProvider.apply(animationName);
        if (animation != null) {
            AnimationRunner runner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
            runner.setState(state);
            namedRunner.put(track, runner);
        }
    }

    /**
     * 在默认轨道上播放动画
     * @param animationName 动画名称
     * @param type 播放类型
     */
    public void playAnimation(String animationName, @NotNull AnimationPlayType type) {
        BedrockAnimation animation = animationProvider.apply(animationName);
        if (animation != null) {
            this.playAnimation(DEFAULT_TRACK_NAME, animation, type);
        }
    }

    /**
     * 在默认轨道上播放动画
     * @param animation 动画
     * @param type 播放类型
     */
    public void playAnimation(BedrockAnimation animation, @NotNull AnimationPlayType type) {
        this.playAnimation(DEFAULT_TRACK_NAME, animation, type);
    }

    /**
     * 在指定轨道上播放动画
     * @param track 轨道名称
     * @param animation 动画
     * @param type 播放类型
     */
    public void playAnimation(String track, BedrockAnimation animation, @NotNull AnimationPlayType type) {
        AnimationRunner runner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
        runner.setState(type.state());
        namedRunner.put(track, runner);
    }

    /**
     * 使用自动分配轨道播放动画
     * @param animationName 动画名称
     * @param type 播放类型
     */
    public void pushAnimation(String animationName, @NotNull AnimationPlayType type) {
        BedrockAnimation animation = animationProvider.apply(animationName);
        if (animation != null) {
            multiAnimationRunner.play(animation, type.state());
        }
    }

    /**
     * 使用自动分配轨道播放动画
     * @param animationName 动画名称
     * @param type 播放类型
     */
    public void pushAnimation(String animationName, @NotNull String type) {
        AnimationPlayType playType = AnimationPlayType.fromString(type);
        if (playType != null) {
            pushAnimation(animationName, playType);
        }
    }

    /**
     * 使用自动混合轨道播放动画
     * @param animation 动画对象
     * @param type 播放类型
     */
    public void pushAnimation(BedrockAnimation animation, @NotNull AnimationPlayType type) {
        multiAnimationRunner.play(animation, type.state());
    }

    public PoseHelper getTrackPose(String track) {
        AnimationRunner runner = namedRunner.get(track);
        if (runner != null) {
            return new PoseHelper(runner.evaluate());
        }
        return PoseHelper.DUMMY;
    }

    public PoseHelper getMultiRunnerPose() {
        return new PoseHelper(multiAnimationRunner.evaluatePose());
    }

    /**
     * 获取指定轨道的动画播放器
     * @param track 轨道名称
     * @return 动画播放器，如果轨道不存在则返回null
     */
    @Nullable
    public AnimationRunner getAnimationRunner(String track) {
        return namedRunner.get(track);
    }

}
