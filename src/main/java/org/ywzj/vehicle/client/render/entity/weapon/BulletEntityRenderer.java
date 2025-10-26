package org.ywzj.vehicle.client.render.entity.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.InternalAssetLoader;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.entity.weapon.BulletEntity;

import java.util.Optional;

public class BulletEntityRenderer extends EntityRenderer<BulletEntity> {

    public BulletEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.DEFAULT_BULLET_MODEL);
    }

    @Override
    public void render(BulletEntity bullet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        renderTracerAmmo(bullet,partialTicks, poseStack, packedLight);
    }

    public void renderTracerAmmo(BulletEntity bullet, float partialTicks, PoseStack poseStack, int packedLight) {
        getModel().ifPresent(model -> {
            poseStack.pushPose();
            {
                float width = 0.025f;
                Vec3 bulletPosition = bullet.getPosition(partialTicks);
                double trailLength = 0.85 * bullet.getDeltaMovement().length();
                double disToEye = bulletPosition.distanceTo(bullet.getStartPos());
                trailLength = Math.min(trailLength, disToEye * 0.8);

                width *= (float) Math.max(1.0, disToEye / 3.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
                poseStack.translate(0, 0, trailLength / 2.0);
                poseStack.scale(width, width, (float) trailLength);
                // 距离两格外才渲染，只在前 5 tick 判定
                double bulletDistance = bulletPosition.distanceTo(bullet.getStartPos());
                if (bullet.tickCount >= 5 || bulletDistance > 2) {
                    RenderType type = RenderType.energySwirl(InternalAssetLoader.DEFAULT_BULLET_TEXTURE, 15, 15);
                    model.render(poseStack, ItemDisplayContext.NONE, type, packedLight, OverlayTexture.NO_OVERLAY,
                            1f, 1f, 1f, 1);
                }
            }
            poseStack.popPose();
        });
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
