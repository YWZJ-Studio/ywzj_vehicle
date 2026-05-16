package org.ywzj.vehicle.client.render.entity.block;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.resource.BedrockModelLoader;

import java.util.HashSet;
import java.util.List;

public class MachineMaxBlockRenderer implements BlockEntityRenderer<MachineMaxBlockEntity> {

    public static final ResourceLocation MACHINE_MAX_BLOCK_MODEL = YwzjVehicle.modLocation("block/machine_max_block");
    public static final ResourceLocation MACHINE_MAX_BLOCK_TEXTURE = YwzjVehicle.modLocation("textures/block/machine_max_block.png");

    public MachineMaxBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MachineMaxBlockEntity machineMaxBlockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.translate(0.5, 0, 0.5);
        float yRot;
        Direction facing = machineMaxBlockEntity.getBlockState().getValue(FigureBoxBlock.FACING);
        switch (facing) {
            case NORTH -> yRot = 0;
            case SOUTH -> yRot = 180f;
            case WEST -> yRot = 90f;
            case EAST -> yRot = 270f;
            default -> yRot = 0f;
        }
        poseStack.rotateAround(Axis.YP.rotationDegrees(yRot), 0, 0, 0);
        BedrockModel machineMaxBlockModel = BedrockModelLoader.getModel(MACHINE_MAX_BLOCK_MODEL);
        BedrockBone boneX = machineMaxBlockModel.getBone("X");
        BedrockBone boneY = machineMaxBlockModel.getBone("Y");
        BedrockBone boneZ = machineMaxBlockModel.getBone("Z");
        float xo = boneX.x;
        float yo = boneY.y;
        float zo = boneZ.z;
        float dy = 0;
        if (machineMaxBlockEntity.craftingVehicleId != null && machineMaxBlockEntity.vehicleData != null) {
            double length = machineMaxBlockEntity.vehicleData.getStructureLength();
            float scale = (float) (1 / Math.max(length, 4) * 0.7);
            BedrockBoneWrapper printingBoneWrapper = machineMaxBlockEntity.printingBoneWrapper;
            // 打印机三轴
            if (printingBoneWrapper != null) {
                float r = scale * 1280;
                boneX.x = (float) (boneX.x - 3f + printingBoneWrapper.x / r);
                boneZ.z = (float) (boneZ.z - 3f + printingBoneWrapper.z / r);
                dy = (float) (printingBoneWrapper.y / r);
                boneY.y = boneY.y + 4.9f - dy;
            }
            machineMaxBlockModel.renderToBuffer(poseStack, bufferSource,
                    RenderType.entityCutout(MACHINE_MAX_BLOCK_TEXTURE),
                    BedrockModelRenderTypes.polyMeshCutout(MACHINE_MAX_BLOCK_TEXTURE),
                    packedLight,
                    OverlayTexture.pack(0f, false)
            );
            boneX.x = xo;
            boneY.y = yo;
            boneZ.z = zo;
            // 载具模型
            poseStack.pushPose();
            {
                poseStack.translate(0,  (float) 9 / 16 - dy / 16, 0);
                poseStack.scale(scale, scale, scale);
                Vec3 root = new Vec3(0, 0, 0);
                poseStack.rotateAround(Axis.YP.rotationDegrees(135), (float) root.x, (float) root.y, (float) root.z);
                machineMaxBlockEntity.bedrockBoneWrappers.forEach(bedrockBoneWrapper -> bedrockBoneWrapper.bone.visible = bedrockBoneWrapper.visible);
                VehicleBedrockModel vehicleModel = machineMaxBlockEntity.vehicleDisplay.getModel();
                vehicleModel.applyPose(vehicleModel.getBindPose());
                vehicleModel.renderToBuffer(poseStack, bufferSource, machineMaxBlockEntity.vehicleDisplay.getTexture(), packedLight);
                vehicleModel.renderSpecialBones(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
                machineMaxBlockEntity.bedrockBoneWrappers.forEach(bedrockBoneWrapper -> bedrockBoneWrapper.bone.visible = true);
            }
            poseStack.popPose();
        } else {
            machineMaxBlockModel.renderToBuffer(poseStack, bufferSource,
                    RenderType.entityCutout(MACHINE_MAX_BLOCK_TEXTURE),
                    BedrockModelRenderTypes.polyMeshCutout(MACHINE_MAX_BLOCK_TEXTURE),
                    packedLight,
                    OverlayTexture.pack(0f, false)
            );
        }
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

        public BedrockBone bone;
        public boolean visible;
        public double x;
        public double y;
        public double z;
        public BedrockBoneWrapper parentWrapper;

        public BedrockBoneWrapper(BedrockBone bone, BedrockBoneWrapper parentWrapper) {
            this.bone = bone;
            Vector3f globalPivot = new Vector3f(bone.x, bone.y, bone.z);
            if (!bone.cubes.isEmpty()) {
                Vector3f centerOffset = new Vector3f();
                double totalX = 0;
                double totalY = 0;
                double totalZ = 0;
                int count = bone.cubes.size();
                for (BedrockCube cube : bone.cubes) {
                    totalX += cube.x() * 16 + cube.width() * 16 / 2.0;
                    totalY += cube.y() * 16 + cube.height() * 16 / 2.0;
                    totalZ += cube.z() * 16 + cube.depth() * 16 / 2.0;
                }
                centerOffset.x += (float) (totalX / count);
                centerOffset.y += (float) (totalY / count);
                centerOffset.z += (float) (totalZ / count);
                bone.rotation.transform(centerOffset);
                globalPivot.add(centerOffset);
            }
            BedrockBone parent = bone.parent;
            while (parent != null) {
                parent.rotation.transform(globalPivot);
                globalPivot.add(parent.x, parent.y, parent.z);
                parent = parent.parent;
            }
            Quaternionf rotation = new Quaternionf();
            rotation.rotateY((float) Math.toRadians(135));
            rotation.transform(globalPivot);
            this.x = globalPivot.x;
            this.y = globalPivot.y;
            this.z = globalPivot.z;
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
