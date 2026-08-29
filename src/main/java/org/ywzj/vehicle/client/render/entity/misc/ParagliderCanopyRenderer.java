package org.ywzj.vehicle.client.render.entity.misc;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.resource.BedrockModelResources;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.misc.ParagliderCanopy;
import org.ywzj.vehicle.resource.ParachuteModels;

public class ParagliderCanopyRenderer extends EntityRenderer<ParagliderCanopy> {

    private BakedModelInstance modelInstance;

    public ParagliderCanopyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ParagliderCanopy entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (modelInstance == null) {
            BakedBedrockModel model = BedrockModelResources.getInstance().getBakedModel(ParachuteModels.PARAGLIDER_CANOPY);
            if (model == null) {
                return;
            }
            modelInstance = model.createInstance();
        }
        poseStack.pushPose();
        {
            float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            Entity owner = entity.getOwner();
            if (!entity.isFalling() && owner instanceof LivingEntity livingEntity) {
                Vec3 ownerPosition = livingEntity.getPosition(partialTick);
                Vec3 canopyPosition = entity.getPosition(partialTick);
                poseStack.translate(
                        ownerPosition.x - canopyPosition.x,
                        ownerPosition.y - canopyPosition.y - 0.1f,
                        ownerPosition.z - canopyPosition.z
                );
                yaw = Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            modelInstance.renderToBuffer(
                    poseStack,
                    bufferSource,
                    RenderType.entityCutout(ParachuteModels.PARAGLIDER_CANOPY_TEXTURE),
                    BedrockModelRenderTypes.polyMeshCutout(ParachuteModels.PARAGLIDER_CANOPY_TEXTURE),
                    packedLight,
                    OverlayTexture.NO_OVERLAY
            );
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ParagliderCanopy entity) {
        return ParachuteModels.PARAGLIDER_CANOPY_TEXTURE;
    }

}
