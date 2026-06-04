package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.maydaymemory.mae.basic.Pose;
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
import org.ywzj.vehicle.entity.vehicle.custom.Ztl11;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class Ztl11Renderer extends EntityRenderer<Ztl11> {

    public Ztl11Renderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Ztl11 vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.ZTL11.getId()).orElse(null);
        if (display == null || display.getModel() == null) {
            return;
        }
        pPoseStack.pushPose();
        {
            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            Vec3 root = new Vec3(0, 0, 0);
            pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.xRotO, vehicle.getXRot())), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.zRotO, vehicle.getZRot())), (float) root.x, (float) root.y, (float) root.z);

            VehicleBedrockModel model = display.getModel();

            BedrockBone wheel1 = model.getBoneMap().get("wheel1");
            BedrockBone wheel2 = model.getBoneMap().get("wheel2");
            BedrockBone wheel3 = model.getBoneMap().get("wheel3");
            BedrockBone wheel4 = model.getBoneMap().get("wheel4");
            BedrockBone wheel5 = model.getBoneMap().get("wheel5");
            BedrockBone wheel6 = model.getBoneMap().get("wheel6");
            BedrockBone wheel7 = model.getBoneMap().get("wheel7");
            BedrockBone wheel8 = model.getBoneMap().get("wheel8");
            BedrockBone turret = model.getBoneMap().get("turret");
            BedrockBone cannon = model.getBoneMap().get("canno");
            BedrockBone machineGunBase = model.getBoneMap().get("machine_gun");
            BedrockBone machineGun = model.getBoneMap().get("bone17");

            // 轮子转速
            float vf = vehicle.getEntityData().get(Ztl11.FORWARD_SPEED);
            float t = (float) (System.currentTimeMillis() - vehicle.lastRenderTime) / 1000 * 20;
            float s = t * vf;
            float l = (float) 20 / 16;
            float r = s / (l * 3.1415f) * 360;
            vehicle.wheelRotation += r;
            vehicle.wheelRotation %= 360;

            // 轮子转向幅度
            float vt = vehicle.getEntityData().get(Ztl11.TURN_ANGLE);
            float turnRotation = vt * 16;

            // 炮塔旋转
            float turretYRot = 0;
            // 炮塔俯仰
            float turretXRot = 0;
            if (!vehicle.seats.isEmpty()) {
                PartUnit partUnit = vehicle.seats.get(0).partUnit;
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    turretYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                    turretXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.getXRot());
                }
            }

            // 车长机枪旋转
            float machineGunYRot = 0;
            // 车长机枪俯仰
            float machineGunXRot = 0;
            if (!vehicle.seats.isEmpty()) {
                PartUnit partUnit = vehicle.seats.get(1).partUnit;
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    machineGunYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                    machineGunXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.getXRot());
                }
            }

            // 应用程序动画
            wheel1.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
            wheel2.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
            wheel3.rotation.mul(Axis.YN.rotationDegrees(turnRotation * 0.5f));
            wheel5.rotation.mul(Axis.YN.rotationDegrees(turnRotation * 0.5f));
            wheel6.rotation.mul(Axis.YN.rotationDegrees(-turnRotation * 0.5f));
            wheel4.rotation.mul(Axis.YN.rotationDegrees(-turnRotation * 0.5f));
            wheel8.rotation.mul(Axis.YN.rotationDegrees(-turnRotation));
            wheel7.rotation.mul(Axis.YN.rotationDegrees(-turnRotation));
            wheel1.rotation.mul(Axis.XP.rotationDegrees(vehicle.wheelRotation));
            wheel2.rotation.mul(Axis.XP.rotationDegrees(vehicle.wheelRotation));
            wheel3.rotation.mul(Axis.XP.rotationDegrees(vehicle.wheelRotation));
            wheel4.rotation.mul(Axis.XP.rotationDegrees(vehicle.wheelRotation));
            wheel5.rotation.mul(Axis.XP.rotationDegrees(vehicle.wheelRotation));
            wheel6.rotation.mul(Axis.XP.rotationDegrees(vehicle.wheelRotation));
            wheel7.rotation.mul(Axis.XP.rotationDegrees(vehicle.wheelRotation));
            wheel8.rotation.mul(Axis.XP.rotationDegrees(vehicle.wheelRotation));
            turret.rotation.mul(Axis.YN.rotationDegrees(turretYRot));
            cannon.rotation.mul(Axis.XP.rotationDegrees(turretXRot));
            machineGunBase.rotation.mul(Axis.YN.rotationDegrees(machineGunYRot));
            machineGun.rotation.mul(Axis.XN.rotationDegrees(machineGunXRot));

            Pose pose = model.getPose();

            var instance = vehicle.getAnimationInstance();
            if (instance != null) {
                instance.tick();
                model.applyPose(BLENDER.blend(pose, instance.getCurrentPose()));
            }

            vehicle.lastRenderTime = System.currentTimeMillis();
            model.renderToBuffer(pPoseStack, bufferSource, display.getTexture(), vehicle.isDestroyed() ? 64 : pPackedLight);
            model.renderSpecialBones(pPoseStack, bufferSource, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY);

            // 渲染部件
            vehicle.getPartUnits().forEach(partUnit -> partUnit.render(pPoseStack, bufferSource, pPackedLight));
            vehicle.getDecorationUnits().values().forEach(decorationUnit -> decorationUnit.render(pPoseStack, bufferSource, pPackedLight));

            model.applyPose(model.getBindPose());
        }
        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Ztl11 pEntity) {
        return null;
    }

}
