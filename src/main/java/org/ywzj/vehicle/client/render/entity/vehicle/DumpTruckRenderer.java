package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
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
import org.ywzj.vehicle.all.AllVehicles;
import org.ywzj.vehicle.entity.vehicle.DumpTruck;
import org.ywzj.vehicle.resource.BedrockModelLoader;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.RotatableUnit;

public class DumpTruckRenderer extends EntityRenderer<DumpTruck> {

    public DumpTruckRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(DumpTruck pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        BedrockModel model = BedrockModelLoader.getModel(AllVehicles.DUMP_TRUCK.getVisualBedrockModel());
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(AllVehicles.DUMP_TRUCK.getVisualBedrockTexture()));

        BedrockBone wheel1 = model.getBoneMap().get("wheel3");
        BedrockBone wheel2 = model.getBoneMap().get("wheel6");
        BedrockBone wheel3 = model.getBoneMap().get("wheel2");
        BedrockBone wheel4 = model.getBoneMap().get("wheel4");
        BedrockBone wheel5 = model.getBoneMap().get("wheel8");
        BedrockBone wheel6 = model.getBoneMap().get("wheel7");
        BedrockBone control = model.getBoneMap().get("control");
        BedrockBone bed = model.getBoneMap().get("back");
        BedrockBone bedDoor = model.getBoneMap().get("back_door");
        BedrockBone lift = model.getBoneMap().get("lift");
        BedrockBone lift2 = model.getBoneMap().get("lift2");
        BedrockBone lift3 = model.getBoneMap().get("lift3");

        // 轮子转速
        float vf = pEntity.getEntityData().get(DumpTruck.FORWARD_SPEED);
        float t = (float) (System.currentTimeMillis() - pEntity.lastRenderTime) / 1000 * 20;
        float s = t * vf;
        float l = (float) 20 / 16;
        float r = s / (l * 3.1415f) * 360;
        pEntity.wheelRotation += r;
        pEntity.wheelRotation %= 360;

        // 轮子转向幅度
        float vt = pEntity.getEntityData().get(DumpTruck.TURN_SPEED);
        float turnRotation = vt * 16;

        // 车斗
        float bedXRot = 0;
        PartUnit dumpTruckBed = pEntity.seats.get(0).partUnit;
        if (dumpTruckBed instanceof RotatableUnit rotatableUnit) {
            bedXRot = rotatableUnit.getXRot();
        }

        // 应用动画
        wheel1.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
        wheel2.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
        control.rotation.mul(Axis.YN.rotationDegrees(turnRotation * 15 - 90));
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
        wheel1.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel2.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel3.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel4.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel5.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel6.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));

        pEntity.lastRenderTime = System.currentTimeMillis();
        model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);

        Quaternionf reset = new Quaternionf(0, 0, 0, 1);
        wheel1.rotation.set(reset);
        wheel2.rotation.set(reset);
        wheel3.rotation.set(reset);
        wheel4.rotation.set(reset);
        wheel5.rotation.set(reset);
        wheel6.rotation.set(reset);
        control.rotation.set(reset);
        bed.rotation.set(reset);
        bedDoor.rotation.set(reset);
        lift.rotation.set(reset);

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DumpTruck pEntity) {
        return null;
    }

}
