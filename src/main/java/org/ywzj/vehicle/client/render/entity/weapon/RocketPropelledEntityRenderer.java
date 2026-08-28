package org.ywzj.vehicle.client.render.entity.weapon;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.InternalAssets;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.entity.weapon.AmmoEntity;
import org.ywzj.vehicle.entity.weapon.MissileEntity;
import org.ywzj.vehicle.entity.weapon.RocketEntity;

import java.util.function.Function;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class RocketPropelledEntityRenderer<T extends AmmoEntity> extends AmmoEntityRenderer<T> {

    public RocketPropelledEntityRenderer(EntityRendererProvider.Context context,
                                         Function<T, ResourceLocation> weaponIdGetter,
                                         ResourceLocation defaultModel,
                                         ResourceLocation defaultTexture) {
        super(context, weaponIdGetter, defaultModel, defaultTexture);
    }

    @Override
    public void render(T ammoEntity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(ammoEntity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        Vec3 nozzleOffset;
        AnimationRunner animationRunner;
        if (ammoEntity instanceof RocketEntity rocket) {
            if (!rocket.isMotorBurning()) {
                return;
            }
            nozzleOffset = rocket.engineNozzleOffset;
            animationRunner = rocket.animationRunner;
        } else if (ammoEntity instanceof MissileEntity missile) {
            if (!missile.isMotorBurning()) {
                return;
            }
            nozzleOffset = missile.engineNozzleOffset;
            animationRunner = missile.animationRunner;
        } else {
            return;
        }
        if (nozzleOffset == null || animationRunner == null) {
            return;
        }
        InternalAssets assets = ClientAssetsManager.INSTANCE.getInternalAssets();
        VehicleBedrockModel flameModel = assets.getRocketMotorFlameModel();
        animationRunner.tick();
        flameModel.applyPose(BLENDER.blend(flameModel.getBindPose(), animationRunner.evaluate()));
        poseStack.pushPose();
        try {
            Vec3 root = Vec3.ZERO;
            poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw),
                    (float) root.x, (float) root.y, (float) root.z);
            poseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(partialTick, ammoEntity.xRotO, ammoEntity.getXRot())),
                    (float) root.x, (float) root.y, (float) root.z);
            poseStack.translate(nozzleOffset.x, nozzleOffset.y, nozzleOffset.z);
            float scale = ammoEntity.getCaliber() / 1000;
            poseStack.scale(scale, scale, scale);
            flameModel.renderToBuffer(poseStack, bufferSource,
                    RenderType.entityTranslucent(InternalAssets.ROCKET_MOTOR_FLAME_TEXTURE),
                    BedrockModelRenderTypes.polyMeshCutout(InternalAssets.ROCKET_MOTOR_FLAME_TEXTURE),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        } finally {
            poseStack.popPose();
            flameModel.applyPose(flameModel.getBindPose());
        }
    }

}
