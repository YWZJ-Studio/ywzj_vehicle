package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.entity.vehicle.custom.Quadcopter;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class QuadcopterRenderer extends EntityRenderer<Quadcopter> {

    public QuadcopterRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Quadcopter vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.QUADCOPTER.getId()).orElse(null);
        if (display == null || display.getModel() == null) {
            return;
        }
        pPoseStack.pushPose();
        {
            VehicleBedrockModel model = display.getModel();
            BakedModelInstance modelInstance = vehicle.getModelInstance();

            BoneState propellerUp1 = modelInstance.getBone("wing1_up");
            BoneState propellerDown1 = modelInstance.getBone("wing1_down");
            BoneState propellerUp2 = modelInstance.getBone("wing2_up");
            BoneState propellerDown2 = modelInstance.getBone("wing2_down");
            BoneState propellerUp3 = modelInstance.getBone("wing3_up");
            BoneState propellerDown3 = modelInstance.getBone("wing3_down");
            BoneState propellerUp4 = modelInstance.getBone("wing4_up");
            BoneState propellerDown4 = modelInstance.getBone("wing4_down");
            BoneState rope = modelInstance.getBone("rope");
            BoneState ropeConnect = modelInstance.getBone("rope_connect");

            vehicle.propellerRotation += vehicle.getPower() / 5;
            vehicle.propellerRotation %= 360;

            propellerUp1.rotation.mul(Axis.YN.rotationDegrees(vehicle.propellerRotation));
            propellerUp4.rotation.mul(Axis.YN.rotationDegrees(vehicle.propellerRotation));
            propellerDown2.rotation.mul(Axis.YN.rotationDegrees(vehicle.propellerRotation));
            propellerDown3.rotation.mul(Axis.YN.rotationDegrees(vehicle.propellerRotation));
            propellerDown1.rotation.mul(Axis.YN.rotationDegrees(-vehicle.propellerRotation));
            propellerDown4.rotation.mul(Axis.YN.rotationDegrees(-vehicle.propellerRotation));
            propellerUp2.rotation.mul(Axis.YN.rotationDegrees(-vehicle.propellerRotation));
            propellerUp3.rotation.mul(Axis.YN.rotationDegrees(-vehicle.propellerRotation));
            if (vehicle.getEntityData().get(Quadcopter.CABLE_LENGTH) > 0) {
                rope.visible = false;
                ropeConnect.visible = false;
            } else {
                rope.visible = true;
                ropeConnect.visible = true;
            }

            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            Vec3 root = new Vec3(0, 0, 0);
            pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.xRotO, vehicle.getXRot())), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.zRotO, vehicle.getZRot())), (float) root.x, (float) root.y, (float) root.z);

            var animationInstance = vehicle.getAnimationInstance();
            if (animationInstance != null) {
                animationInstance.getContext().setPartialTick(pPartialTick);
                animationInstance.tick();
                modelInstance.applyPose(BLENDER.blend(modelInstance.getPose(), animationInstance.getCurrentPose()));
            }
            model.renderToBuffer(modelInstance, pPoseStack, bufferSource, display.getTexture(), vehicle.isDestroyed() ? 64 : pPackedLight);
            model.renderSpecialBones(modelInstance, pPoseStack, bufferSource, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY, vehicle == LocalVehiclePlayer.instance.getVehicle());
            // 渲染部件
            vehicle.getPartUnits().forEach(partUnit -> partUnit.render(pPoseStack, bufferSource, pPackedLight));
            vehicle.getDecorationUnits().values().forEach(decorationUnit -> decorationUnit.render(pPoseStack, bufferSource, pPackedLight));
            // 渲染弹孔
            vehicle.getBulletHoleParticles().forEach(bulletHoleParticle -> bulletHoleParticle.renderOnVehicle(pPartialTick, pPoseStack, bufferSource, modelInstance));
            vehicle.lastRenderTime = System.currentTimeMillis();

            modelInstance.applyPose(modelInstance.getBindPose());
        }
        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Quadcopter pEntity) {
        return null;
    }

}
