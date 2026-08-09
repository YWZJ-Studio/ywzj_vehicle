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
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.entity.vehicle.custom.Ztl11;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class Ztl11Renderer extends EntityRenderer<Ztl11> {

    public Ztl11Renderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Ztl11 vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        VehicleRender.renderHitbox(vehicle, pPoseStack, bufferSource);
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.ZTL11.getId()).orElse(null);
        if (display == null || display.getModel() == null) {
            return;
        }
        pPoseStack.pushPose();
        {
            VehicleBedrockModel model = display.getModel();
            BakedModelInstance modelInstance = vehicle.getModelInstance();

            BoneState wheel1 = modelInstance.getBone("wheel1");
            BoneState wheel2 = modelInstance.getBone("wheel2");
            BoneState wheel3 = modelInstance.getBone("wheel3");
            BoneState wheel4 = modelInstance.getBone("wheel4");
            BoneState wheel5 = modelInstance.getBone("wheel5");
            BoneState wheel6 = modelInstance.getBone("wheel6");
            BoneState wheel7 = modelInstance.getBone("wheel7");
            BoneState wheel8 = modelInstance.getBone("wheel8");
            BoneState turret = modelInstance.getBone("turret");
            BoneState cannon = modelInstance.getBone("canno");
            BoneState machineGunBase = modelInstance.getBone("machine_gun");
            BoneState machineGun = modelInstance.getBone("bone17");

            // 轮子转速
            float vf = vehicle.getEntityData().get(Ztl11.FORWARD_SPEED);
            float t = (float) (System.currentTimeMillis() - vehicle.lastRenderTime) / 1000 * 20;
            float s = t * vf;
            float l = (float) 20 / 16;
            float r = s / (l * 3.1415f) * 360;
            vehicle.wheelRotation += r;
            vehicle.wheelRotation %= 360;

            // 轮子转向幅度
            float vt = Mth.lerp(pPartialTick, vehicle.turnAngleO, vehicle.turnAngle);
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
            model.renderSpecialBones(modelInstance, pPoseStack, bufferSource, modelLight, OverlayTexture.NO_OVERLAY, null, vehicle == LocalVehiclePlayer.instance.vehicle);
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
    public ResourceLocation getTextureLocation(Ztl11 pEntity) {
        return null;
    }

}
