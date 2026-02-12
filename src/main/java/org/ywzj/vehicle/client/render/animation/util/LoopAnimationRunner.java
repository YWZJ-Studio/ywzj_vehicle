package org.ywzj.vehicle.client.render.animation.util;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;

public class LoopAnimationRunner {
    private AnimationRunner runner;

    public LoopAnimationRunner(BedrockAnimation animation) {
        AnimationRunner animRunner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
        animRunner.setState(AnimationPlayType.LOOP.state());
        this.runner = animRunner;
    }

    public void tick() {
        if (runner != null) {
            runner.tick();
        }
    }

    public Pose evaluate() {
        if (runner != null) {
            return runner.evaluate();
        }
        return null;
    }

    public void setRunner(AnimationRunner runner) {
        this.runner = runner;
    }
}
