package org.ywzj.vehicle.client.render.animation.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class RepairItemAnimationInstance {
    private Map<String, BedrockAnimation> animations;
    private ItemStack itemStack;
    private AnimationStateMachine<RepairItemContext> animationStateMachine;

    public RepairItemAnimationInstance(ItemStack stack, Map<String, BedrockAnimation> animations) {
        this.animations = animations;
        this.itemStack = stack;
        var ctx = new RepairItemContext(animations);
        this.animationStateMachine = new AnimationStateMachine<>(
                RepairItemAnimationStates.Main.INIT_STATE,
                ctx,
                System::nanoTime
        );

        RepairItemAnimationStates.Main.INIT_STATE.onEnter(ctx, RepairItemAnimationStates.Main.INIT_STATE);
    }

    public void tick(float renderTickTime) {
        animationStateMachine.tick();
    }

    public Pose getCurrentPose() {
        return animationStateMachine.getPose();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
