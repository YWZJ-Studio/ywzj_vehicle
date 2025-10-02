package org.ywzj.vehicle.client.render.entity;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
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
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.entity.vehicle.Z10;

public class Z10Renderer extends EntityRenderer<Z10> {

    public Z10Renderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Z10 pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        BedrockModel model = BedrockModelLoader.getModel(AllVehicles.Z10.getVisualBedrockModel());
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(AllVehicles.Z10.getVisualBedrockTexture()));

        BedrockBone propeller = model.getBoneMap().get("z10w_top");
        BedrockBone propeller2 = model.getBoneMap().get("z10w_tail");
        pEntity.propellerRotation += (float) pEntity.getEngineSpeed() / 5;
        pEntity.propellerRotation %= 360;

//        // 轮子转速
//        float vf = pEntity.getEntityData().get(Z10.FORWARD_SPEED);
//        float t = (float) (System.currentTimeMillis() - pEntity.lastRenderTime) / 1000 * 20;
//        float s = t * vf;
//        float l = (float) 20 / 16;
//        float r = s / (l * 3.1415f) * 360;
//        pEntity.wheelRotation += r;
//        pEntity.wheelRotation %= 360;
//
//        // 轮子转向幅度
//        float vt = pEntity.getEntityData().get(Z10.TURN_SPEED);
//        float turnRotation = vt * 16;

//        // 炮塔旋转
//        float turretYRot = 0;
//        // 炮塔俯仰
//        float turretXRot = 0;
//        if (!pEntity.operatorUnits.isEmpty()) {
//            PartUnit weaponUnit = pEntity.operatorUnits.get(0);
//            turretYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.yRot);
//            turretXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.xRot);
//        }
//
//        // 车长机枪旋转
//        float machineGunYRot = 0;
//        // 车长机枪俯仰
//        float machineGunXRot = 0;
//        if (!pEntity.operatorUnits.isEmpty()) {
//            PartUnit weaponUnit = pEntity.operatorUnits.get(1);
//            machineGunYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.yRot);
//            machineGunXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.xRot);
//        }

        // 应用动画
//        wheel1.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
//        wheel2.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
//        wheel3.rotation.mul(Axis.YN.rotationDegrees(turnRotation * 0.5f));
//        wheel5.rotation.mul(Axis.YN.rotationDegrees(turnRotation * 0.5f));
//        wheel6.rotation.mul(Axis.YN.rotationDegrees(-turnRotation * 0.5f));
//        wheel4.rotation.mul(Axis.YN.rotationDegrees(-turnRotation * 0.5f));
//        wheel8.rotation.mul(Axis.YN.rotationDegrees(-turnRotation));
//        wheel7.rotation.mul(Axis.YN.rotationDegrees(-turnRotation));
//        wheel1.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
//        wheel2.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
//        wheel3.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
//        wheel4.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
//        wheel5.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
//        wheel6.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
//        wheel7.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
//        wheel8.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));

        propeller.rotation.mul(Axis.YN.rotationDegrees(pEntity.propellerRotation));
        propeller2.rotation.mul(Axis.XN.rotationDegrees(pEntity.propellerRotation * 5));

        pEntity.lastRenderTime = System.currentTimeMillis();
        model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);

        Quaternionf reset = new Quaternionf(0, 0, 0, 1);
        propeller.rotation.set(reset);
        propeller2.rotation.set(reset);

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Z10 pEntity) {
        return null;
    }

}
