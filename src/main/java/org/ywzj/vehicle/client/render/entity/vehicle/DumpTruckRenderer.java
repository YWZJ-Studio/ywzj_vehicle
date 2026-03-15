package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.maydaymemory.mae.basic.Pose;
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
import org.joml.Quaternionf;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.DumpTruck;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class DumpTruckRenderer extends EntityRenderer<DumpTruck> {

    public DumpTruckRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(DumpTruck vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.DUMP_TRUCK.getId()).orElse(null);
        if (display == null || display.getModel() == null) {
            return;
        }
        pPoseStack.pushPose();
        {
            BedrockModel model = display.getModel();
            VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(display.getTexture()));

            BedrockBone wheel1 = model.getBoneMap().get("wheel3");
            BedrockBone wheel2 = model.getBoneMap().get("wheel6");
            BedrockBone wheel3 = model.getBoneMap().get("wheel2");
            BedrockBone wheel4 = model.getBoneMap().get("wheel4");
            BedrockBone wheel5 = model.getBoneMap().get("wheel8");
            BedrockBone wheel6 = model.getBoneMap().get("wheel7");
            BedrockBone control = model.getBoneMap().get("control");
            Quaternionf controlO = new Quaternionf(control.rotation);
            BedrockBone bed = model.getBoneMap().get("back");
            BedrockBone bedDoor = model.getBoneMap().get("back_door");
            BedrockBone lift = model.getBoneMap().get("lift");
            BedrockBone lift2 = model.getBoneMap().get("lift2");
            BedrockBone lift3 = model.getBoneMap().get("lift3");

            // 轮子转速
            float vf = vehicle.getEntityData().get(DumpTruck.FORWARD_SPEED);
            float t = (float) (System.currentTimeMillis() - vehicle.lastRenderTime) / 1000 * 20;
            float s = t * vf;
            float l = (float) 20 / 16;
            float r = s / (l * 3.1415f) * 360;
            vehicle.wheelRotation += r;
            vehicle.wheelRotation %= 360;

            // 轮子转向幅度
            float vt = vehicle.getEntityData().get(DumpTruck.TURN_ANGLE);
            float turnRotation = vt * 16;

            // 车斗
            float bedXRot = 0;
            PartUnit<?> dumpTruckBed = vehicle.seats.get(0).partUnit.getSubPartUnits().get(0);
            if (dumpTruckBed instanceof RotatableUnit<?> rotatableUnit) {
                bedXRot = Mth.lerp(pPartialTick, rotatableUnit.xRotO, rotatableUnit.getXRot());
            }

            // 应用程序动画
            wheel1.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
            wheel2.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
            control.rotation.mul(Axis.YN.rotationDegrees(turnRotation * 15));
            bed.rotation.mul(Axis.XN.rotationDegrees(bedXRot));
            bedDoor.rotation.mul(Axis.XN.rotationDegrees(-bedXRot * 2));
            lift.rotation.mul(Axis.XN.rotationDegrees(-70 + 65 * (-bedXRot / 45)));
            double a = Math.toRadians(-bedXRot);
            double c = Math.toRadians(180 + bedXRot + 65 * bedXRot / 45);
            float b = (float) (Math.sin(a) * 87 / Math.sin(c));
            float d = (float) (65.46 - b);
            if (d <= 18) {
                lift2.y = 18 - d;
            } else {
                lift2.y = 0;
                lift3.y = 23 - (d - 18);
            }
            wheel1.rotation.mul(Axis.XN.rotationDegrees(vehicle.wheelRotation));
            wheel2.rotation.mul(Axis.XN.rotationDegrees(vehicle.wheelRotation));
            wheel3.rotation.mul(Axis.XN.rotationDegrees(vehicle.wheelRotation));
            wheel4.rotation.mul(Axis.XN.rotationDegrees(vehicle.wheelRotation));
            wheel5.rotation.mul(Axis.XN.rotationDegrees(vehicle.wheelRotation));
            wheel6.rotation.mul(Axis.XN.rotationDegrees(vehicle.wheelRotation));

            Pose pose = model.getPose();
            
            var instance = vehicle.getAnimationInstance();
            if (instance != null) {
                instance.tick();
                model.applyPose(BLENDER.blend(pose, instance.getCurrentPose()));
            }

            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            Vec3 root = new Vec3(0, 0, 0);
            pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.xRotO, vehicle.getXRot())), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.zRotO, vehicle.getZRot())), (float) root.x, (float) root.y, (float) root.z);
            vehicle.lastRenderTime = System.currentTimeMillis();
            model.renderToBuffer(pPoseStack, builder, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY);

            // 渲染饰品
            VehicleRender.renderDecorations(vehicle, model, pPoseStack, bufferSource, pPackedLight);

            model.applyPose(model.getBindPose());
            Quaternionf reset = new Quaternionf(0, 0, 0, 1);
            control.rotation.set(controlO);
            lift.rotation.set(reset);
        }
        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DumpTruck pEntity) {
        return null;
    }

}
