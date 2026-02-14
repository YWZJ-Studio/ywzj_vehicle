package org.ywzj.vehicle.client.render.animation;

import com.maydaymemory.mae.basic.Animation;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Keyframe;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.IAnimationState;
import com.maydaymemory.mae.control.runner.PlayingState;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.client.render.animation.util.PoseBlenders;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Function;

// 用于自动分配多个轨道混合播放的工具
public class MultiAnimationRunner<T extends Animation> {

    private final int maxRunners;
    private final ConcurrentLinkedDeque<AnimationRunner> animationRunners = new ConcurrentLinkedDeque<>();
    private final Function<T, AnimationRunner> runnerSupplier;

    public MultiAnimationRunner(Function<T, AnimationRunner> runnerSupplier) {
        this(runnerSupplier, 5);
    }

    public MultiAnimationRunner(Function<T, AnimationRunner> runnerSupplier, int maxRunners) {
        this.maxRunners = maxRunners;
        this.runnerSupplier = runnerSupplier;
    }

    public void play(T animation, IAnimationState state) {
        AnimationRunner runnerToUse = runnerSupplier.apply(animation);
        if (animationRunners.size() >= maxRunners) {
            animationRunners.removeFirst();
        }
        animationRunners.addLast(runnerToUse);
        runnerToUse.setState(new PlayingState(System::nanoTime, () -> state));
    }

    public void tick() {
        for (AnimationRunner runner : animationRunners) {
            runner.tick();
        }
        // 移除已停止的runner
        animationRunners.removeIf(runner -> runner.getAnimationContext().isEnd());
    }

    public Pose evaluatePose() {
        Pose blendedPose = null;
        for (AnimationRunner runner : animationRunners) {
            Pose pose = runner.evaluate();
            if (blendedPose == null) {
                blendedPose = pose;
            } else {
                blendedPose = PoseBlenders.BLENDER.blend(blendedPose, pose);
            }
        }
        return blendedPose != null ? blendedPose : DummyPose.INSTANCE;
    }

    public void clip(String channelName, Consumer<Iterable<Keyframe<ResourceLocation>>> consumer) {
        for (AnimationRunner runner : animationRunners) {
            Iterable<Keyframe<ResourceLocation>> clip = runner.clip(channelName);
            if (clip != null) {
                consumer.accept(clip);
            }
        }
    }

}
