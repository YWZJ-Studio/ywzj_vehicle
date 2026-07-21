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
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.render.ModRenderTypes;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 兼容 v1 运行时骨骼模型的载具模型，同时可选地附带 v2 烘焙定义。
 * 专用渲染器继续使用本类继承的 v1 API；只有通用载具渲染器会使用 baked 实例。
 */
public class VehicleBedrockModel extends BedrockModel {

    private static final Pattern ILLUMINATED_BONE_PATTERN = Pattern.compile("^illum_.*");

    public record SpecialBoneEntry(BedrockBone bone, SpecialBoneEffect effect) {}
    public record BakedSpecialBoneEntry(int boneIndex, SpecialBoneEffect effect) {}

    private final Map<String, SpecialBoneEffect> specialBoneMap;
    private final List<SpecialBoneEntry> specialBoneEntries;
    private final List<BakedSpecialBoneEntry> bakedSpecialBoneEntries;
    @Nullable
    private final BakedBedrockModel bakedModel;

    public VehicleBedrockModel(BedrockModelPOJO pojo, List<SpecialBoneEffect> specialBoneEffects) {
        this(pojo, specialBoneEffects, null);
    }

    public VehicleBedrockModel(BedrockModelPOJO pojo, List<SpecialBoneEffect> specialBoneEffects,
                               @Nullable BakerOptions bakerOptions) {
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
            this.bakedSpecialBoneEntries = new ArrayList<>();
            for (SpecialBoneEffect effect : specialBoneMap.values()) {
                int boneIndex = bakedModel.getIndex(effect.bone);
                if (boneIndex >= 0) {
                    bakedSpecialBoneEntries.add(new BakedSpecialBoneEntry(boneIndex, effect));
                }
            }
        } else {
            this.bakedModel = null;
            this.bakedSpecialBoneEntries = List.of();
        }
    }

    public boolean hasBakedModel() {
        return bakedModel != null;
    }

    @Nullable
    public BakedBedrockModel getBakedModel() {
        return bakedModel;
    }

    public BakedModelInstance createBakedInstance() {
        if (bakedModel == null) {
            throw new IllegalStateException("This display does not enable a baked model");
        }
        BakedModelInstance instance = bakedModel.createInstance();
        initializeBakedInstance(instance);
        return instance;
    }

    /** 初始化新建实例的 bind pose 与固定骨骼状态。 */
    private void initializeBakedInstance(BakedModelInstance instance) {
        instance.resetPose();
        for (BoneState bone : instance.getBoneIndexes()) {
            bone.illuminated = bone.name().startsWith("illum_");
        }
    }

    public Map<String, SpecialBoneEffect> getSpecialBoneMap() {
        return specialBoneMap;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
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
        for (SpecialBoneEntry entry : specialBoneEntries) {
            VertexConsumer buffer;
            if (entry.bone.hasCubesInTree()) {
                switch (entry.effect.type) {
                    case MUZZLE_FLASH -> buffer = source.getBuffer(ModRenderTypes.muzzleFlash(entry.effect.texture));
                    case TRANSPARENT -> buffer = source.getBuffer(ModRenderTypes.cubeTransparent(entry.effect.texture));
                    case COCKPIT -> {
                        if (isLocalPlayerVehicle && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                            continue;
                        }
                        buffer = source.getBuffer(ModRenderTypes.cubeTransparent(entry.effect.texture));
                    }
                    default -> {
                        continue;
                    }
                }
                poseStack.pushPose();
                poseStack.mulPoseMatrix(getGlobalTransform(entry.bone));
                entry.bone.render(poseStack, buffer, packedLight, packedOverlay);
                poseStack.popPose();
            } else if (entry.bone.hasMeshesInTree()) {
                switch (entry.effect.type) {
                    case MUZZLE_FLASH -> buffer = source.getBuffer(ModRenderTypes.muzzleFlash(entry.effect.texture));
                    case TRANSPARENT -> buffer = source.getBuffer(ModRenderTypes.polyMeshTransparent(entry.effect.texture));
                    case COCKPIT -> {
                        if (isLocalPlayerVehicle && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                            continue;
                        }
                        buffer = source.getBuffer(ModRenderTypes.polyMeshTransparent(entry.effect.texture));
                    }
                    default -> {
                        continue;
                    }
                }
                poseStack.pushPose();
                poseStack.mulPoseMatrix(getGlobalTransform(entry.bone));
                entry.bone.renderMeshes(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                poseStack.popPose();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void renderBakedToBuffer(BakedModelInstance instance, PoseStack poseStack, MultiBufferSource bufferSource,
                                    ResourceLocation texture, int packedLight) {
        setBakedSpecialBoneVisible(instance, false);
        instance.renderToBuffer(poseStack, bufferSource,
                RenderType.entityCutout(texture),
                BedrockModelRenderTypes.polyMeshCutout(texture),
                packedLight,
                OverlayTexture.NO_OVERLAY);
    }

    @OnlyIn(Dist.CLIENT)
    public void renderBakedSpecialBones(BakedModelInstance instance, PoseStack poseStack, MultiBufferSource source,
                                        int packedLight, int packedOverlay, boolean isLocalPlayerVehicle) {
        if (bakedModel == null) {
            return;
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
            bone.visible = true;
            bakedModel.renderBone(instance, entry.boneIndex, poseStack, source.getBuffer(quadType), packedLight,
                    packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F, true);
            bakedModel.renderBone(instance, entry.boneIndex, poseStack, source.getBuffer(meshType), packedLight,
                    packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F, false);
            bone.visible = false;
        }
    }

    /** 取得 v1 骨骼除自身变换以外的全局变换矩阵。 */
    public static Matrix4f getGlobalTransform(@NotNull BedrockBone targetBone) {
        Matrix4f matrix = new Matrix4f();
        for (BedrockBone bone = targetBone.parent; bone != null; bone = bone.parent) {
            matrix.scaleLocal(bone.xScale, bone.yScale, bone.zScale);
            matrix.rotateLocal(bone.rotation);
            matrix.translateLocal(bone.x / 16.0F, bone.y / 16.0F, bone.z / 16.0F);
        }
        return matrix;
    }

    private void setSpecialBoneVisible(boolean visible) {
        for (SpecialBoneEntry entry : specialBoneEntries) {
            entry.bone.visible = visible;
        }
    }

    private void setBakedSpecialBoneVisible(BakedModelInstance instance, boolean visible) {
        for (BakedSpecialBoneEntry entry : bakedSpecialBoneEntries) {
            BoneState bone = instance.getBone(entry.boneIndex);
            if (bone != null) {
                bone.visible = visible;
            }
        }
    }
}
