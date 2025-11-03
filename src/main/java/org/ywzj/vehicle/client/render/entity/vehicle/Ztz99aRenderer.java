package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockAnimationReloadListenerEvent;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllVehicles;
import org.ywzj.vehicle.client.render.animation.TrackAnimationInstance;
import org.ywzj.vehicle.entity.vehicle.Ztz99a;
import org.ywzj.vehicle.resource.BedrockModelLoader;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Ztz99aRenderer extends EntityRenderer<Ztz99a> {
    private static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    public Ztz99aRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    private static List<BedrockAnimation> animations;

    @SubscribeEvent
    public static void onRegisterAnimationReloadListener(RegisterBedrockAnimationReloadListenerEvent event) {
        event.register(map -> {
            animations = map.get(YwzjVehicle.modLoc("bedrock/entity/ztz99a.animation"));
        });
    }

    @Override
    public void render(Ztz99a pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();

        Vec3 root = new Vec3(0, 0, 0);

        pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        pPoseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.zRotO, pEntity.getZRot())), (float) root.x, (float) root.y, (float) root.z);

        BedrockModel model = BedrockModelLoader.getModel(AllVehicles.ZTZ99A.getVisualBedrockModel());
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(AllVehicles.ZTZ99A.getVisualBedrockTexture()));

        TrackAnimationInstance instance = pEntity.getTrackAnimationInstance();
        if (instance == null) {
            instance = new TrackAnimationInstance(animations);
            pEntity.setTrackAnimationInstance(instance);
        }

        float deltaTime = (System.currentTimeMillis() - pEntity.lastRenderTime) / 1000f;

        float vf = pEntity.getEntityData().get(Ztz99a.FORWARD_SPEED); // 线速度
        float omega = pEntity.getEntityData().get(Ztz99a.TURN_SPEED); // 角速度
        float trackWidth = 3.0f / 20f;

        float leftTrackSpeed = vf + omega * trackWidth / 2;
        float rightTrackSpeed = vf - omega * trackWidth / 2;

        leftTrackSpeed *= 20f; // 转换为米/秒
        rightTrackSpeed *= 20f; // 转换为米/秒

        instance.advanceProgress(leftTrackSpeed, rightTrackSpeed, deltaTime, 0.25f);

        Pose bindPose = model.getBindPose();
        Pose blended = BLENDER.blend(bindPose, instance.evaluate());
        model.applyPose(blended);

        for (int i = 0; i < 13; i++) {
            String boneName = "hull_big_" + i;
            BedrockBone bone = model.getBoneMap().get(boneName);
            if (bone != null) {
                float angle = i < 7 ? instance.leftWheelDegrees(0.375f) : instance.rightWheelDegrees(0.375f);
                bone.rotation.mul(Axis.XP.rotationDegrees(angle));
            }
        }

        for (int i = 0; i < 5; i++) {
            String boneName = "hull_small_" + i;
            BedrockBone bone = model.getBoneMap().get(boneName);
            if (bone != null) {
                float angle = i < 3 ? instance.leftWheelDegrees(0.28f) : instance.rightWheelDegrees(0.28f);
                bone.rotation.mul(Axis.XP.rotationDegrees(angle));
            }
        }

        BedrockBone turret = model.getBoneMap().get("turret");
        BedrockBone cannon = model.getBoneMap().get("canno");
        BedrockBone machineGunBase = model.getBoneMap().get("machine_gun");
        BedrockBone machineGun = model.getBoneMap().get("machine_gun_high");

        // 炮塔旋转
        float turretYRot = 0;
        // 炮塔俯仰
        float turretXRot = 0;
        if (!pEntity.seats.isEmpty()) {
            PartUnit<?> partUnit = pEntity.seats.get(0).partUnit;
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
            PartUnit<?> partUnit = pEntity.seats.get(1).partUnit;
            if (partUnit instanceof WeaponUnit weaponUnit) {
                machineGunYRot = Mth.rotLerp(pPartialTick, weaponUnit.yRotO, weaponUnit.getYRot());
                machineGunXRot = Mth.rotLerp(pPartialTick, weaponUnit.xRotO, weaponUnit.getXRot());
            }
        }


        turret.rotation.mul(Axis.YN.rotationDegrees(turretYRot));
        cannon.rotation.mul(Axis.XN.rotationDegrees(-turretXRot));
        machineGunBase.rotation.mul(Axis.YN.rotationDegrees(machineGunYRot));
        machineGun.rotation.mul(Axis.XN.rotationDegrees(-machineGunXRot));

        pEntity.lastRenderTime = System.currentTimeMillis();
        model.renderToBuffer(pPoseStack, builder, pPackedLight, OverlayTexture.NO_OVERLAY);

        model.applyPose(model.getBindPose());
        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Ztz99a pEntity) {
        return null;
    }

}
