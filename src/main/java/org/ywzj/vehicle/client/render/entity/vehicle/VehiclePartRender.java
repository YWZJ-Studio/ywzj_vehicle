package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.entity.misc.VehiclePart;
import org.ywzj.vehicle.vehicle.part.PartUnit;

import java.util.ArrayList;
import java.util.List;

public class VehiclePartRender extends EntityRenderer<VehiclePart> {

    public VehiclePartRender(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(VehiclePart part, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        VehicleRender.renderHitbox(part, pPoseStack, bufferSource);
        BaseDisplay display = ClientAssetsManager.INSTANCE.getVehicleDisplay(part.getDisplayId()).orElse(null);
        if (display == null || display.getModel() == null || display.getTexture() == null) {
            return;
        }
        VehicleBedrockModel model = display.getModel();
        if (!model.hasBakedModel()) {
            return;
        }
        BakedModelInstance modelInstance = part.getModelInstance();
        if (modelInstance == null) {
            return;
        }
        PartUnit<?> partUnit = part.getPartUnit();
        if (partUnit == null) {
            return;
        }
        String boneName = partUnit.getRenderBoneName();
        int boneIndex = modelInstance.getIndex(boneName);
        if (boneIndex < 0) {
            return;
        }
        int modelLight = pPackedLight;
        if (part.isDestroyed()) {
            int blockLight = (int) (LightTexture.block(pPackedLight) / 1.5f);
            int skyLight = (int) (LightTexture.sky(pPackedLight) / 1.5f);
            modelLight = LightTexture.pack(blockLight, skyLight);
        }
        List<BoneState> hiddenBones = new ArrayList<>();
        for (String excludedBoneName : part.getExcludedBoneNames()) {
            BoneState bone = modelInstance.getBone(excludedBoneName);
            if (bone != null && bone.visible) {
                bone.visible = false;
                hiddenBones.add(bone);
            }
        }
        pPoseStack.pushPose();
        try {
            super.render(part, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            VehicleRender.applyVehicleRotation(part, pPartialTick, pPoseStack);
            Vec3 bottom = part.getBottomOffset();
            pPoseStack.translate(-bottom.x, -bottom.y, -bottom.z);
            modelInstance.renderSingleBone(pPoseStack, boneIndex, bufferSource,
                    RenderType.entityCutout(display.getTexture()),
                    BedrockModelRenderTypes.polyMeshCutout(display.getTexture()),
                    modelLight, OverlayTexture.NO_OVERLAY,
                     1.0F, 1.0F, 1.0F, 1.0F, false);
        } finally {
            hiddenBones.forEach(bone -> bone.visible = true);
            pPoseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(@NotNull VehiclePart pEntity) {
        return null;
    }

}
