package org.ywzj.vehicle.client.render.animation.util;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.PauseState;
import com.maydaymemory.mae.control.runner.PlayingState;
import org.ywzj.vehicle.vehicle.parts.SwitchableUnit;

// 简单的 SwitchableRunner 管理器，用于管理门、起落架等可开关结构的动画 Runner
public class SwitchableRunner {
    private final SwitchableUnit<?> unit;
    private AnimationRunner runner;
    private boolean lastState;
    private boolean invert;

    public SwitchableRunner(SwitchableUnit<?> unit, BedrockAnimation animation, boolean invert) {
        this.unit = unit;
        this.invert = invert;
        this.lastState = unit.isOn();
        AnimationContext animContext = new AnimationContext(animation.getSpecifiedEndTimeS());
        runner = new AnimationRunner(animation, animContext);

        boolean effectiveState = unit.isOn();
        if (invert) {
            effectiveState = !effectiveState;
        }
        if (effectiveState) {
            animContext.setProgress(animation.getSpecifiedEndTimeS());
        } else {
            animContext.setProgress(0);
        }
        runner.setState(new PauseState());
        lastState = unit.isOn();
    }

    public void setRunner(AnimationRunner runner) {
        this.runner = runner;
    }

    public void tick() {
        if (runner != null) {
            runner.tick();
        }

        if (unit == null) {
            return;
        }

        if (unit.isOn() != lastState) {
            float speed = unit.isOn() ? 1.0f : -1.0f;
            if (invert) {
                speed = -speed;
            }
            PlayingState playingState = new PlayingState(System::nanoTime, PauseState::new);
            playingState.setSpeed(speed);
            runner.setState(playingState);
            lastState = unit.isOn();
        }
    }

    public Pose evaluate() {
        if (runner != null) {
            return runner.evaluate();
        }
        return DummyPose.INSTANCE;
    }

    public boolean isInvert() {
        return invert;
    }
}
