package org.ywzj.vehicle.client.render.entity.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.weapon.DecoyFlareEntity;

public class DecoyFlareEntityRenderer extends EntityRenderer<DecoyFlareEntity> {

    public DecoyFlareEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(DecoyFlareEntity pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return true;
    }

    @Override
    public void render(
            DecoyFlareEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.1, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float age = entity.tickCount + partialTick;
        float scale = 0.9f + (float) Math.sin(age * 0.5f) * 0.1f;
        poseStack.scale(scale, scale, scale);
        VertexConsumer buffer = bufferSource.getBuffer(
                RenderType.entityTranslucent(getTextureLocation(entity))
        );
        PoseStack.Pose pose = poseStack.last();
        int fullBright = LightTexture.FULL_BRIGHT;
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        buffer.vertex(pose.pose(), -0.5f, -0.5f, 0.0f)
                .color(r, g, b, a)
                .uv(0.0f, 1.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(fullBright)
                .normal(pose.normal(), 0, 0, 1)
                .endVertex();
        buffer.vertex(pose.pose(), 0.5f, -0.5f, 0.0f)
                .color(r, g, b, a)
                .uv(1.0f, 1.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(fullBright)
                .normal(pose.normal(), 0, 0, 1)
                .endVertex();
        buffer.vertex(pose.pose(), 0.5f, 0.5f, 0.0f)
                .color(r, g, b, a)
                .uv(1.0f, 0.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(fullBright)
                .normal(pose.normal(), 0, 0, 1)
                .endVertex();
        buffer.vertex(pose.pose(), -0.5f, 0.5f, 0.0f)
                .color(r, g, b, a)
                .uv(0.0f, 0.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(fullBright)
                .normal(pose.normal(), 0, 0, 1)
                .endVertex();
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DecoyFlareEntity pEntity) {
        return YwzjVehicle.modLocation("textures/entity/decoy_flare.png");
    }

}
