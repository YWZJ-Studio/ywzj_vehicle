package org.ywzj.vehicle.client.render.entity.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.ywzj.vehicle.all.AllKeys;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.client.particle.BulletHoleParticle;
import org.ywzj.vehicle.client.render.animation.VehicleAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.EntityContext;
import org.ywzj.vehicle.client.render.util.OBBRenderer;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.client.resource.vehicle.VehicleDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VehicleRender<T extends AbstractVehicle> extends EntityRenderer<T> {

    public VehicleRender(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(T vehicle, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        renderHitbox(vehicle, pPoseStack, bufferSource);
        VehicleDisplay<?, ?> display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).orElse(null);
        if (display == null) {
            return;
        }
        boolean isCabinView = LocalVehiclePlayer.instance.vehicle == vehicle
                && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR
                && display.getCabinDisplay() != null;
        VehicleBedrockModel model = isCabinView ? display.getCabinDisplay().getModel() : display.getModel();
        ResourceLocation texture = isCabinView ? display.getCabinDisplay().getTexture() : display.getTexture();
        if (model == null || texture == null) {
            return;
        }
        if (!model.hasBakedModel()) {
            return;
        }
        BakedModelInstance modelInstance = isCabinView ? vehicle.getCabinModelInstance() : vehicle.getVehicleModelInstance();
        if (modelInstance == null) {
            modelInstance = model.getDefaultModelInstance();
        }
        int modelLight = pPackedLight;
        if (vehicle.isDestroyed()) {
            int blockLight = (int) (LightTexture.block(pPackedLight) / 1.5f);
            int skyLight = (int) (LightTexture.sky(pPackedLight) / 1.5f);
            modelLight = LightTexture.pack(blockLight, skyLight);
        }
        pPoseStack.pushPose();
        try {
            super.render(vehicle, pEntityYaw, pPartialTick, pPoseStack, bufferSource, pPackedLight);
            List<BoneState> invisibleBones = applyDetachedPart(vehicle, modelInstance);
            applyVehicleRotation(vehicle, pPartialTick, pPoseStack);
            applyAnimationPose(vehicle, pPartialTick, modelInstance, isCabinView);
            // 载具
            model.renderToBuffer(modelInstance, pPoseStack, bufferSource, texture, modelLight);
            model.renderSpecialBones(modelInstance, pPoseStack, bufferSource, texture, modelLight, OverlayTexture.NO_OVERLAY, invisibleBones, vehicle == LocalVehiclePlayer.instance.vehicle);
            // 部件
            for (PartUnit<?> partUnit : vehicle.getPartUnits()) {
                if (partUnit.isDetached()) {
                    continue;
                }
                partUnit.render(pPoseStack, bufferSource, pPackedLight);
            }
            // 饰品
            for (DecorationUnit decorationUnit : vehicle.getDecorationUnits().values()) {
                decorationUnit.render(pPoseStack, bufferSource, pPackedLight);
            }
            // 弹孔
            for (BulletHoleParticle bulletHoleParticle : vehicle.getBulletHoleParticles()) {
                bulletHoleParticle.renderOnVehicle(pPartialTick, pPoseStack, bufferSource, modelInstance);
            }
            vehicle.lastRenderTime = System.currentTimeMillis();
        } finally {
            pPoseStack.popPose();
        }
    }

    public static void renderHitbox(AbstractVehicle vehicle, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        boolean renderSelfVehicleHitbox = vehicle == LocalVehiclePlayer.instance.vehicle && AllKeys.INSPECT_VEHICLE.isDown();
        List<PartUnit<?>> partUnits = new ArrayList<>();
        if (dispatcher.shouldRenderHitBoxes() || renderSelfVehicleHitbox) {
            // 物理框
            if (vehicle.getMainCubeOBB() != null) {
                OBBRenderer.INSTANCE.render(vehicle.position(),
                        List.of(vehicle.getMainCubeOBB().obb()),
                        poseStack, buffer, 0, 0, 1, 1);
            }
            // 车体框
            for (VehicleCubeOBB vehicleCubeOBB : vehicle.getVehicleCubeOBBs()) {
                OBBRenderer.INSTANCE.render(vehicle.position(),
                        List.of(vehicleCubeOBB.obb()),
                        poseStack, buffer, 0, 1, 0, 1);
            }
            if (vehicle instanceof FixedWingVehicle fixedWingVehicle
                    && fixedWingVehicle.aerodynamicCubeOBB != null) {
                // 气动框
                OBBRenderer.INSTANCE.render(vehicle.position(),
                        List.of(fixedWingVehicle.aerodynamicCubeOBB.obb()),
                        poseStack, buffer, 0, 1, 1, 1);
            }
            partUnits.addAll(vehicle.getPartUnits());
            partUnits.addAll(vehicle.getDecorationUnits().values());
        } else {
            PartUnit<?> partUnit = LocalVehiclePlayer.instance.lookAtPartUnit;
            if (AllKeys.INSPECT_VEHICLE.isDown() && partUnit != null) {
                partUnits.add(partUnit);
            }
        }
        for (PartUnit<?> partUnit : partUnits) {
            if (partUnit.isDetached()) {
                continue;
            }
            if (partUnit instanceof WeaponUnit weaponUnit) {
                // 武器框
                OBBRenderer.INSTANCE.render(vehicle.position(), weaponUnit.getOBBs(),
                        poseStack, buffer, 1, 0, 0, 1);
            } else if (partUnit instanceof DecorationUnit decorationUnit) {
                // 装饰品框
                OBBRenderer.INSTANCE.render(vehicle.position(), decorationUnit.getOBBs(),
                        poseStack, buffer, 1, 0, 1, 1);
            } else {
                // 其他部件框
                OBBRenderer.INSTANCE.render(vehicle.position(), partUnit.getOBBs(),
                        poseStack, buffer, 1, 1, 0, 1);
            }
        }
    }

    public static List<BoneState> applyDetachedPart(AbstractVehicle vehicle, BakedModelInstance modelInstance) {
        List<BoneState> invisibleBones = new ArrayList<>();
        for (PartUnit<?> partUnit : vehicle.getPartUnits()) {
            String renderBoneName = partUnit.getRenderBoneName();
            if (renderBoneName == null) {
                continue;
            }
            BoneState bone = modelInstance.getBone(renderBoneName);
            if (bone != null) {
                bone.visible = !partUnit.isDetached();
                if (!bone.visible) {
                    invisibleBones.add(bone);
                }
            }
        }
        return invisibleBones;
    }

    public static void applyVehicleRotation(AbstractVehicle vehicle, float partialTick, PoseStack poseStack) {
        Vec3 root = vehicle.centerOffset;
        Quaternionf rot = vehicle.rotYXZ(partialTick);
        poseStack.rotateAround(rot, (float) root.x, (float) root.y, (float) root.z);
    }

    public static void applyAnimationPose(AbstractVehicle vehicle, float partialTick, BakedModelInstance modelInstance, boolean isCabinView) {
        if (vehicle instanceof IAnimationEntity<?, ?> animationEntity) {
            var animationInstance = animationEntity.getAnimationInstance();
            if (animationInstance instanceof VehicleAnimationInstance<? extends EntityContext<?>> vehicleAnimationInstance) {
                if (isCabinView) {
                    animationInstance = vehicleAnimationInstance.getCabinAnimationInstance();
                }
                animationInstance.getContext().setPartialTick(partialTick);
                animationInstance.tick();
                modelInstance.applyPose(BLENDER.blend(modelInstance.getBindPose(), animationInstance.getCurrentPose()));
            }
        }
    }

    @SubscribeEvent
    public static void onFire(VehicleFireEvent.Post event) {
        if (!event.isClientSide()) {
            return;
        }
        AbstractVehicle vehicle = event.getVehicle();
        VehicleDisplay<?, ?> display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).orElse(null);
        if (display == null) {
            return;
        }
        if (vehicle instanceof IAnimationEntity<?, ?> animationEntity) {
            if (animationEntity.getAnimationInstance() instanceof VehicleAnimationInstance<? extends EntityContext<?>> vehicleAnimationInstance) {
                String partId = event.getWeapon().getWeaponUnit().getId();
                boolean isCabinView = LocalVehiclePlayer.instance.vehicle == vehicle
                        && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR
                        && display.getCabinDisplay() != null;
                if (isCabinView) {
                    var cabinAnimationInstance = vehicleAnimationInstance.getCabinAnimationInstance();
                    if (cabinAnimationInstance != null) {
                        cabinAnimationInstance.getContext().offerEvent(partId + "_fire");
                    }
                } else {
                    vehicleAnimationInstance.getContext().offerEvent(partId + "_fire");
                }
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(@NotNull T pEntity) {
        return null;
    }

}
