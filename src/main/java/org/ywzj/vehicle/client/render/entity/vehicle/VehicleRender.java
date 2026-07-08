package org.ywzj.vehicle.client.render.entity.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Quaternionf;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VehicleRender<T extends AbstractVehicle> extends EntityRenderer<T> {

    public VehicleRender(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(T vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        ResourceLocation displayId = vehicle.getDisplayId();
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(displayId).orElse(null);
        if (display == null) {
            return;
        }
        VehicleBedrockModel model = display.getModel();
        if (model == null) {
            return;
        }
        pPoseStack.pushPose();
        {
            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            // 载具自身旋转
            Vec3 root = vehicle.centerOffset;
            Quaternionf rot = new Quaternionf()
                    .rotateY(Math.toRadians(-vehicle.yRotO))
                    .rotateX(Math.toRadians(vehicle.xRotO))
                    .rotateZ(Math.toRadians(vehicle.zRotO)).slerp(vehicle.rotYXZ(), pPartialTick);
            pPoseStack.rotateAround(rot, (float) root.x, (float) root.y, (float) root.z);
            // 载具动画
            if (vehicle instanceof IAnimationEntity<?,?> animationEntity) {
                var instance = animationEntity.getAnimationInstance();
                if (instance != null) {
                    instance.getContext().setPartialTick(pPartialTick);
                    instance.tick();
                    model.applyPose(BLENDER.blend(model.getBindPose(), instance.getCurrentPose()));
                }
            }
            // 渲染载具
            if (model instanceof VehicleBedrockModel) {
                model.renderToBuffer(pPoseStack, bufferSource, display.getTexture(), vehicle.isDestroyed() ? 64 : pPackedLight);
                model.renderSpecialBones(pPoseStack, bufferSource, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY, vehicle == LocalVehiclePlayer.instance.getVehicle());
            }
            // 渲染部件
            vehicle.getPartUnits().forEach(partUnit -> partUnit.render(pPoseStack, bufferSource, pPackedLight));
            vehicle.getDecorationUnits().values().forEach(decorationUnit -> decorationUnit.render(pPoseStack, bufferSource, pPackedLight));
            // 渲染弹孔
            vehicle.getBulletHoleParticles().forEach(bulletHoleParticle -> bulletHoleParticle.renderOnVehicle(pPartialTick, pPoseStack, bufferSource));
            // 复原模型
            model.applyPose(model.getBindPose());
            vehicle.lastRenderTime = System.currentTimeMillis();
        }
        pPoseStack.popPose();
    }

    @SubscribeEvent
    public static void onFire(VehicleFireEvent.Post event) {
        if (!event.isClientSide()) {
            return;
        }
        if (event.getVehicle() instanceof IAnimationEntity<?,?> animationEntity) {
            var instance = animationEntity.getAnimationInstance();
            if (instance != null) {
                String partId = event.getWeapon().getWeaponUnit().getId();
                instance.getContext().offerEvent(partId + "_fire");
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(@NotNull T pEntity) {
        return null;
    }

}
