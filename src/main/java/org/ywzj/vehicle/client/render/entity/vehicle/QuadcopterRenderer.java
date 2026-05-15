package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
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
import org.ywzj.vehicle.entity.vehicle.Quadcopter;

import static org.ywzj.vehicle.entity.vehicle.Quadcopter.CABLE_LENGTH;

public class QuadcopterRenderer extends EntityRenderer<Quadcopter> {

    public QuadcopterRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(Quadcopter vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(AllEntities.QUADCOPTER.getId()).orElse(null);
        if (display == null || display.getModel() == null) {
            return;
        }
        pPoseStack.pushPose();
        {
            BedrockModel model = display.getModel();

            BedrockBone propellerUp1 = model.getBoneMap().get("wing1_up");
            BedrockBone propellerDown1 = model.getBoneMap().get("wing1_down");
            BedrockBone propellerUp2 = model.getBoneMap().get("wing2_up");
            BedrockBone propellerDown2 = model.getBoneMap().get("wing2_down");
            BedrockBone propellerUp3 = model.getBoneMap().get("wing3_up");
            BedrockBone propellerDown3 = model.getBoneMap().get("wing3_down");
            BedrockBone propellerUp4 = model.getBoneMap().get("wing4_up");
            BedrockBone propellerDown4 = model.getBoneMap().get("wing4_down");
            BedrockBone ropeConnect = model.getBoneMap().get("rope_connect");
            BedrockBone rope = model.getBoneMap().get("rope");

            float cableLength = vehicle.getEntityData().get(CABLE_LENGTH);
            vehicle.propellerRotation += vehicle.getPower() / 5;
            vehicle.propellerRotation %= 360;

            propellerUp1.rotation.mul(Axis.YN.rotationDegrees(vehicle.propellerRotation));
            propellerUp4.rotation.mul(Axis.YN.rotationDegrees(vehicle.propellerRotation));
            propellerDown2.rotation.mul(Axis.YN.rotationDegrees(vehicle.propellerRotation));
            propellerDown3.rotation.mul(Axis.YN.rotationDegrees(vehicle.propellerRotation));
            propellerDown1.rotation.mul(Axis.YN.rotationDegrees(-vehicle.propellerRotation));
            propellerDown4.rotation.mul(Axis.YN.rotationDegrees(-vehicle.propellerRotation));
            propellerUp2.rotation.mul(Axis.YN.rotationDegrees(-vehicle.propellerRotation));
            propellerUp3.rotation.mul(Axis.YN.rotationDegrees(-vehicle.propellerRotation));

            float d = 16 * cableLength;
            ropeConnect.y -= d;
            BedrockCube cube = rope.getChildren().get(0).cubes.get(0);
            float scale = (cube.height() + cableLength) / cube.height();
            rope.yScale = scale;
            float diffY = (cube.y() * scale - cube.y()) * 1.8f;
            rope.y -= diffY;

            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            Vec3 root = new Vec3(0, 0, 0);
            pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.xRotO, vehicle.getXRot())), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, vehicle.zRotO, vehicle.getZRot())), (float) root.x, (float) root.y, (float) root.z);
            vehicle.lastRenderTime = System.currentTimeMillis();
            model.renderToBuffer(pPoseStack, bufferSource,
                    RenderType.entityCutout(display.getTexture()),
                    BedrockModelRenderTypes.polyMeshCutout(display.getTexture()),
                    vehicle.isDestroyed() ? 64 : pPackedLight,
                    OverlayTexture.pack(0f, false)
            );

            // 渲染部件
            vehicle.getPartUnits().forEach(partUnit -> partUnit.render(pPoseStack, bufferSource, pPackedLight));
            vehicle.getDecorationUnits().values().forEach(decorationUnit -> decorationUnit.render(pPoseStack, bufferSource, pPackedLight));

            Quaternionf reset = new Quaternionf(0, 0, 0, 1);
            propellerUp1.rotation.set(reset);
            propellerUp2.rotation.set(reset);
            propellerUp3.rotation.set(reset);
            propellerUp4.rotation.set(reset);
            propellerDown1.rotation.set(reset);
            propellerDown2.rotation.set(reset);
            propellerDown3.rotation.set(reset);
            propellerDown4.rotation.set(reset);
            ropeConnect.y += d;
            rope.yScale = 1;
            rope.y += diffY;
        }
        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Quadcopter pEntity) {
        return null;
    }

}
