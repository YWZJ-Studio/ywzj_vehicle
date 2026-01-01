package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.render.animation.TrackAnimationInstance;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.Ztz99a;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class Ztz99aRenderer extends VehicleRender<Ztz99a> {
    private static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    public Ztz99aRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Ztz99a pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();
        {
            super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.ZTZ99A.getId()).orElse(null);
            if (display == null || display.getModel() == null) {
                return;
            }

            BedrockModel model = display.getModel();
            VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(display.getTexture()));

            TrackAnimationInstance instance = pEntity.getTrackAnimationInstance();
            if (instance == null) {
                var animations = display.getAnimations();
                instance = new TrackAnimationInstance(animations.get("tread_l_move"), animations.get("tread_r_move"));
                pEntity.setTrackAnimationInstance(instance);
            }

            float deltaTime = (System.currentTimeMillis() - pEntity.lastRenderTime) / 1000f;

            float vf = pEntity.getEntityData().get(Ztz99a.FORWARD_SPEED); // 线速度
            float omega = pEntity.getEntityData().get(Ztz99a.TURN_SPEED); // 角速度
            float trackWidth = 3.0f / 20f;

            float leftTrackSpeed = vf + omega * trackWidth / 2;
            float rightTrackSpeed = vf - omega * trackWidth / 2;

            leftTrackSpeed *= 20f; // 转换为米/秒
            rightTrackSpeed *= 20f; // 转换为米/秒

            instance.advanceProgress(leftTrackSpeed, rightTrackSpeed, deltaTime, 0.25f);

            Pose bindPose = model.getBindPose();
            Pose blended = BLENDER.blend(bindPose, instance.evaluate());
            model.applyPose(blended);

            for (int i = 0; i < 13; i++) {
                String boneName = "hull_big_" + i;
                BedrockBone bone = model.getBoneMap().get(boneName);
                if (bone != null) {
                    float angle = i < 7 ? instance.leftWheelDegrees(0.375f) : instance.rightWheelDegrees(0.375f);
                    bone.rotation.mul(Axis.XP.rotationDegrees(angle));
                }
            }

            for (int i = 0; i < 5; i++) {
                String boneName = "hull_small_" + i;
                BedrockBone bone = model.getBoneMap().get(boneName);
                if (bone != null) {
                    float angle = i < 3 ? instance.leftWheelDegrees(0.28f) : instance.rightWheelDegrees(0.28f);
                    bone.rotation.mul(Axis.XP.rotationDegrees(angle));
                }
            }

            BedrockBone turret = model.getBoneMap().get("turret");
            BedrockBone cannon = model.getBoneMap().get("canno");
            BedrockBone machineGunBase = model.getBoneMap().get("machine_gun");
            BedrockBone machineGun = model.getBoneMap().get("machine_gun_high");

            // 炮塔旋转
            float turretYRot = 0;
            // 炮塔俯仰
            float turretXRot = 0;
            if (!pEntity.seats.isEmpty()) {
                PartUnit<?> partUnit = pEntity.seats.get(0).partUnit;
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    turretYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                    turretXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.getXRot());
                }
            }

            // 车长机枪旋转
            float machineGunYRot = 0;
            // 车长机枪俯仰
            float machineGunXRot = 0;
            if (!pEntity.seats.isEmpty()) {
                PartUnit<?> partUnit = pEntity.seats.get(1).partUnit;
                if (partUnit instanceof WeaponUnit weaponUnit) {
                    machineGunYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                    machineGunXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.getXRot());
                }
            }


            turret.rotation.mul(Axis.YN.rotationDegrees(turretYRot));
            cannon.rotation.mul(Axis.XN.rotationDegrees(-turretXRot));
            machineGunBase.rotation.mul(Axis.YN.rotationDegrees(machineGunYRot));
            machineGun.rotation.mul(Axis.XN.rotationDegrees(-machineGunXRot));

            pEntity.lastRenderTime = System.currentTimeMillis();
            model.renderToBuffer(pPoseStack, builder, pEntity.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY);

            model.applyPose(model.getBindPose());
        }
        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Ztz99a pEntity) {
        return null;
    }

}
