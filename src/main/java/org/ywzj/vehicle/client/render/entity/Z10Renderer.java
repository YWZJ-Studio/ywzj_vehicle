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
import org.ywzj.vehicle.vehicle.PartUnit;

public class Z10Renderer extends EntityRenderer<Z10> {

    public Z10Renderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Z10 pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
//        if (Math.abs(pEntity.xRotO - pEntity.getXRot()) > 90) {
//            pPoseStack.rotateAround(Axis.XP.rotationDegrees(pEntity.getXRot()), (float) root.x, (float) root.y, (float) root.z);
//        } else {
//            pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
//        }
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        BedrockModel model = BedrockModelLoader.getModel(AllVehicles.Z10.getVisualBedrockModel());
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(AllVehicles.Z10.getVisualBedrockTexture()));

        BedrockBone propeller = model.getBoneMap().get("z10w_top");
        BedrockBone propeller2 = model.getBoneMap().get("z10w_tail");
        BedrockBone camera = model.getBoneMap().get("z10w_camera");
        BedrockBone cannon = model.getBoneMap().get("z10w_canno");
        pEntity.propellerRotation += (float) pEntity.getEngineSpeed() / 5;
        pEntity.propellerRotation %= 360;

        // 炮塔旋转
        float turretYRot = 0;
        float turretYRotAim = 0;
        // 炮塔俯仰
        float turretXRot = 0;
        float turretXRotAim = 0;
        if (!pEntity.operatorUnits.isEmpty()) {
            PartUnit weaponUnit = pEntity.operatorUnits.get(0);
            turretYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.yRot);
            turretXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.xRot);
            turretYRotAim = weaponUnit.yAimRot;
            turretXRotAim = weaponUnit.xAimRot;
        }

        propeller.rotation.mul(Axis.YN.rotationDegrees(pEntity.propellerRotation));
        propeller2.rotation.mul(Axis.XN.rotationDegrees(pEntity.propellerRotation * 5));
        cannon.rotation.mul(Axis.YN.rotationDegrees(turretYRot + 180));
        cannon.rotation.mul(Axis.XN.rotationDegrees(-turretXRot));
        camera.rotation.mul(Axis.YN.rotationDegrees(turretYRotAim + 180));
        camera.rotation.mul(Axis.XN.rotationDegrees(-turretXRotAim));

        pEntity.lastRenderTime = System.currentTimeMillis();
        model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);

        Quaternionf reset = new Quaternionf(0, 0, 0, 1);
        propeller.rotation.set(reset);
        propeller2.rotation.set(reset);
        camera.rotation.set(reset);
        cannon.rotation.set(reset);

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Z10 pEntity) {
        return null;
    }

}
