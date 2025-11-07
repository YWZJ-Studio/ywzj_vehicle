package org.ywzj.vehicle.client.render.entity.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.ywzj.vehicle.block.FigureBoxBlock;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class FigureBoxBlockRenderer implements BlockEntityRenderer<FigureBoxBlockEntity> {

    public FigureBoxBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FigureBoxBlockEntity figureBoxBlockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (figureBoxBlockEntity.getEntity() == null) {
            return;
        }

        Entity entity = figureBoxBlockEntity.getEntity();
        Direction facing = figureBoxBlockEntity.getBlockState().getValue(FigureBoxBlock.FACING);
        float yaw;
        switch (facing) {
            case NORTH -> yaw = 180f;
            case SOUTH -> yaw = 0f;
            case WEST -> yaw = 90f;
            case EAST -> yaw = -90f;
            default -> yaw = 0f;
        }
        yaw += 45;
        entity.moveTo(figureBoxBlockEntity.getBlockPos().getX() + 0.5,
                figureBoxBlockEntity.getBlockPos().getY(),
                figureBoxBlockEntity.getBlockPos().getZ() + 0.5,
                yaw, 0);
        if (entity instanceof LivingEntity) {
            entity.setYRot(yaw);
            entity.setYBodyRot(yaw);
            entity.setYHeadRot(yaw);
        }

        poseStack.pushPose();
        {
            double length;
            if (entity instanceof AbstractVehicle) {
                AABB aabb = entity.getBoundingBox();
                length = aabb.maxZ - aabb.minZ;
            } else {
                length = entity.getBbHeight() * 2;
            }
            poseStack.translate(0.5, 0.2, 0.5);
            float scale = (float) (1 / length / 1.2);
            poseStack.scale(scale, scale, scale);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.render(entity, 0.0, 0.0, 0.0, entity.getYRot(), 1, poseStack, bufferSource, 0xF000F0);
        }
        poseStack.popPose();
    }

}
