package org.ywzj.vehicle.client.animation;

import com.maydaymemory.mae.basic.Animation;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.LoopingState;
import com.maydaymemory.mae.control.statemachine.*;
import com.maydaymemory.mae.util.Easing;

import java.util.List;

public class SelfTransferState implements IAnimationState<TestAnimationContext> {
    public static final SelfTransferState INSTANCE = new SelfTransferState();

    private SelfTransferState() {}

    @Override
    public Iterable<IAnimationTransition<TestAnimationContext>> transitions() {
        return List.of(new Transition());
    }

    @Override
    public void onEnter(TestAnimationContext testAnimationContext, IAnimationState<TestAnimationContext> iAnimationState) {
        AnimationRunner runner = testAnimationContext.getRunner();
        if (runner != null) {
            runner.setState(new LoopingState(System::nanoTime));
        }
    }

    @Override
    public void onExit(TestAnimationContext testAnimationContext, IAnimationTransition<TestAnimationContext> iAnimationTransition) {}

    @Override
    public void onUpdate(TestAnimationContext testAnimationContext) {}

    @Override
    public Pose evaluatePose(TestAnimationContext testAnimationContext) {
        AnimationRunner runner = testAnimationContext.getRunner();
        if (runner == null) {
            return DummyPose.INSTANCE;
        }
        return runner.evaluate();
    }

    public static class Transition implements IAnimationTransition<TestAnimationContext> {
        @Override
        public IAnimationState<TestAnimationContext> targetState() {
            return SelfTransferState.INSTANCE;
        }

        @Override
        public IBlendCurve curve() {
            return new EasingBlendCurve(Easing.LINEAR);
        }

        @Override
        public float duration() {
            return 0.3f;
        }

        @Override
        public TransferOutStrategy transferOutStrategy() {
            return TransferOutStrategy.TO_STATE;
        }

        @Override
        public boolean canTrigger(TestAnimationContext testAnimationContext) {
            if (testAnimationContext.needTransition) {
                testAnimationContext.needTransition = false;
                return true;
            }
            return false;
        }

        @Override
        public void afterTrigger(TestAnimationContext testAnimationContext) {
            testAnimationContext.snapshotVelocity();
            Animation animation = testAnimationContext.nextAnimation();
            AnimationRunner runner = new AnimationRunner(animation, new AnimationContext(animation.getEndTimeS()));
            testAnimationContext.setRunner(runner);
        }

        @Override
        public Pose getInterpolatedPose(TestAnimationContext testAnimationContext, Pose pose, Pose pose1, float v) {
            Pose targetVelocity = testAnimationContext.targetVelocityEstimatorNode.getVelocityPose();
            float time = duration() * v;
            //return pose;
            //return TestAnimationContext.blender2.blend(pose, pose1, v);
            return TestAnimationContext.blender.blend(pose, testAnimationContext.getVelocitySnapshot(), pose1, targetVelocity, time, duration());
        }
    }
}
