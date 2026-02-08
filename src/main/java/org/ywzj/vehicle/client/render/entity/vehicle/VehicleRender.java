package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VehicleRender<T extends AbstractVehicle> extends EntityRenderer<T> {

    public static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    private static final Object[] EMPTY_ARGS = new Object[0];

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
        BedrockModel model = display.getModel();
        if (model == null) {
            return;
        }
        pPoseStack.pushPose();
        {
            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            Vec3 root = new Vec3(0, 0, 0);
            pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.xRotO, vehicle.getXRot())), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.zRotO, vehicle.getZRot())), (float) root.x, (float) root.y, (float) root.z);

            if (vehicle instanceof IAnimationEntity<?,?> animationEntity) {
                var instance = animationEntity.getAnimationInstance();
                if (instance != null) {
                    instance.tick();
                    instance.getContext().setPartialTick(pPartialTick);
                    model.applyPose(BLENDER.blend(model.getBindPose(), instance.getCurrentPose()));
                }
            }

            VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(display.getTexture()));
            model.renderToBuffer(pPoseStack, builder, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY);
            model.getBoneIndexes().forEach(bone -> bone.visible = true);
            model.applyPose(model.getBindPose());
            vehicle.lastRenderTime = System.currentTimeMillis();
        }
        pPoseStack.popPose();
    }

    @SubscribeEvent
    public static void onFire(VehicleFireEvent.Post event) {
        if (event.isClientSide()) {
//            event.getVehicle().getAnimationInstance().onFire(event.getWeapon());
        }
    }

    @Override
    public ResourceLocation getTextureLocation(@NotNull T pEntity) {
        return null;
    }

}
