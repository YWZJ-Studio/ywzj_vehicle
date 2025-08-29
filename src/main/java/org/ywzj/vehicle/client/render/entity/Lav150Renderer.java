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
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.BedrockModelLoader;
import org.ywzj.vehicle.entity.vehicle.Lav150;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class Lav150Renderer extends EntityRenderer<Lav150> {

    public Lav150Renderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Lav150 pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(180), (float) root.x, (float) root.y, (float) root.z);


        BedrockModel model = BedrockModelLoader.getModel(BedrockModelLoader.LAV150);
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(YwzjVehicle.modLoc("textures/entity/lav150.png")));

        BedrockBone wheel1 = model.getBoneMap().get("wheel1");
        BedrockBone wheel2 = model.getBoneMap().get("wheel2");
        BedrockBone wheel3 = model.getBoneMap().get("wheel3");
        BedrockBone wheel4 = model.getBoneMap().get("wheel4");
        BedrockBone cannon = model.getBoneMap().get("canno");
        BedrockBone barrel = model.getBoneMap().get("barrel");

        // 轮子转速
        float vf = pEntity.getEntityData().get(Lav150.FORWARD_SPEED);
        float t = (float) (System.currentTimeMillis() - pEntity.lastRenderTime) / 1000 * 20;
        float s = t * vf;
        float l = (float) 20 / 16;
        float r = s / (l * 3.1415f) * 360;
        pEntity.wheelRotation += r;
        pEntity.wheelRotation %= 360;

        // 轮子转向幅度
        float vt = pEntity.getEntityData().get(Lav150.TURN_SPEED);
        float turnRotation = vt * 16;

        // 炮塔旋转
        float machineGunYRot = 0;
        // 炮塔俯仰
        float machineGunXRot = 0;
        if (!pEntity.weaponUnits.isEmpty()) {
            WeaponUnit weaponUnit = pEntity.weaponUnits.get(0);
            machineGunYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.yRot);
            machineGunXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.xRot);
        }

        // 应用动画
        wheel1.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
        wheel2.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
        wheel1.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel2.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel3.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel4.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        cannon.rotation.mul(Axis.YN.rotationDegrees(machineGunYRot));
        barrel.rotation.mul(Axis.XN.rotationDegrees(machineGunXRot));

        pEntity.lastRenderTime = System.currentTimeMillis();
        model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);

        Quaternionf reset = new Quaternionf(0, 0, 0, 1);
        wheel1.rotation.set(reset);
        wheel2.rotation.set(reset);
        wheel3.rotation.set(reset);
        wheel4.rotation.set(reset);
        cannon.rotation.set(reset);
        barrel.rotation.set(reset);

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Lav150 pEntity) {
        return null;
    }

}
