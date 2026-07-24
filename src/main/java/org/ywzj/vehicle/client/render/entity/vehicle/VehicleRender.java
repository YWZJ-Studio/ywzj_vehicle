package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.AnimationRateLimiter;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.maydaymemory.mae.basic.Pose;
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
import org.ywzj.vehicle.client.particle.BulletHoleParticle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VehicleRender<T extends AbstractVehicle> extends EntityRenderer<T> {

    public VehicleRender(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(T vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        BaseDisplay display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).orElse(null);
        if (display == null || display.getModel() == null || display.getTexture() == null) {
            return;
        }
        VehicleBedrockModel model = display.getModel();
        if (!model.hasBakedModel()) {
            return;
        }
        BakedModelInstance modelInstance = vehicle.getModelInstance();
        if (modelInstance == null) {
            modelInstance = display.getModel().getDefaultModelInstance();
        }
        int modelLight = vehicle.isDestroyed() ? 64 : pPackedLight;
        pPoseStack.pushPose();
        try {
            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            applyVehicleRotation(vehicle, pPartialTick, pPoseStack);
            applyAnimationPose(vehicle, pPartialTick, modelInstance);
            // 载具
            model.renderToBuffer(modelInstance, pPoseStack, bufferSource, display.getTexture(), modelLight);
            model.renderSpecialBones(modelInstance, pPoseStack, bufferSource, modelLight, OverlayTexture.NO_OVERLAY, vehicle == LocalVehiclePlayer.instance.getVehicle());
            // 部件
            for (PartUnit<?> partUnit : vehicle.getPartUnits()) {
                partUnit.render(pPoseStack, bufferSource, pPackedLight);
            }
            // 饰品
            for (DecorationUnit decorationUnit : vehicle.getDecorationUnits().values()) {
                decorationUnit.render(pPoseStack, bufferSource, pPackedLight);
            }
            // 弹孔
            for (BulletHoleParticle bulletHoleParticle : vehicle.getBulletHoleParticles()) {
                bulletHoleParticle.renderOnVehicle(pPartialTick, pPoseStack, bufferSource, modelInstance);
            }
            vehicle.lastRenderTime = System.currentTimeMillis();
        } finally {
            pPoseStack.popPose();
        }
    }

    public static void applyVehicleRotation(AbstractVehicle vehicle, float partialTick, PoseStack poseStack) {
        Vec3 root = vehicle.centerOffset;
        Quaternionf rot = new Quaternionf()
                .rotateY(Math.toRadians(-vehicle.yRotO))
                .rotateX(Math.toRadians(vehicle.xRotO))
                .rotateZ(Math.toRadians(vehicle.zRotO))
                .slerp(vehicle.rotYXZ(), partialTick);
        poseStack.rotateAround(rot, (float) root.x, (float) root.y, (float) root.z);
    }

    public static void applyAnimationPose(AbstractVehicle vehicle, float partialTick, BakedModelInstance modelInstance) {
        if (vehicle instanceof IAnimationEntity<?, ?> animationEntity) {
            var animationInstance = animationEntity.getAnimationInstance();
            if (animationInstance != null) {
                animationInstance.getContext().setPartialTick(partialTick);
                animationInstance.tick();
                AnimationRateLimiter<Pose> animationRateLimiter = vehicle.getAnimationRateLimiter();
                Pose pose = animationRateLimiter.update(() -> BLENDER.blend(modelInstance.getBindPose(), animationInstance.getCurrentPose()));
                if (pose != vehicle.lastPose) {
                    modelInstance.applyPose(pose);
                    vehicle.lastPose = pose;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFire(VehicleFireEvent.Post event) {
        if (!event.isClientSide()) {
            return;
        }
        if (event.getVehicle() instanceof IAnimationEntity<?, ?> animationEntity) {
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
