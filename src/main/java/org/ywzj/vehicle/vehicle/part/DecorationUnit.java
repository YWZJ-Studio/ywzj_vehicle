package org.ywzj.vehicle.vehicle.part;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.AnimationRateLimiter;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.LoopingState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.client.screen.DecorationSettingsScreen;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.sync.PartUnitSyncData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.pojo.DecorationAction;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class DecorationUnit extends PartUnit<PartUnitData> {

    public String baseBoneName = "";
    public float scale;
    public float selfXRot;
    public float selfYRot;
    public float selfZRot;
    public Vec3 offsetFromBone;
    public Vec3 offsetFromVehicle;
    public Quaternionf rotation;
    private BakedModelInstance modelInstance;
    private AnimationRateLimiter<Pose> animationRateLimiter;
    public Pose lastPose;
    private AnimationRunner animationRunner;

    public DecorationUnit(int index, AbstractVehicle vehicle, PartUnitData data) {
        super(index, vehicle, data);
        this.syncData = new PartUnitSyncData(this, 20);
        OBB obb = new OBB(vehicle.position().toVector3f(), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf());
        this.partCubeOBBs = new ArrayList<>();
        this.partCubeOBBs.add(new VehicleCubeOBB(obb));
    }

    private void initAnimation() {
        if (vehicle.level().isClientSide()) {
            var decorationDisplayOptional = ClientAssetsManager.INSTANCE.getDecorationDisplay(displayId);
            if (decorationDisplayOptional.isPresent()) {
                BaseDisplay decorationDisplay = decorationDisplayOptional.get();
                if (decorationDisplay.getModel() != null && decorationDisplay.getModel().hasBakedModel()) {
                    modelInstance = decorationDisplay.getModel().createBakedInstance();
                    animationRateLimiter = new AnimationRateLimiter<>(AnimationClocks.client(), () -> {
                        double distanceSqr = this.vehicle.distanceToSqr(LocalVehiclePlayer.instance.getPlayer());
                        if (distanceSqr < 64 * 64) {
                            return AnimationRateLimiter.FPS_120;
                        } else if (distanceSqr < 128 * 128) {
                            return AnimationRateLimiter.FPS_60;
                        } else {
                            return AnimationRateLimiter.FPS_30;
                        }
                    });
                }
                Map<String, BedrockAnimation> animations = decorationDisplay.getAnimations();
                if (!animations.isEmpty()) {
                    BedrockAnimation animation = animations.values().iterator().next();
                    AnimationContext animContext = new AnimationContext(animation.getSpecifiedEndTimeS());
                    animationRunner = new AnimationRunner(animation, animContext);
                    animationRunner.setState(new LoopingState(System::nanoTime));
                }
            }
        }
    }

    public void update(DecorationAction message) {
        displayId = YwzjVehicle.resourceLocation(message.displayId);
        baseBoneName = message.baseBoneName;
        scale = message.scale;
        selfXRot = message.selfXRot;
        selfYRot = message.selfYRot;
        selfZRot = message.selfZRot;
        offsetFromBone = message.offsetFromBone;
        initAnimation();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = super.serializeNBT(provider);
        if (displayId != null) {
            tag.putString("decorationDisplayId", displayId.toString());
        }
        if (baseBoneName != null) {
            tag.putString("baseBoneName", baseBoneName);
        }
        tag.putFloat("x", (float) offsetFromBone.x);
        tag.putFloat("y", (float) offsetFromBone.y);
        tag.putFloat("z", (float) offsetFromBone.z);
        tag.putFloat("scale", scale);
        tag.putFloat("selfXRot", selfXRot);
        tag.putFloat("selfYRot", selfYRot);
        tag.putFloat("selfZRot", selfZRot);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        super.deserializeNBT(provider, nbt);
        if (nbt.contains("decorationDisplayId")) {
            displayId = YwzjVehicle.resourceLocation(nbt.getString("decorationDisplayId"));
        }
        if (nbt.contains("baseBoneName")) {
            baseBoneName = nbt.getString("baseBoneName");
        }
        offsetFromBone = new Vec3(nbt.getFloat("x"), nbt.getFloat("y"), nbt.getFloat("z"));
        scale = nbt.getFloat("scale");
        selfXRot = nbt.getFloat("selfXRot");
        selfYRot = nbt.getFloat("selfYRot");
        selfZRot = nbt.getFloat("selfZRot");
        initAnimation();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        Optional<BaseDisplay> decorationDisplayOptional = ClientAssetsManager.INSTANCE.getDecorationDisplay(displayId);
        if (decorationDisplayOptional.isEmpty()) {
            return;
        }
        BaseDisplay decorationDisplay = decorationDisplayOptional.get();
        VehicleBedrockModel decorationModel = decorationDisplay.getModel();
        ResourceLocation decorationTexture = decorationDisplay.getTexture();
        if (decorationModel == null || decorationTexture == null) {
            return;
        }
        if (!decorationModel.hasBakedModel()) {
            return;
        }
        var vehicleDisplay = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).orElse(null);
        if (vehicleDisplay == null) {
            return;
        }
        VehicleBedrockModel vehicleModel = vehicleDisplay.getModel();
        if (vehicleModel == null) {
            return;
        }
        if (!vehicleModel.hasBakedModel()) {
            return;
        }
        boolean rootAttachment = baseBoneName == null || baseBoneName.isBlank();
        BakedModelInstance vehicleModelInstance = vehicle.getModelInstance();
        int attachmentBoneIndex = rootAttachment ? -1 : vehicleModelInstance.getIndex(baseBoneName);
        if (!rootAttachment && vehicleModelInstance.getBone(attachmentBoneIndex) == null) {
            return;
        }
        Matrix4f attachmentTransform = attachmentBoneIndex < 0
                ? new Matrix4f()
                : vehicleModelInstance.getGlobalTransform(attachmentBoneIndex);
        Quaternionf localRotation = new Quaternionf().rotateYXZ((float) Math.toRadians(-selfYRot),
                (float) Math.toRadians(selfXRot),
                (float) Math.toRadians(selfZRot));
        Quaternionf globalRotation = attachmentTransform.getUnnormalizedRotation(new Quaternionf())
                .normalize()
                .mul(localRotation);
        Vector3f globalPivot = offsetFromBone.toVector3f().mulPosition(attachmentTransform);
        Vector3f localEulerRotation = new Vector3f();
        localRotation.getEulerAnglesYXZ(localEulerRotation);
        pPoseStack.pushPose();
        {
            if (attachmentBoneIndex >= 0) {
                vehicleModelInstance.mulGlobalTransform(pPoseStack, attachmentBoneIndex);
            }
            pPoseStack.translate(offsetFromBone.x, offsetFromBone.y, offsetFromBone.z);
            pPoseStack.scale(scale, scale, scale);
            pPoseStack.rotateAround(Axis.YP.rotation(localEulerRotation.y), 0, 0, 0);
            pPoseStack.rotateAround(Axis.XP.rotation(localEulerRotation.x), 0, 0, 0);
            pPoseStack.rotateAround(Axis.ZP.rotation(localEulerRotation.z), 0, 0, 0);
            offsetFromVehicle = new Vec3(globalPivot);
            rotation = globalRotation;
            if (animationRunner != null) {
                animationRunner.tick();
                Pose pose = animationRateLimiter.update(() -> BLENDER.blend(modelInstance.getBindPose(), animationRunner.evaluate()));
                if (pose != lastPose) {
                    modelInstance.applyPose(pose);
                    lastPose = pose;
                }
            }
            decorationModel.renderToBuffer(modelInstance, pPoseStack, bufferSource, decorationTexture, vehicle.isDestroyed() ? 64 : pPackedLight);
            decorationModel.renderSpecialBones(modelInstance, pPoseStack, bufferSource, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY, null, false);
        }
        pPoseStack.popPose();
    }

    @Override
    public void tick() {
        super.tick();
        OBB obb = partCubeOBBs.get(0).obb();
        if (offsetFromVehicle != null && rotation != null) {
            obb.setCenter(vehicle.relativeRotPos(vehicle.position().add(offsetFromVehicle), false).toVector3f());
            obb.setRotation(vehicle.rotYXZ().mul(rotation));
        } else {
            obb.setCenter(vehicle.position().toVector3f());
        }
    }

    public boolean onInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide() && hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown()) {
            openScreen();
        }
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void openScreen() {
        Minecraft.getInstance().setScreen(new DecorationSettingsScreen(this));
    }

}
