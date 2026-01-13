package org.ywzj.vehicle.client.render.entity.block;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;

import java.util.HashSet;
import java.util.List;

public class MachineMaxBlockRenderer implements BlockEntityRenderer<MachineMaxBlockEntity> {

    public MachineMaxBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MachineMaxBlockEntity machineMaxBlockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (machineMaxBlockEntity.craftingCustomId == null) {
            return;
        }
        machineMaxBlockEntity.bedrockBoneWrappers.forEach(bedrockBoneWrapper -> bedrockBoneWrapper.bedrockBone.visible = bedrockBoneWrapper.visible);
        double length = machineMaxBlockEntity.vehicleData.getStructureLength();
        float scale = (float) (1 / Math.max(length, 3) * 0.7);
        poseStack.pushPose();
        {
            poseStack.translate(0.5, 1.15, 0.5);
            poseStack.scale(scale, scale, scale);
            Vec3 root = new Vec3(0, 0, 0);
            poseStack.rotateAround(Axis.YP.rotationDegrees(45), (float) root.x, (float) root.y, (float) root.z);
            VertexConsumer builder = bufferSource.getBuffer(RenderType.entityCutout(machineMaxBlockEntity.vehicleDisplay.getTexture()));
            machineMaxBlockEntity.vehicleDisplay.getModel().renderToBuffer(poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
        machineMaxBlockEntity.bedrockBoneWrappers.forEach(bedrockBoneWrapper -> bedrockBoneWrapper.bedrockBone.visible = true);
    }

    public static void buildBedrockBoneWrappers(BedrockBone bone, HashSet<BedrockBone> bones, List<BedrockBoneWrapper> bedrockBoneWrappers, BedrockBoneWrapper parentWrapper) {
        if (!bones.contains(bone)) {
            bones.add(bone);
            BedrockBoneWrapper bedrockBoneWrapper = new BedrockBoneWrapper(bone, parentWrapper);
            bedrockBoneWrappers.add(bedrockBoneWrapper);
            for (BedrockBone child : bone.getChildren()) {
                buildBedrockBoneWrappers(child, bones, bedrockBoneWrappers, bedrockBoneWrapper);
            }
        }
    }

    public static class BedrockBoneWrapper {

        public BedrockBone bedrockBone;
        public boolean visible;
        public double x;
        public double y;
        public double z;
        public BedrockBoneWrapper parentWrapper;

        public BedrockBoneWrapper(BedrockBone bedrockBone, BedrockBoneWrapper parentWrapper) {
            this.bedrockBone = bedrockBone;
            this.x = bedrockBone.x;
            this.y = bedrockBone.y;
            this.z = bedrockBone.z;
            BedrockBone parent = bedrockBone.parent;
            while (parent != null) {
                this.x += parent.x;
                this.y += parent.y;
                this.z += parent.z;
                parent = parent.parent;
            }
            this.parentWrapper = parentWrapper;
        }

        public void appear() {
            this.visible = true;
            BedrockBoneWrapper parentWrapper = this.parentWrapper;
            while (parentWrapper != null) {
                parentWrapper.appear();
                parentWrapper = parentWrapper.parentWrapper;
            }
        }

    }

}
