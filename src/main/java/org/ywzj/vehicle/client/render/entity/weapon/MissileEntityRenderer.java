package org.ywzj.vehicle.client.render.entity.weapon;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
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
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.entity.weapon.MissileEntity;

public class MissileEntityRenderer extends EntityRenderer<MissileEntity> {

    public MissileEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(MissileEntity pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();
        {
            Vec3 root = new Vec3(0, 0, 0);
            pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);

            BedrockModel model = BedrockModelLoader.getModel(YwzjVehicle.modLoc("bedrock/entity/missile_akd10"));
            VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(YwzjVehicle.modLoc("textures/entity/missile_akd10.png")));

            model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);
        }
        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity pEntity) {
        return null;
    }

}
