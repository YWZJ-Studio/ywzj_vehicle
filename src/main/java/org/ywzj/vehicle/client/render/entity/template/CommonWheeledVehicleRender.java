package org.ywzj.vehicle.client.render.entity.template;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.entity.vehicle.CommonWheeledVehicle;
import org.ywzj.vehicle.resource.BedrockModelLoader;

// todo 测试用
public class CommonWheeledVehicleRender extends EntityRenderer<CommonWheeledVehicle> {

    public CommonWheeledVehicleRender(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(CommonWheeledVehicle pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        ResourceLocation displayId = pEntity.getCustomDisplayId();
        ResourceLocation modelLoc = new ResourceLocation(displayId.getNamespace(), "entity/" + displayId.getPath());
        ResourceLocation textureLoc = new ResourceLocation( modelLoc.getNamespace(), "textures/entity/" + displayId.getPath() + ".png");

        BedrockModel model = BedrockModelLoader.getModel(modelLoc);
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(textureLoc));

        if (model != null) {
            model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);
        }

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CommonWheeledVehicle pEntity) {
        return null;
    }

}
