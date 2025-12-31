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
import org.ywzj.vehicle.entity.vehicle.Bgm71tow;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class Bgm71towRenderer extends EntityRenderer<Bgm71tow> {

    public Bgm71towRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Bgm71tow pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.BGM_71_TOW.getId()).orElse(null);
        if (display == null || display.getModel() == null) {
            return;
        }

        BedrockModel model = display.getModel();
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(display.getTexture()));

        BedrockBone tow = model.getBoneMap().get("rot");
        BedrockBone towBarrel = model.getBoneMap().get("tow");

        float turretYRot = 0;
        float turretXRot = 0;
        if (!pEntity.seats.isEmpty()) {
            PartUnit partUnit = pEntity.seats.get(0).partUnit;
            if (partUnit instanceof WeaponUnit weaponUnit) {
                turretYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                turretXRot = -Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.getXRot());
            }
        }

        // 应用动画
        tow.rotation.mul(Axis.YN.rotationDegrees(turretYRot));
        towBarrel.rotation.mul(Axis.XN.rotationDegrees(turretXRot));

        pEntity.lastRenderTime = System.currentTimeMillis();
        model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);

        Quaternionf reset = new Quaternionf(0, 0, 0, 1);
        tow.rotation.set(reset);
        towBarrel.rotation.set(reset);

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Bgm71tow pEntity) {
        return null;
    }

}
