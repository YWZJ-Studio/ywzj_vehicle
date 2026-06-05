package org.ywzj.vehicle.client.render.entity.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;
import org.ywzj.vehicle.resource.BedrockModelLoader;

public class BulletEntityRenderer extends EntityRenderer<BulletEntity> {

    public static final ResourceLocation DEFAULT_BULLET_MODEL = YwzjVehicle.modLocation("entity/basic_bullet");
    public static final ResourceLocation DEFAULT_BULLET_TEXTURE = YwzjVehicle.modLocation("textures/entity/basic_bullet.png");

    public BulletEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(BulletEntity bullet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        var model = BedrockModelLoader.getModel(DEFAULT_BULLET_MODEL);
        poseStack.pushPose();
        {
            float width = Math.min(0.04f * bullet.getCaliber() / 7.62f, 0.2f);
            Vec3 bulletPosition = bullet.getPosition(partialTicks);
            double trailLength = 0.3 * bullet.getDeltaMovement().length();
            double disToEye = bulletPosition.distanceTo(bullet.getStartPos());
            trailLength = Math.min(trailLength, disToEye * 0.8);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
            poseStack.translate(0, 0, trailLength / 2.0);
            poseStack.scale(width, width, (float) trailLength);
            double bulletDistance = bulletPosition.distanceTo(bullet.getStartPos());
            if (bulletDistance > 1) {
                RenderType type = RenderType.energySwirl(DEFAULT_BULLET_TEXTURE, 15, 15);
                VertexConsumer builder = bufferSource.getBuffer(type);
                model.renderToBuffer(poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY, bullet.getTracerR(), bullet.getTracerG(), bullet.getTracerB(), 1);
            }
        }
        poseStack.popPose();
    }

    @Override
    protected int getBlockLightLevel(@NotNull BulletEntity entityBullet, @NotNull BlockPos blockPos) {
        return 15;
    }

    @Override
    public boolean shouldRender(BulletEntity bullet, Frustum camera, double pCamX, double pCamY, double pCamZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(@NotNull BulletEntity entity) {
        return null;
    }

}
