package org.ywzj.vehicle.vehicle.part;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.Optional;

public class DecorationUnit extends PartUnit<PartUnitData> {

    public String baseBoneName = "";
    public float scale;
    public float selfXRot;
    public float selfYRot;
    public float selfZRot;
    public Vec3 offsetFromBone;
    public Vec3 offsetFromVehicle;
    public Quaternionf rotation;
    public boolean setting;

    public DecorationUnit(int index, AbstractVehicle vehicle, PartUnitData data) {
        super(index, vehicle, data);
        this.syncData = new PartUnitSyncData(this, 20);
        OBB obb = new OBB(vehicle.position().toVector3f(), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf());
        this.partCubeOBBs = new ArrayList<>();
        this.partCubeOBBs.add(new VehicleCubeOBB(obb));
    }

    public void update(DecorationAction message) {
        displayId = YwzjVehicle.resourceLocation(message.displayId);
        baseBoneName = message.baseBoneName;
        scale = message.scale;
        selfXRot = message.selfXRot;
        selfYRot = message.selfYRot;
        selfZRot = message.selfZRot;
        offsetFromBone = message.offsetFromBone;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
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
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
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
        var vehicleDisplay = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).orElse(null);
        if (vehicleDisplay == null) {
            return;
        }
        VehicleBedrockModel vehicleModel = vehicleDisplay.getModel();
        if (vehicleModel == null) {
            return;
        }
        BedrockBone bone = vehicleModel.getBoneMap().get(baseBoneName);
        if (bone == null) {
            return;
        }
        Quaternionf globalRotation = new Quaternionf(bone.rotation);
        globalRotation.rotateYXZ((float) Math.toRadians(-selfYRot),
                (float) Math.toRadians(selfXRot),
                (float) Math.toRadians(selfZRot));
        Vector3f offset = bone.rotation.transform(offsetFromBone.toVector3f());
        Vector3f globalPivot = new Vector3f(bone.x / 16.0F + offset.x, bone.y / 16.0F + offset.y, bone.z / 16.0F + offset.z);
        BedrockBone parent = bone.parent;
        while (parent != null) {
            parent.rotation.transform(globalPivot);
            globalPivot.add(parent.x / 16, parent.y / 16, parent.z / 16);
            globalRotation.premul(parent.rotation);
            parent = parent.parent;
        }
        Vector3f rot = new Vector3f();
        globalRotation.getEulerAnglesYXZ(rot);
        pPoseStack.pushPose();
        {
            pPoseStack.translate(globalPivot.x, globalPivot.y, globalPivot.z);
            pPoseStack.scale(scale, scale, scale);
            pPoseStack.rotateAround(Axis.YP.rotation(rot.y), 0, 0, 0);
            pPoseStack.rotateAround(Axis.XP.rotation(rot.x), 0, 0, 0);
            pPoseStack.rotateAround(Axis.ZP.rotation(rot.z), 0, 0, 0);
            offsetFromVehicle = new Vec3(globalPivot);
            rotation = globalRotation;
            decorationModel.renderToBuffer(pPoseStack, bufferSource, decorationTexture, vehicle.isDestroyed() ? 64 : pPackedLight);
            decorationModel.renderSpecialBones(pPoseStack, bufferSource, vehicle.isDestroyed() ? 64 : pPackedLight, OverlayTexture.NO_OVERLAY);
            if (setting) {
                RenderHelper.renderArrow3D(pPoseStack, bufferSource, 0.15f, 0.3f, 0, 255, 0, 255);
            }
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
