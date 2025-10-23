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
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.entity.vehicle.Ka50;
import org.ywzj.vehicle.vehicle.PartUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class Ka50Renderer extends EntityRenderer<Ka50> {

    public Ka50Renderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Ka50 pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        BedrockModel model = BedrockModelLoader.getModel(pEntity.getVehicleType().getVisualBedrockModel());
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(pEntity.getVehicleType().getVisualBedrockTexture()));

        BedrockBone propeller = model.getBoneMap().get("1");
        BedrockBone propeller2 = model.getBoneMap().get("2");
        BedrockBone autoCannon = model.getBoneMap().get("auto_cannon");
        BedrockBone czg = model.getBoneMap().get("czg");
        BedrockBone zjg = model.getBoneMap().get("zjg");
        pEntity.propellerRotation += pEntity.getPower() / 10;
        pEntity.propellerRotation %= 360;

        // 炮塔旋转
        float turretYRot = 0;
        // 炮塔俯仰
        float turretXRot = 0;
        if (!pEntity.seats.isEmpty()) {
            PartUnit partUnit = pEntity.getPartUnits().get(1);
            if (partUnit instanceof WeaponUnit weaponUnit) {
                turretYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.yRot);
                turretXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.xRot);
            }
        }

        propeller.rotation.mul(Axis.YN.rotationDegrees(-pEntity.propellerRotation));
        propeller2.rotation.mul(Axis.YN.rotationDegrees(pEntity.propellerRotation));
        autoCannon.rotation.mul(Axis.YN.rotationDegrees(turretYRot));
        autoCannon.rotation.mul(Axis.XN.rotationDegrees(turretXRot));
        float d1 = 0;
        float d2 = 0;
        if (pEntity.controlUnit.left || pEntity.controlUnit.right) {
            if (pEntity.controlUnit.left) {
                d1 = -10;
            } else {
                d1 = 10;
            }
        }
        if (pEntity.controlUnit.forward || pEntity.controlUnit.backward) {
            if (pEntity.controlUnit.forward) {
                d2 = 10;
            } else {
                d2 = -10;
            }
        } else {
            d2 = Mth.clamp((pEntity.controlUnit.xRot - pEntity.getXRot()) / 30 * 10, -10, 10);
        }
        czg.rotation.mul(Axis.ZN.rotationDegrees(d1));
        czg.rotation.mul(Axis.XN.rotationDegrees(d2));
        zjg.rotation.mul(Axis.XN.rotationDegrees(-(float) pEntity.getCollectivePitch() / 100 * 20));

        pEntity.lastRenderTime = System.currentTimeMillis();
        model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);

        Quaternionf reset = new Quaternionf(0, 0, 0, 1);
        propeller.rotation.set(reset);
        propeller2.rotation.set(reset);
        autoCannon.rotation.set(reset);
        czg.rotation.set(reset);
        zjg.rotation.set(reset);

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Ka50 pEntity) {
        return null;
    }

}
