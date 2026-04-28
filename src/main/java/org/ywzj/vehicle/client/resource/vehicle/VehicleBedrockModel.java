package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.render.ModRenderTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleBedrockModel extends BedrockModel {

    public record SpecialBoneEntry(BedrockBone bone, SpecialBoneEffect effect) {}
    private final Map<String, SpecialBoneEffect> specialBoneMap;
    private final List<SpecialBoneEntry> specialBoneEntries;

    /**
     * 构造函数
     * @param pojo 模型POJO数据
     * @param specialBoneEffects 特殊骨骼效果列表
     */
    public VehicleBedrockModel(BedrockModelPOJO pojo, List<SpecialBoneEffect> specialBoneEffects) {
        super(pojo);
        this.specialBoneMap = new HashMap<>();
        this.specialBoneEntries = new ArrayList<>();
        if (specialBoneEffects != null) {
            for (SpecialBoneEffect effect : specialBoneEffects) {
                if (effect.isValid()) {
                    specialBoneMap.put(effect.bone, effect);
                    BedrockBone bone = getBone(effect.bone);
                    if (bone != null) {
                        specialBoneEntries.add(new SpecialBoneEntry(bone, effect));
                    }
                }
            }
        }
    }

    public void setSpecialBoneVisible(boolean visible) {
        for (SpecialBoneEntry entry : specialBoneEntries) {
            entry.bone.visible = visible;
        }
    }

    public Map<String, SpecialBoneEffect> getSpecialBoneMap() {
        return specialBoneMap;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        setSpecialBoneVisible(false);
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        setSpecialBoneVisible(false);
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public void renderSpecialBones(PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay) {
        if (specialBoneEntries.isEmpty()) {
            return;
        }
        setSpecialBoneVisible(true);
        for (var entry : specialBoneEntries) {
            VertexConsumer buffer = source.getBuffer(ModRenderTypes.muzzleFlash(entry.effect.texture));
            poseStack.pushPose();
            poseStack.last().pose().mul(getGlobalTransform(entry.bone));
            entry.bone.render(poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    // 取得骨骼除了自身变换以外的全局变换矩阵
    public static Matrix4f getGlobalTransform(@NotNull BedrockBone targetBone) {
        Matrix4f matrix = new Matrix4f();

        for (BedrockBone bone = targetBone.parent; bone != null; bone = bone.parent) {
            matrix.scaleLocal(bone.xScale, bone.yScale, bone.zScale);
            matrix.rotateLocal(bone.rotation);
            matrix.translateLocal(bone.x / 16.0F, bone.y / 16.0F, bone.z / 16.0F);
        }

        return matrix;
    }
}
