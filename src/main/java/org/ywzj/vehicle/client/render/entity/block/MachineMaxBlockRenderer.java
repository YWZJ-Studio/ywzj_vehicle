package org.ywzj.vehicle.client.render.entity.block;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.ICube;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBoneDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeGeometryWriter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.MachineMaxBlockEntity;
import org.ywzj.vehicle.resource.BedrockModelLoader;

public class MachineMaxBlockRenderer implements BlockEntityRenderer<MachineMaxBlockEntity> {

    public static final ResourceLocation MACHINE_MAX_BLOCK_MODEL = YwzjVehicle.modLocation("block/machine_max_block");
    public static final ResourceLocation MACHINE_MAX_BLOCK_TEXTURE = YwzjVehicle.modLocation("textures/block/machine_max_block.png");
    private static final float VEHICLE_MODEL_YAW = 135.0F;
    private static final float PRINTER_MODEL_UNITS_PER_BLOCK = 80.0F;

    public MachineMaxBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MachineMaxBlockEntity machineMaxBlockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
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

        boolean hasPrintingPreview = machineMaxBlockEntity.hasPrintingPreview()
                && machineMaxBlockEntity.vehicleData != null
                && machineMaxBlockEntity.vehicleDisplay != null
                && machineMaxBlockEntity.vehicleDisplay.getTexture() != null
                && machineMaxBlockEntity.printingModel != null
                && machineMaxBlockEntity.printingModelInstance != null;
        float scale = 1;
        if (hasPrintingPreview) {
            double length = machineMaxBlockEntity.vehicleData.getStructureLength();
            scale = (float) (1 / Math.max(length, 4) * 0.7);
            MachineMaxBlockEntity.PrintingCube currentCube = machineMaxBlockEntity.getCurrentPrintingCube();
            if (currentCube != null) {
                Vector3f headPosition = new Vector3f(
                        (float) currentCube.modelCenter().x,
                        (float) currentCube.modelCenter().y,
                        (float) currentCube.modelCenter().z
                ).rotateY((float) Math.toRadians(VEHICLE_MODEL_YAW));
                float printerScale = scale * PRINTER_MODEL_UNITS_PER_BLOCK;
                boneX.x = boneX.x - 3F + headPosition.x / printerScale;
                boneZ.z = boneZ.z - 3F + headPosition.z / printerScale;
                dy = headPosition.y / printerScale;
                boneY.y = boneY.y + 4.9F - dy;
            }
        }

        try {
            machineMaxBlockModel.renderToBuffer(poseStack, bufferSource,
                    RenderType.entityCutout(MACHINE_MAX_BLOCK_TEXTURE),
                    BedrockModelRenderTypes.polyMeshCutout(MACHINE_MAX_BLOCK_TEXTURE),
                    packedLight,
                    OverlayTexture.pack(0f, false)
            );
        } finally {
            boneX.x = xo;
            boneY.y = yo;
            boneZ.z = zo;
        }

        if (!hasPrintingPreview) {
            return;
        }

        poseStack.pushPose();
        try {
            poseStack.translate(0, (float) 9 / 16 - dy / 16, 0);
            poseStack.scale(scale, scale, scale);
            poseStack.rotateAround(Axis.YP.rotationDegrees(VEHICLE_MODEL_YAW), 0, 0, 0);
            renderStaticPrintingCubes(machineMaxBlockEntity, poseStack, bufferSource, packedLight);
            if (machineMaxBlockEntity.isPrintingPolyMeshStage()) {
                renderPrintingPolyMeshes(machineMaxBlockEntity, poseStack, bufferSource, packedLight);
            }
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * 每个 cube 已在方块实体准备阶段烘焙为模型空间几何，因此当前可见部分只需一次写入。
     */
    private static void renderStaticPrintingCubes(MachineMaxBlockEntity blockEntity, PoseStack poseStack,
                                                  MultiBufferSource bufferSource, int packedLight) {
        ICube[] cubes = blockEntity.visiblePrintingCubes;
        if (cubes.length == 0) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(blockEntity.vehicleDisplay.getTexture()));
        PoseStack.Pose pose = poseStack.last();
        TreeGeometryWriter.writeCubes(cubes, consumer, pose.pose(), pose.normal(),
                packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
    }

    /**
     * Poly mesh 没有可用于打印进度的 cube 颗粒度，因此只在最后一个 cube 阶段整体显示。
     */
    private static void renderPrintingPolyMeshes(MachineMaxBlockEntity blockEntity, PoseStack poseStack,
                                                  MultiBufferSource bufferSource, int packedLight) {
        TreeBedrockModel model = blockEntity.printingModel;
        TreeModelInstance instance = blockEntity.printingModelInstance;
        VertexConsumer consumer = bufferSource.getBuffer(BedrockModelRenderTypes.polyMeshCutout(blockEntity.vehicleDisplay.getTexture()));
        for (TreeBoneDefinition bone : model.bones()) {
            if (bone.polyMeshes().length == 0) {
                continue;
            }
            Matrix4f boneTransform = instance.getGlobalTransform(bone.index());
            PoseStack.Pose pose = poseStack.last();
            Matrix4f meshPose = new Matrix4f(pose.pose()).mul(boneTransform);
            Matrix3f meshNormal = new Matrix3f(pose.normal()).mul(instance.getGlobalNormal(bone.index()));
            TreeGeometryWriter.writePolyMeshes(bone.polyMeshes(), consumer, meshPose, meshNormal,
                    packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        }
    }

}
