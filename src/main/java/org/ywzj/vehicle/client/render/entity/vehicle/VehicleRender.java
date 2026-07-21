package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
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
import org.joml.Vector3f;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
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
        BaseDisplay display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).orElse(null);
        if (display == null || display.getModel() == null || display.getTexture() == null) {
            return;
        }
        VehicleBedrockModel model = display.getModel();
        pPoseStack.pushPose();
        try {
            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            applyVehicleRotation(vehicle, pPartialTick, pPoseStack);
            int modelLight = vehicle.isDestroyed() ? 64 : pPackedLight;

            BakedModelInstance bakedInstance = null;
            if (model.hasBakedModel()) {
                bakedInstance = prepareModelInstance(vehicle, display, pPartialTick);
                model.renderBakedToBuffer(bakedInstance, pPoseStack, bufferSource, display.getTexture(), modelLight);
                model.renderBakedSpecialBones(bakedInstance, pPoseStack, bufferSource, modelLight, OverlayTexture.NO_OVERLAY,
                        vehicle == LocalVehiclePlayer.instance.getVehicle());
            } else {
                if (vehicle instanceof IAnimationEntity<?, ?> animationEntity) {
                    var instance = animationEntity.getAnimationInstance();
                    if (instance != null) {
                        instance.getContext().setPartialTick(pPartialTick);
                        instance.tick();
                        model.applyPose(BLENDER.blend(model.getBindPose(), instance.getCurrentPose()));
                    }
                }
                model.renderToBuffer(pPoseStack, bufferSource, display.getTexture(), modelLight);
                model.renderSpecialBones(pPoseStack, bufferSource, modelLight, OverlayTexture.NO_OVERLAY,
                        vehicle == LocalVehiclePlayer.instance.getVehicle());
            }

            vehicle.getPartUnits().forEach(partUnit -> partUnit.render(pPoseStack, bufferSource, pPackedLight));
            vehicle.getDecorationUnits().values().forEach(decorationUnit -> decorationUnit.render(pPoseStack, bufferSource, pPackedLight));
            if (bakedInstance != null) {
                BakedModelInstance instance = bakedInstance;
                vehicle.getBulletHoleParticles().forEach(bulletHoleParticle ->
                        bulletHoleParticle.renderOnVehicle(pPartialTick, pPoseStack, bufferSource, instance));
            } else {
                vehicle.getBulletHoleParticles().forEach(bulletHoleParticle ->
                        bulletHoleParticle.renderOnVehicle(pPartialTick, pPoseStack, bufferSource));
                model.applyPose(model.getBindPose());
            }
            vehicle.lastRenderTime = System.currentTimeMillis();
        } finally {
            pPoseStack.popPose();
        }
    }

    public static BakedModelInstance prepareModelInstance(AbstractVehicle vehicle, BaseDisplay display, float partialTick) {
        VehicleBedrockModel model = display.getModel();
        if (!model.hasBakedModel()) {
            throw new IllegalArgumentException("Baked instance requested for a v1-only display");
        }
        BakedModelInstance modelInstance = vehicle.getModelInstance(model);
        applyAnimationPose(vehicle, modelInstance, partialTick);
        return modelInstance;
    }

    private static void applyAnimationPose(AbstractVehicle vehicle, BakedModelInstance modelInstance,
                                           float partialTick) {
        if (vehicle instanceof IAnimationEntity<?, ?> animationEntity) {
            var animationInstance = animationEntity.getAnimationInstance();
            if (animationInstance != null) {
                animationInstance.getContext().setPartialTick(partialTick);
                animationInstance.tick();
                vehicle.applyRateLimitedBakedPose(modelInstance,
                        () -> BLENDER.blend(modelInstance.getBindPose(), animationInstance.getCurrentPose()));
                return;
            }
        }
        vehicle.resetBakedAnimationPose(modelInstance);
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

    /** 与通用渲染器一致的 baked 模型世界变换，供 baked Cube 命中检测使用。 */
    public static ModelTransform getModelTransform(AbstractVehicle vehicle, float partialTick) {
        Quaternionf rotation = new Quaternionf()
                .rotateY(Math.toRadians(-vehicle.yRotO))
                .rotateX(Math.toRadians(vehicle.xRotO))
                .rotateZ(Math.toRadians(vehicle.zRotO))
                .slerp(vehicle.rotYXZ(), partialTick);
        Vector3f root = vehicle.centerOffset.toVector3f();
        Vector3f rotatedRoot = new Vector3f(root).rotate(rotation);
        Vec3 origin = vehicle.position().add(root.x - rotatedRoot.x, root.y - rotatedRoot.y, root.z - rotatedRoot.z);
        return new ModelTransform(rotation, origin);
    }

    public record ModelTransform(Quaternionf rotation, Vec3 origin) {
        public ModelTransform {
            rotation = new Quaternionf(rotation);
        }

        @Override
        public Quaternionf rotation() {
            return new Quaternionf(rotation);
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
