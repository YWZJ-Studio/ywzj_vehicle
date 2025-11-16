package org.ywzj.vehicle.client.render.entity.template;

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
import org.mozilla.javascript.ContextFactory;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.CommonWheeledVehicle;


// todo 测试用
public class CommonWheeledVehicleRender extends EntityRenderer<CommonWheeledVehicle> {
    private static final Object[] EMPTY_ARGS = new Object[0];

    public CommonWheeledVehicleRender(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(CommonWheeledVehicle pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        ResourceLocation displayId = pEntity.getCustomDisplayId();
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(displayId).orElse(null);
        if (display == null) {
            return;
        }

        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        BedrockModel model = display.getModel();
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(display.getTexture()));

        if (model != null) {
            display.getVehicleContext().update(pPartialTick, pEntity);
            var func = display.getPrepareBonesFunction();
            if (func != null) {
                try (var ctx = ContextFactory.getGlobal().enterContext()) {
                    func.call(ctx, display.getScope(), func, EMPTY_ARGS);
                }
            }

            pEntity.lastRenderTime = System.currentTimeMillis();
            model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);
            model.applyPose(model.getBindPose());
        }

        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CommonWheeledVehicle pEntity) {
        return null;
    }

}
