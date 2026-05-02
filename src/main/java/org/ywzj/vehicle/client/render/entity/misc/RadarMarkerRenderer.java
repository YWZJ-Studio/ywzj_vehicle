package org.ywzj.vehicle.client.render.entity.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.entity.misc.RadarMarkerEntity;

public class RadarMarkerRenderer extends EntityRenderer<RadarMarkerEntity> {

    public RadarMarkerRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public boolean shouldRender(RadarMarkerEntity pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return false;
    }

    @Override
    public void render(RadarMarkerEntity rope, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {}

    @Override
    public ResourceLocation getTextureLocation(RadarMarkerEntity pEntity) {
        return null;
    }

}
