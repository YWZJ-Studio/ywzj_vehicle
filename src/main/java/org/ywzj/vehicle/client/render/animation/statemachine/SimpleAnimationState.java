package org.ywzj.vehicle.client.render.animation.statemachine;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.IAnimationState;
import com.maydaymemory.mae.control.statemachine.IAnimationTransition;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 简单状态机状态，用于辅助构建状态机状态
 * @param <T>
 */
public class SimpleAnimationState<T> implements IAnimationState<T> {
    private final List<IAnimationTransition<T>> transitions = new ArrayList<>();
    private final BiConsumer<T, IAnimationState<T>> onEnter;
    private final BiConsumer<T, IAnimationTransition<T>> onExit;
    private final Consumer<T> onUpdate;
    private final Function<T, Pose> evaluatePose;

    public SimpleAnimationState(BiConsumer<T, IAnimationState<T>> onEnter,
                                BiConsumer<T, IAnimationTransition<T>> onExit,
                                Consumer<T> onUpdate,
                                Function<T, Pose> evaluatePose) {
        this.onEnter = onEnter;
        this.onExit = onExit;
        this.onUpdate = onUpdate;
        this.evaluatePose = evaluatePose;
    }

    public Iterable<IAnimationTransition<T>> transitions() {
        return transitions;
    }

    public void onEnter(T context, IAnimationState<T> fromState) {
        onEnter.accept(context, fromState);
    }

    public void onExit(T context, IAnimationTransition<T> triggeredTransition) {
        onExit.accept(context, triggeredTransition);
    }

    public void onUpdate(T context) {
        onUpdate.accept(context);
    }

    public Pose evaluatePose(T context) {
        return evaluatePose.apply(context);
    }

    @ApiStatus.Internal
    public void addTransition(IAnimationTransition<T> transition) {
        this.transitions.add(transition);
    }

    public static class Builder<T> {
        private BiConsumer<T, IAnimationState<T>> onEnter = (ctx, from) -> {};
        private BiConsumer<T, IAnimationTransition<T>> onExit = (ctx, transition) -> {};
        private Consumer<T> onUpdate = (ctx) -> {};
        private Function<T, Pose> evaluatePose = (ctx) -> DummyPose.INSTANCE;

        public Builder<T> onEnter(BiConsumer<T, IAnimationState<T>> onEnter) {
            this.onEnter = onEnter;
            return this;
        }

        public Builder<T> onExit(BiConsumer<T, IAnimationTransition<T>> onExit) {
            this.onExit = onExit;
            return this;
        }

        public Builder<T> onUpdate(Consumer<T> onUpdate) {
            this.onUpdate = onUpdate;
            return this;
        }

        public Builder<T> evaluatePose(Function<T, Pose> evaluatePose) {
            this.evaluatePose = evaluatePose;
            return this;
        }

        public SimpleAnimationState<T> build() {
            return new SimpleAnimationState<>(onEnter, onExit, onUpdate, evaluatePose);
        }
    }
}
