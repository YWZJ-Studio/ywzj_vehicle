package org.ywzj.vehicle.client.render.animation;

import com.maydaymemory.mae.basic.*;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.IAnimationState;
import com.maydaymemory.mae.control.runner.PlayingState;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

public class MultiAnimationRunner<T extends Animation> {

    public static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
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
                blendedPose = BLENDER.blend(blendedPose, pose);
            }
        }
        return blendedPose != null ? blendedPose : DummyPose.INSTANCE;
    }

    public Iterable<Keyframe<ResourceLocation>> clip(String channelName) {
        return animationRunners.stream()
                .findFirst()
                .<Iterable<Keyframe<ResourceLocation>>>map(animationRunner -> animationRunner.clip(channelName))
                .orElse(null);
    }

}
