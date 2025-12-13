package org.ywzj.vehicle.client.render.model;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.FaceItem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.FaceUVsItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;


public class SlotModel extends EntityModel<Entity> {
    private static final FaceItem EMPTY = new FaceItem(new float[]{0f, 0f}, new float[]{0f, 0f});
    private static final FaceItem X16 = new FaceItem(new float[]{0f, 0f}, new float[]{16f, 16f});
    private static final FaceUVsItem SINGLE_SOUTH_X16 = new FaceUVsItem(EMPTY, EMPTY, EMPTY, X16, EMPTY, EMPTY);
    private final BedrockBone bone;

    public SlotModel(boolean illuminated) {
        bone = new BedrockBone();
        bone.x = 8.0F;
        bone.y = 8.0F;
        bone.z = 8.0F;
        bone.cubes.add(new BedrockCubePerFace(-16.0F, -16.0F, 0F, 16.0F, 16.0F, 0, 0, 16, 16, SINGLE_SOUTH_X16));
        bone.illuminated = illuminated;
    }

    public SlotModel() {
        this(false);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bone.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
