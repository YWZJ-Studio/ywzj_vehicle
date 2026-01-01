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
import org.ywzj.vehicle.entity.vehicle.M1a2;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class M1a2Renderer extends VehicleRender<M1a2> {
    private static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    public M1a2Renderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(M1a2 pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();
        {
            super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.M1A2.getId()).orElse(null);
            if (display == null || display.getModel() == null) {
                pPoseStack.popPose();
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

            float vf = pEntity.getEntityData().get(M1a2.FORWARD_SPEED); // 线速度
            float omega = pEntity.getEntityData().get(M1a2.TURN_SPEED); // 角速度
            float trackWidth = 3.0f / 20f;

            float leftTrackSpeed = vf + omega * trackWidth / 2;
            float rightTrackSpeed = vf - omega * trackWidth / 2;

            leftTrackSpeed *= 20f; // 转换为米/秒
            rightTrackSpeed *= 20f; // 转换为米/秒

            instance.advanceProgress(leftTrackSpeed, rightTrackSpeed, deltaTime, 0.25f);

            Pose bindPose = model.getBindPose();
            Pose blended = BLENDER.blend(bindPose, instance.evaluate());
            model.applyPose(blended);

            for (int i = 0; i < 19; i++) {
                String boneName = "wheel" + i;
                BedrockBone bone = model.getBoneMap().get(boneName);
                if (bone != null) {
                    float angle = i < 10 ? instance.leftWheelDegrees(0.3125f) : instance.rightWheelDegrees(0.3125f);
                    bone.rotation.mul(Axis.XP.rotationDegrees(angle));
                }
            }

            BedrockBone turret = model.getBoneMap().get("turret");
            BedrockBone cannon = model.getBoneMap().get("cannon");
            BedrockBone machineGunBase = model.getBoneMap().get("mg");
            BedrockBone machineGun = model.getBoneMap().get("mg_up");

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
    public ResourceLocation getTextureLocation(M1a2 pEntity) {
        return null;
    }

}
