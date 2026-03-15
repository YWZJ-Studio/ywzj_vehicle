package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.vehicle.parts.DecorationUnit;

import java.util.Optional;

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
            pPoseStack.rotateAround(Axis.YP.rotationDegrees(-vehicle.getViewYRot(pPartialTick)), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(vehicle.getViewXRot(pPartialTick)), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getViewZRot(pPartialTick)), (float) root.x, (float) root.y, (float) root.z);
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
            VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(display.getTexture()));
            model.renderToBuffer(pPoseStack, builder, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY);
            model.renderSpecialBones(pPoseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            // 渲染饰品
            renderDecorations(vehicle, model, pPoseStack, bufferSource, pPackedLight);
            // 复原模型
            model.applyPose(model.getBindPose());
            vehicle.lastRenderTime = System.currentTimeMillis();
        }
        pPoseStack.popPose();
    }

    public static void renderDecorations(AbstractVehicle vehicle, BedrockModel vehicleModel, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        for (DecorationUnit decorationUnit : vehicle.getDecorationUnits().values()) {
            Optional<BaseDisplay> decorationDisplayOptional = ClientAssetsManager.INSTANCE.getDecorationDisplay(YwzjVehicle.resourceLocation(decorationUnit.decorationDisplayId));
            if (decorationDisplayOptional.isEmpty()) {
                continue;
            }
            BaseDisplay decorationDisplay =  decorationDisplayOptional.get();
            BedrockModel decorationModel = decorationDisplay.getModel();
            ResourceLocation decorationTexture = decorationDisplay.getTexture();
            if (decorationModel == null || decorationTexture == null) {
                continue;
            }
            BedrockBone bone = vehicleModel.getBoneMap().get(decorationUnit.baseBoneName);
            if (bone == null) {
                continue;
            }
            Quaternionf globalRotation = new Quaternionf(bone.rotation);
            globalRotation.rotateYXZ((float) Math.toRadians(-decorationUnit.selfYRot),
                    (float) Math.toRadians(decorationUnit.selfXRot),
                    (float) Math.toRadians(decorationUnit.selfZRot));
            Vector3f offset = bone.rotation.transform(decorationUnit.offsetFromBone.toVector3f());
            Vector3f globalPivot = new Vector3f(bone.x / 16.0F + offset.x, bone.y / 16.0F + offset.y, bone.z / 16.0F + offset.z);
            BedrockBone parent = bone.parent;
            while (parent != null) {
                parent.rotation.transform(globalPivot);
                globalPivot.add(parent.x / 16, parent.y / 16, parent.z / 16);
                globalRotation.premul(parent.rotation);
                parent = parent.parent;
            }
            Vector3f rot = new Vector3f();
            globalRotation.getEulerAnglesYXZ(rot);
            pPoseStack.pushPose();
            {
                pPoseStack.translate(globalPivot.x, globalPivot.y, globalPivot.z);
                pPoseStack.scale(decorationUnit.scale, decorationUnit.scale, decorationUnit.scale);
                pPoseStack.rotateAround(Axis.YP.rotation(rot.y), 0, 0, 0);
                pPoseStack.rotateAround(Axis.XP.rotation(rot.x), 0, 0, 0);
                pPoseStack.rotateAround(Axis.ZP.rotation(rot.z), 0, 0, 0);
                decorationUnit.offsetFromVehicle = new Vec3(globalPivot);
                decorationUnit.rotation = globalRotation;
                VertexConsumer decorationBuilder = bufferSource.getBuffer(RenderType.entityCutout(decorationTexture));
                decorationModel.renderToBuffer(pPoseStack, decorationBuilder, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY);
                if (decorationUnit.setting) {
                    RenderHelper.renderArrow3D(pPoseStack, bufferSource, 0.15f, 0.3f, 0, 255, 0, 255);
                }
            }
            pPoseStack.popPose();
        }
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
