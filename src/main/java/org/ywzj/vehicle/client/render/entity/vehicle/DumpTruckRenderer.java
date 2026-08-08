package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.entity.vehicle.custom.DumpTruck;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RotatableUnit;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class DumpTruckRenderer extends EntityRenderer<DumpTruck> {

    public DumpTruckRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(DumpTruck vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        VehicleRender.renderHitbox(vehicle, pPoseStack, bufferSource);
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.DUMP_TRUCK.getId()).orElse(null);
        if (display == null || display.getModel() == null) {
            return;
        }
        pPoseStack.pushPose();
        {
            VehicleBedrockModel model = display.getModel();
            BakedModelInstance modelInstance = vehicle.getModelInstance();

            BoneState wheel1 = modelInstance.getBone("wheel3");
            BoneState wheel2 = modelInstance.getBone("wheel6");
            BoneState wheel3 = modelInstance.getBone("wheel2");
            BoneState wheel4 = modelInstance.getBone("wheel4");
            BoneState wheel5 = modelInstance.getBone("wheel8");
            BoneState wheel6 = modelInstance.getBone("wheel7");
            BoneState control = modelInstance.getBone("control");
            Quaternionf controlO = new Quaternionf(control.rotation);
            BoneState bed = modelInstance.getBone("back");
            BoneState bedDoor = modelInstance.getBone("back_door");
            BoneState lift = modelInstance.getBone("lift");
            BoneState lift2 = modelInstance.getBone("lift2");
            BoneState lift3 = modelInstance.getBone("lift3");

            // 轮子转速
            float vf = vehicle.getEntityData().get(DumpTruck.FORWARD_SPEED);
            float t = (float) (System.currentTimeMillis() - vehicle.lastRenderTime) / 1000 * 20;
            float s = t * vf;
            float l = (float) 20 / 16;
            float r = s / (l * 3.1415f) * 360;
            vehicle.wheelRotation += r;
            vehicle.wheelRotation %= 360;

            // 轮子转向幅度
            float vt = Mth.lerp(pPartialTick, vehicle.turnAngleO, vehicle.turnAngle);
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

            int modelLight = pPackedLight;
            if (vehicle.isDestroyed()) {
                int blockLight = (int) (LightTexture.block(pPackedLight) / 1.5f);
                int skyLight = (int) (LightTexture.sky(pPackedLight) / 1.5f);
                modelLight = LightTexture.pack(blockLight, skyLight);
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
            model.renderToBuffer(modelInstance, pPoseStack, bufferSource, display.getTexture(), modelLight);
            model.renderSpecialBones(modelInstance, pPoseStack, bufferSource, modelLight, OverlayTexture.NO_OVERLAY, vehicle == LocalVehiclePlayer.instance.vehicle);
            // 渲染部件
            vehicle.getPartUnits().forEach(partUnit -> partUnit.render(pPoseStack, bufferSource, pPackedLight));
            vehicle.getDecorationUnits().values().forEach(decorationUnit -> decorationUnit.render(pPoseStack, bufferSource, pPackedLight));
            // 渲染弹孔
            vehicle.getBulletHoleParticles().forEach(bulletHoleParticle -> bulletHoleParticle.renderOnVehicle(pPartialTick, pPoseStack, bufferSource, modelInstance));
            vehicle.lastRenderTime = System.currentTimeMillis();

            modelInstance.applyPose(modelInstance.getBindPose());
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
