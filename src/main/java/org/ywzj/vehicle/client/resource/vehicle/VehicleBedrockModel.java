package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakerOptions;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.client.render.ModRenderTypes;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 兼容 v1 运行时骨骼模型的载具模型，同时可选地附带 v2 烘焙定义。
 * 专用渲染器继续使用本类继承的 v1 API；只有通用载具渲染器会使用 baked 实例。
 */
public class VehicleBedrockModel extends BedrockModel {

    private static final Pattern ILLUMINATED_BONE_PATTERN = Pattern.compile("^illum_.*");
    private final BakedBedrockModel bakedModel;
    private final BakedModelInstance defaultModelInstance;
    private final List<SpecialBoneEntry> specialBoneEntries;
    private final List<BakedSpecialBoneEntry> bakedSpecialBoneEntries;

    public record SpecialBoneEntry(BedrockBone bone, SpecialBoneEffect effect) {}
    public record BakedSpecialBoneEntry(int boneIndex, SpecialBoneEffect effect) {}

    public VehicleBedrockModel(BedrockModelPOJO pojo, List<SpecialBoneEffect> specialBoneEffects) {
        this(pojo, specialBoneEffects, null);
    }

    public VehicleBedrockModel(BedrockModelPOJO pojo, List<SpecialBoneEffect> specialBoneEffects, BakerOptions bakerOptions) {
        super(pojo);
        Map<String, SpecialBoneEffect> specialBoneMap = new HashMap<>();
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
            if (ILLUMINATED_BONE_PATTERN.matcher(name).matches()) {
                bone.illuminated = true;
            }
        });

        if (bakerOptions != null) {
            Set<String> preservedBones = new HashSet<>(bakerOptions.preservedBones());
            preservedBones.addAll(specialBoneMap.keySet());
            Set<Pattern> preservedBonePatterns = new HashSet<>(bakerOptions.preservedBonePatterns());
            preservedBonePatterns.add(ILLUMINATED_BONE_PATTERN);
            BakerOptions options = new BakerOptions(
                    bakerOptions.animatedBones(),
                    preservedBones,
                    preservedBonePatterns,
                    bakerOptions.bakeStaticGeometry(),
                    bakerOptions.debugFoldedTree(),
                    true
            );
            this.bakedModel = BakedBedrockModel.bake(pojo, options);
            this.defaultModelInstance = this.bakedModel.createInstance();
            this.bakedSpecialBoneEntries = new ArrayList<>();
            for (SpecialBoneEffect effect : specialBoneMap.values()) {
                int boneIndex = bakedModel.getIndex(effect.bone);
                if (boneIndex >= 0) {
                    bakedSpecialBoneEntries.add(new BakedSpecialBoneEntry(boneIndex, effect));
                }
            }
        } else {
            this.bakedModel = null;
            this.defaultModelInstance = null;
            this.bakedSpecialBoneEntries = List.of();
        }
    }

    public boolean hasBakedModel() {
        return bakedModel != null;
    }

    public BakedBedrockModel getBakedModel() {
        return bakedModel;
    }

    public BakedModelInstance createBakedInstance() {
        if (bakedModel == null) {
            throw new IllegalStateException("This display does not enable a baked model");
        }
        BakedModelInstance instance = bakedModel.createInstance();
        instance.resetPose();
        for (BoneState bone : instance.getBoneIndexes()) {
            bone.illuminated = ILLUMINATED_BONE_PATTERN.matcher(bone.name()).matches();
        }
        return instance;
    }

//    @Override
//    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
//                               float red, float green, float blue, float alpha) {
//        setSpecialBoneVisible(false);
//        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
//    }
//
//    @OnlyIn(Dist.CLIENT)
//    @ParametersAreNonnullByDefault
//    public void renderToBuffer(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int packedLight) {
//        setSpecialBoneVisible(false);
//        super.renderToBuffer(poseStack, bufferSource,
//                RenderType.entityCutout(texture),
//                BedrockModelRenderTypes.polyMeshCutout(texture),
//                packedLight,
//                OverlayTexture.pack(0f, false)
//        );
//    }

    @OnlyIn(Dist.CLIENT)
    public void renderToBufferBaked(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int packedLight) {
        setSpecialBoneBakedVisible(defaultModelInstance, false);
        defaultModelInstance.renderToBuffer(poseStack, bufferSource,
                RenderType.entityCutout(texture),
                BedrockModelRenderTypes.polyMeshCutout(texture),
                packedLight,
                OverlayTexture.NO_OVERLAY);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderToBufferBaked(BakedModelInstance instance, PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int packedLight) {
        setSpecialBoneBakedVisible(instance, false);
        instance.renderToBuffer(poseStack, bufferSource,
                RenderType.entityCutout(texture),
                BedrockModelRenderTypes.polyMeshCutout(texture),
                packedLight,
                OverlayTexture.NO_OVERLAY);
    }

//    @OnlyIn(Dist.CLIENT)
//    @ParametersAreNonnullByDefault
//    public void renderSpecialBones(PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay) {
//        renderSpecialBones(poseStack, source, packedLight, packedOverlay, false);
//    }
//
//    @OnlyIn(Dist.CLIENT)
//    @ParametersAreNonnullByDefault
//    public void renderSpecialBones(PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay, boolean isLocalPlayerVehicle) {
//        for (SpecialBoneEntry entry : specialBoneEntries) {
//            VertexConsumer buffer;
//            if (entry.bone.hasCubesInTree()) {
//                switch (entry.effect.type) {
//                    case MUZZLE_FLASH -> buffer = source.getBuffer(ModRenderTypes.muzzleFlash(entry.effect.texture));
//                    case TRANSPARENT -> buffer = source.getBuffer(ModRenderTypes.cubeTransparent(entry.effect.texture));
//                    case COCKPIT -> {
//                        if (isLocalPlayerVehicle && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
//                            continue;
//                        }
//                        buffer = source.getBuffer(ModRenderTypes.cubeTransparent(entry.effect.texture));
//                    }
//                    default -> {
//                        continue;
//                    }
//                }
//                poseStack.pushPose();
//                poseStack.mulPoseMatrix(VectorUtil.getGlobalTransform(entry.bone));
//                entry.bone.render(poseStack, buffer, packedLight, packedOverlay);
//                poseStack.popPose();
//            } else if (entry.bone.hasMeshesInTree()) {
//                switch (entry.effect.type) {
//                    case MUZZLE_FLASH -> buffer = source.getBuffer(ModRenderTypes.muzzleFlash(entry.effect.texture));
//                    case TRANSPARENT -> buffer = source.getBuffer(ModRenderTypes.polyMeshTransparent(entry.effect.texture));
//                    case COCKPIT -> {
//                        if (isLocalPlayerVehicle && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
//                            continue;
//                        }
//                        buffer = source.getBuffer(ModRenderTypes.polyMeshTransparent(entry.effect.texture));
//                    }
//                    default -> {
//                        continue;
//                    }
//                }
//                poseStack.pushPose();
//                poseStack.mulPoseMatrix(VectorUtil.getGlobalTransform(entry.bone));
//                entry.bone.renderMeshes(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
//                poseStack.popPose();
//            }
//        }
//    }

    @OnlyIn(Dist.CLIENT)
    public void renderSpecialBonesBaked(PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay) {
        renderSpecialBonesBaked(defaultModelInstance, poseStack, source, packedLight, packedOverlay, false);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderSpecialBonesBaked(BakedModelInstance instance, PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay) {
        renderSpecialBonesBaked(instance, poseStack, source, packedLight, packedOverlay, false);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderSpecialBonesBaked(BakedModelInstance instance, PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay, boolean isLocalPlayerVehicle) {
        if (bakedModel == null) {
            return;
        }
        setSpecialBoneBakedVisible(instance, true);
        for (BakedSpecialBoneEntry entry : bakedSpecialBoneEntries) {
            if (entry.effect.type == SpecialBoneEffect.SpecialBoneEffectType.COCKPIT
                    && isLocalPlayerVehicle
                    && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                continue;
            }
            RenderType quadType;
            RenderType meshType;
            switch (entry.effect.type) {
                case MUZZLE_FLASH -> {
                    quadType = ModRenderTypes.muzzleFlash(entry.effect.texture);
                    meshType = ModRenderTypes.muzzleFlash(entry.effect.texture);
                }
                case TRANSPARENT, COCKPIT -> {
                    quadType = ModRenderTypes.cubeTransparent(entry.effect.texture);
                    meshType = ModRenderTypes.polyMeshTransparent(entry.effect.texture);
                }
                default -> {
                    continue;
                }
            }
            BoneState bone = instance.getBone(entry.boneIndex);
            if (bone == null) {
                continue;
            }
            bakedModel.renderBone(instance, entry.boneIndex, poseStack, source.getBuffer(quadType), packedLight,
                    packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F, true);
            bakedModel.renderBone(instance, entry.boneIndex, poseStack, source.getBuffer(meshType), packedLight,
                    packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F, false);
        }
    }

    private void setSpecialBoneVisible(boolean visible) {
        for (SpecialBoneEntry entry : specialBoneEntries) {
            entry.bone.visible = visible;
        }
    }

    private void setSpecialBoneBakedVisible(BakedModelInstance instance, boolean visible) {
        for (BakedSpecialBoneEntry entry : bakedSpecialBoneEntries) {
            BoneState bone = instance.getBone(entry.boneIndex);
            if (bone != null) {
                bone.visible = visible;
            }
        }
    }

}
