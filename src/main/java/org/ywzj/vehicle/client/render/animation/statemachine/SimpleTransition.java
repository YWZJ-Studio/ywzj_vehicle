package org.ywzj.vehicle.client.render.animation.statemachine;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.SimpleInterpolatorBlender;
import com.maydaymemory.mae.control.blend.EasingBlendCurve;
import com.maydaymemory.mae.control.blend.IBlendCurve;
import com.maydaymemory.mae.control.statemachine.IAnimationState;
import com.maydaymemory.mae.control.statemachine.IAnimationTransition;
import com.maydaymemory.mae.control.statemachine.TransferOutStrategy;
import com.maydaymemory.mae.util.Easing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 简单状态机状态过渡实现，用于辅助构建状态机过渡
 * @param <T>
 */
public class SimpleTransition<T> implements IAnimationTransition<T> {
    public static final SimpleInterpolatorBlender TRANSITION_BLENDER =
            new SimpleInterpolatorBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    private final IAnimationState<T> target;
    private final float duration;
    private final TransferOutStrategy strategy;
    private final Predicate<T> predicate;
    private final Supplier<IBlendCurve> curve;
    private final Consumer<T> afterTrigger;
    private final InterpolatedPoseFunction<T> interpolatedPoseFunction;

    public SimpleTransition(IAnimationState<T> target,
                            float duration,
                            TransferOutStrategy strategy,
                            Predicate<T> predicate,
                            Supplier<IBlendCurve> curve,
                            Consumer<T> afterTrigger,
                            InterpolatedPoseFunction<T> interpolatedPoseFunction) {
        this.target = target;
        this.duration = duration;
        this.strategy = strategy;
        this.predicate = predicate;
        this.curve = curve;
        this.afterTrigger = afterTrigger;
        this.interpolatedPoseFunction = interpolatedPoseFunction;
    }

    @Override
    public boolean canTrigger(T context) {
        return predicate.test(context);
    }

    @Override
    public void afterTrigger(T context) {
        afterTrigger.accept(context);
    }

    @Override
    public IAnimationState<T> targetState() {
        return target;
    }

    @Override
    public IBlendCurve curve() {
        return curve.get();
    }

    @Override
    public float duration() {
        return duration;
    }

    @Override
    public TransferOutStrategy transferOutStrategy() {
        return strategy;
    }

    @Override
    public Pose getInterpolatedPose(T context, Pose fromPose, Pose toPose, float progress) {
        return interpolatedPoseFunction.getPose(context, fromPose, toPose, progress);
    }


    @FunctionalInterface
    public interface InterpolatedPoseFunction<T> {
        Pose getPose(T context, Pose fromPose, Pose toPose, float progress);
    }

    public static class Builder<T> {
        private IAnimationState<T> target;
        private List<IAnimationState<T>> from;
        private float duration = 0.3f;
        private TransferOutStrategy strategy = TransferOutStrategy.TO_STATE;
        private Predicate<T> predicate = (ctx) -> true;
        private Supplier<IBlendCurve> curve = () -> new EasingBlendCurve(Easing.LINEAR);
        private Consumer<T> afterTrigger = (state) -> {};
        private InterpolatedPoseFunction<T> interpolatedPoseFunction = (ctx, fromPose, toPose, progress) ->
                TRANSITION_BLENDER.blend(fromPose, toPose, progress);

        @SafeVarargs
        public final Builder<T> from(IAnimationState<T>... target) {
            List<IAnimationState<T>> list = new ArrayList<>(target.length);
            Collections.addAll(list, target);
            this.from = list;
            return this;
        }

        public Builder<T> target(IAnimationState<T> target) {
            this.target = target;
            return this;
        }

        public Builder<T> duration(float duration) {
            this.duration = duration;
            return this;
        }

        public Builder<T> strategy(TransferOutStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder<T> predicate(Predicate<T> predicate) {
            this.predicate = predicate;
            return this;
        }

        public Builder<T> curve(Supplier<IBlendCurve> curve) {
            this.curve = curve;
            return this;
        }

        public Builder<T> afterTrigger(Consumer<T> afterTrigger) {
            this.afterTrigger = afterTrigger;
            return this;
        }

        public Builder<T> interpolatedPose(InterpolatedPoseFunction<T> function) {
            this.interpolatedPoseFunction = function;
            return this;
        }

        public SimpleTransition<T> build() {
            var transition = new SimpleTransition<>(
                    target, duration, strategy, predicate,
                    curve, afterTrigger, interpolatedPoseFunction
            );
            for (IAnimationState<T> state : from) {
                if (state instanceof SimpleAnimationState<T> simpleState) {
                    simpleState.addTransition(transition);
                }
            }
            return transition;
        }
    }

}
