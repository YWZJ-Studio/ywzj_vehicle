package org.ywzj.vehicle.client.resource.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBoneDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakerOptions;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
    private final List<BakedSpecialBoneEntry> bakedSpecialBoneEntries;
    private final boolean hasMeshGeometry;

    public record BakedSpecialBoneEntry(int boneIndex, SpecialBoneEffect effect, boolean hasQuads, boolean hasVertices) {}

    public VehicleBedrockModel(BedrockModelPOJO pojo, List<SpecialBoneEffect> specialBoneEffects) {
        this(pojo, specialBoneEffects, null);
    }

    public VehicleBedrockModel(BedrockModelPOJO pojo, List<SpecialBoneEffect> specialBoneEffects, BakerOptions bakerOptions) {
        super(pojo);
        Map<String, SpecialBoneEffect> specialBoneMap = new HashMap<>();
        if (specialBoneEffects != null) {
            for (SpecialBoneEffect effect : specialBoneEffects) {
                if (effect.isValid()) {
                    specialBoneMap.put(effect.bone, effect);
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
            BakedBoneDefinition[] bakedBones = this.bakedModel.bones();
            for (SpecialBoneEffect effect : specialBoneMap.values()) {
                int boneIndex = bakedModel.getIndex(effect.bone);
                if (boneIndex >= 0) {
                    BakedBoneDefinition definition = bakedBones[boneIndex];
                    bakedSpecialBoneEntries.add(new BakedSpecialBoneEntry(boneIndex, effect,
                            definition.hasQuadsInTree(), definition.hasVerticesInTree()));
                }
            }
            this.hasMeshGeometry = this.bakedModel.meshChunks().length > 0;
        } else {
            this.bakedModel = null;
            this.defaultModelInstance = null;
            this.bakedSpecialBoneEntries = List.of();
            this.hasMeshGeometry = false;
        }
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

    @OnlyIn(Dist.CLIENT)
    public void renderToBuffer(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int packedLight) {
        renderToBuffer(defaultModelInstance, poseStack, bufferSource, texture, packedLight);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderToBuffer(BakedModelInstance instance, PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, int packedLight) {
        setSpecialBoneVisible(instance, false);
        RenderType quadType = RenderType.entityCutout(texture);
        if (hasMeshGeometry) {
            instance.renderToBuffer(poseStack, bufferSource,
                    quadType,
                    BedrockModelRenderTypes.polyMeshCutout(texture),
                    packedLight,
                    OverlayTexture.NO_OVERLAY);
            return;
        }
        bakedModel.renderBoneTree(instance, poseStack, bufferSource.getBuffer(quadType),
                packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F, true);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderSpecialBones(PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay) {
        renderSpecialBones(defaultModelInstance, poseStack, source, packedLight, packedOverlay, null, false);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderSpecialBones(BakedModelInstance instance, PoseStack poseStack, MultiBufferSource source, int packedLight, int packedOverlay, List<BoneState> invisibleBones, boolean isLocalPlayerVehicle) {
        setSpecialBoneVisible(instance, true);
        if (invisibleBones != null) {
            invisibleBones.forEach(invisibleBone -> invisibleBone.visible = false);
        }
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
                    meshType = ModRenderTypes.muzzleFlashMesh(entry.effect.texture);
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
            if (entry.hasQuads) {
                instance.renderSingleBonePass(poseStack, entry.boneIndex, source.getBuffer(quadType), packedLight,
                        packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F, true, false);
            }
            if (entry.hasVertices) {
                instance.renderSingleBonePass(poseStack, entry.boneIndex, source.getBuffer(meshType), packedLight,
                        packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F, false, false);
            }
        }
    }

    private void setSpecialBoneVisible(BakedModelInstance instance, boolean visible) {
        for (BakedSpecialBoneEntry entry : bakedSpecialBoneEntries) {
            BoneState bone = instance.getBone(entry.boneIndex);
            if (bone != null) {
                bone.visible = visible;
            }
        }
    }

    public boolean hasBakedModel() {
        return bakedModel != null;
    }

    public BakedBedrockModel getBakedModel() {
        return bakedModel;
    }

    public BakedModelInstance getDefaultModelInstance() {
        return defaultModelInstance;
    }

}
