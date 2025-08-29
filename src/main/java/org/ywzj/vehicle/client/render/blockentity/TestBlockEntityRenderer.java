package org.ywzj.vehicle.client.render.blockentity;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.AdditiveBlender;
import com.maydaymemory.mae.blend.SimpleAdditiveBlender;
import com.maydaymemory.mae.control.misc.RealtimeVelocityEstimatorNode;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.blockentity.TestBlockEntity;
import org.ywzj.vehicle.client.animation.SelfTransferState;
import org.ywzj.vehicle.client.animation.TestAnimationContext;
import org.ywzj.vehicle.client.resource.BedrockModelLoader;

public class TestBlockEntityRenderer extends BedrockModelBlockEntityRenderer<TestBlockEntity> {
    private static final Material MATERIAL = new Material(InventoryMenu.BLOCK_ATLAS, YwzjVehicle.modLoc("block/test"));
    private static final AdditiveBlender BLENDER = new SimpleAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    public TestBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(BedrockModelLoader.TEST_MODEL, MATERIAL, RenderType::entityCutout);
    }

    @Override
    public void render(@NotNull TestBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.stateMachine == null) {
            blockEntity.velocityEstimatorNode = new RealtimeVelocityEstimatorNode(ArrayPoseBuilder::new, System::nanoTime);
            blockEntity.stateMachine = new AnimationStateMachine<>(SelfTransferState.INSTANCE, new TestAnimationContext(blockEntity.velocityEstimatorNode), System::nanoTime);
            blockEntity.velocityEstimatorNode.getPoseSlot().connect(blockEntity.stateMachine.getOutputPort());
        }
        blockEntity.tick();
        Pose bindPose = model.getBindPose();
        Pose animationPose = blockEntity.stateMachine.getPose();
        Pose blended = BLENDER.blend(bindPose, animationPose);
        model.applyPose(blended);
        super.render(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }
}
