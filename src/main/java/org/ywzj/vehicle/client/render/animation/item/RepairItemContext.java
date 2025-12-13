package org.ywzj.vehicle.client.render.animation.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.Tickable;
import com.maydaymemory.mae.control.runner.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class RepairItemContext implements Tickable {
    private final Map<String, BedrockAnimation> animations;
    private AnimationRunner runner;
    private boolean inited = false;

    public RepairItemContext(Map<String, BedrockAnimation> animations) {
        this.animations = animations;
    }

    @Override
    public void tick() {
        if (runner != null) {
            runner.tick();
        }
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

    public void playAnimationLoop(String name) {
        this.playAnimation(name, new LoopingState(System::nanoTime));
    }

    public Pose evaluatePose() {
        if (runner != null) {
            return runner.evaluate();
        }
        return DummyPose.INSTANCE;
    }

    public void setInited(boolean b) {
        this.inited = b;
    }

    public boolean isUsingItem() {
        Player player = Minecraft.getInstance().player;
        return inited && player != null && player.isUsingItem();
    }
}
