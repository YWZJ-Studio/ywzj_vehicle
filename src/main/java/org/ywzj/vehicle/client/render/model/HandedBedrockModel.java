package org.ywzj.vehicle.client.render.model;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.ywzj.vehicle.util.MathUtil;
import org.ywzj.vehicle.util.RenderHelper;

import javax.annotation.ParametersAreNonnullByDefault;

public class HandedBedrockModel extends BedrockModelBase {
    private boolean renderHand = true;
    private final BedrockBone leftHandBone;
    private final BedrockBone rightHandBone;

    public HandedBedrockModel(BedrockModelPOJO pojo, @Nullable TransformScale scales) {
        super(pojo, scales);
        leftHandBone = getBone("lefthand_pos");
        rightHandBone = getBone("righthand_pos");
        if (leftHandBone != null) {
            leftHandBone.visible = false;
        }
        if (rightHandBone != null) {
            rightHandBone.visible = false;
        }
    }

    @ParametersAreNonnullByDefault
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        // 渲染枪械
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay);
        // 渲染双臂
        if (renderHand) {
            if (leftHandBone != null) {
                Matrix4f transform = leftHandBone.getGlobalTransform();
                poseStack.pushPose();
                MathUtil.mulMatrix(poseStack, transform);
                RenderHelper.renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.LEFT, poseStack, packedLight);
                poseStack.popPose();
            }
            if (rightHandBone != null) {
                Matrix4f transform = rightHandBone.getGlobalTransform();
                poseStack.pushPose();
                MathUtil.mulMatrix(poseStack, transform);
                RenderHelper.renderFirstPersonArm(Minecraft.getInstance().player, HumanoidArm.RIGHT, poseStack, packedLight);
                poseStack.popPose();
            }
        }
    }

    public boolean isRenderHand() {
        return renderHand;
    }

    public void setRenderHand(boolean renderHand) {
        this.renderHand = renderHand;
    }
}
