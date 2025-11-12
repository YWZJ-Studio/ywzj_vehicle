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
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.Z10;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

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

        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.Z10.getId()).orElse(null);
        if (display == null || display.getModel() == null) {
            return;
        }

        BedrockModel model = display.getModel();
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(display.getTexture()));

        BedrockBone propeller = model.getBoneMap().get("z10w_top");
        BedrockBone propeller2 = model.getBoneMap().get("z10w_tail");
        BedrockBone camera = model.getBoneMap().get("z10w_camera");
        BedrockBone cannon = model.getBoneMap().get("z10w_canno");
        pEntity.propellerRotation += pEntity.getPower() / 5;
        pEntity.propellerRotation %= 360;

        // 炮塔旋转
        float turretYRot = 0;
        float turretYRotAim = 0;
        // 炮塔俯仰
        float turretXRot = 0;
        float turretXRotAim = 0;
        if (!pEntity.seats.isEmpty()) {
            PartUnit partUnit = pEntity.seats.get(0).partUnit;
            if (partUnit instanceof WeaponUnit weaponUnit) {
                turretYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                turretXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.getXRot());
                turretYRotAim = weaponUnit.getYAimRot();
                turretXRotAim = weaponUnit.getXAimRot();
            }
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
