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
import org.ywzj.vehicle.entity.vehicle.Ztl11;
import org.ywzj.vehicle.resource.BedrockModelLoader;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class Ztl11Renderer extends EntityRenderer<Ztl11> {

    public Ztl11Renderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Ztl11 pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        BedrockModel model = BedrockModelLoader.getModel(AllVehicles.ZTL11.getVisualBedrockModel());
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(AllVehicles.ZTL11.getVisualBedrockTexture()));

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
        float vf = pEntity.getEntityData().get(Ztl11.FORWARD_SPEED);
        float t = (float) (System.currentTimeMillis() - pEntity.lastRenderTime) / 1000 * 20;
        float s = t * vf;
        float l = (float) 20 / 16;
        float r = s / (l * 3.1415f) * 360;
        pEntity.wheelRotation += r;
        pEntity.wheelRotation %= 360;

        // 轮子转向幅度
        float vt = pEntity.getEntityData().get(Ztl11.TURN_SPEED);
        float turnRotation = vt * 16;

        // 炮塔旋转
        float turretYRot = 0;
        // 炮塔俯仰
        float turretXRot = 0;
        if (!pEntity.seats.isEmpty()) {
            PartUnit partUnit = pEntity.seats.get(0).partUnit;
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
            PartUnit partUnit = pEntity.seats.get(1).partUnit;
            if (partUnit instanceof WeaponUnit weaponUnit) {
                machineGunYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                machineGunXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.getXRot());
            }
        }

        // 应用动画
        wheel1.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
        wheel2.rotation.mul(Axis.YN.rotationDegrees(turnRotation));
        wheel3.rotation.mul(Axis.YN.rotationDegrees(turnRotation * 0.5f));
        wheel5.rotation.mul(Axis.YN.rotationDegrees(turnRotation * 0.5f));
        wheel6.rotation.mul(Axis.YN.rotationDegrees(-turnRotation * 0.5f));
        wheel4.rotation.mul(Axis.YN.rotationDegrees(-turnRotation * 0.5f));
        wheel8.rotation.mul(Axis.YN.rotationDegrees(-turnRotation));
        wheel7.rotation.mul(Axis.YN.rotationDegrees(-turnRotation));
        wheel1.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel2.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel3.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel4.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel5.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel6.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel7.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        wheel8.rotation.mul(Axis.XN.rotationDegrees(pEntity.wheelRotation));
        turret.rotation.mul(Axis.YN.rotationDegrees(turretYRot));
        cannon.rotation.mul(Axis.XN.rotationDegrees(180 + turretXRot));
        machineGunBase.rotation.mul(Axis.YN.rotationDegrees(machineGunYRot));
        machineGun.rotation.mul(Axis.XN.rotationDegrees(machineGunXRot));

        pEntity.lastRenderTime = System.currentTimeMillis();
        model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);

        Quaternionf reset = new Quaternionf(0, 0, 0, 1);
        wheel1.rotation.set(reset);
        wheel2.rotation.set(reset);
        wheel3.rotation.set(reset);
        wheel4.rotation.set(reset);
        wheel5.rotation.set(reset);
        wheel6.rotation.set(reset);
        wheel7.rotation.set(reset);
        wheel8.rotation.set(reset);
        turret.rotation.set(reset);
        cannon.rotation.set(reset);
        machineGunBase.rotation.set(reset);
        machineGun.rotation.set(reset);

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Ztl11 pEntity) {
        return null;
    }

}
