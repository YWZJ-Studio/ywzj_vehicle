package org.ywzj.vehicle.client.render.animation.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.HandedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.Map;

public class RepairItemAnimationInstance implements IFPAnimationInstance {
    private static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    private RepairItemContext ctx;
    private Map<String, BedrockAnimation> animations;
    private HandedBedrockModel model;
    private ItemStack itemStack;
    private AnimationStateMachine<RepairItemContext> animationStateMachine;
    private boolean drawn = false;

    private Pose cachePose = DummyPose.INSTANCE;
    private Quaternionf rotation = new Quaternionf();

    public RepairItemAnimationInstance(ItemStack stack, Map<String, BedrockAnimation> animations, @NotNull HandedBedrockModel repairToolModel) {
        this.animations = animations;
        this.model = repairToolModel;
        this.itemStack = stack;
        this.ctx = new RepairItemContext(animations);
        this.animationStateMachine = new AnimationStateMachine<>(
                RepairItemAnimationStates.Main.INIT_STATE,
                ctx,
                System::nanoTime
        );
    }

    @Override
    public ItemStack currentItem() {
        return itemStack;
    }

    @Override
    public Pose getPose() {
        return animationStateMachine.getPose();
    }

    public void tick(float renderTickTime) {
        animationStateMachine.tick();

        cachePose = BLENDER.blend(model.getBindPose(), this.getPose());
    }

    @NotNull
    @Override
    public Quaternionf getCameraRotation() {
        return rotation;
    }

    @Override
    public void setCameraRotation(@NotNull Quaternionf quaternionf) {
        this.rotation = quaternionf;
    }

    @Override
    public Pose getCachedPose() {
        return cachePose;
    }

    @Override
    public void updateItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void triggerDraw() {
        if (drawn) {
            return;
        }
        drawn = true;
        RepairItemAnimationStates.Main.INIT_STATE.onEnter(ctx, RepairItemAnimationStates.Main.INIT_STATE);
    }

    @Override
    public void triggerPutAway() {
        ctx.setEnded(true);
    }
}
