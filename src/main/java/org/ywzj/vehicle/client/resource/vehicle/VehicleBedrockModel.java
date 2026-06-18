package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.render.ModRenderTypes;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

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
        this.boneMap.forEach((name, bone) -> {
            if (name.startsWith("illum_")) {
                bone.illuminated = true;
            }
        });
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
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        setSpecialBoneVisible(false);
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public void renderToBuffer(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int packedLight) {
        setSpecialBoneVisible(false);
        super.renderToBuffer(poseStack, bufferSource,
                RenderType.entityCutout(texture),
                BedrockModelRenderTypes.polyMeshCutout(texture),
                packedLight,
                OverlayTexture.pack(0f, false)
        );
    }

    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public void renderSpecialBones(PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay) {
        renderSpecialBones(poseStack, source, packedLight, packedOverlay, false);
    }

    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public void renderSpecialBones(PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay, boolean isLocalPlayerVehicle) {
        if (specialBoneEntries.isEmpty()) {
            return;
        }
        setSpecialBoneVisible(true);
        for (var entry : specialBoneEntries) {
            VertexConsumer buffer;
            if (entry.bone.hasCubesInTree()) {
                switch (entry.effect.type) {
                    case MUZZLE_FLASH ->
                            buffer = source.getBuffer(ModRenderTypes.muzzleFlash(entry.effect.texture));
                    case TRANSPARENT ->
                            buffer = source.getBuffer(ModRenderTypes.cubeTransparent(entry.effect.texture));
                    case COCKPIT -> {
                        if (isLocalPlayerVehicle && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                            continue;
                        } else {
                            buffer = source.getBuffer(ModRenderTypes.cubeTransparent(entry.effect.texture));
                        }
                    }
                    default -> {
                        continue;
                    }
                }
                poseStack.pushPose();
                {
                    poseStack.last().pose().mul(getGlobalTransform(entry.bone));
                    entry.bone.render(poseStack, buffer, packedLight, packedOverlay);
                }
                poseStack.popPose();
            } else if (entry.bone.hasMeshesInTree()) {
                switch (entry.effect.type) {
                    case MUZZLE_FLASH ->
                            buffer = source.getBuffer(ModRenderTypes.muzzleFlash(entry.effect.texture));
                    case TRANSPARENT ->
                            buffer = source.getBuffer(ModRenderTypes.polyMeshTransparent(entry.effect.texture));
                    case COCKPIT -> {
                        if (isLocalPlayerVehicle && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                            continue;
                        } else {
                            buffer = source.getBuffer(ModRenderTypes.polyMeshTransparent(entry.effect.texture));
                        }
                    }
                    default -> {
                        continue;
                    }
                }
                poseStack.pushPose();
                {
                    poseStack.last().pose().mul(getGlobalTransform(entry.bone));
                    entry.bone.renderMeshes(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                poseStack.popPose();
            }
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
