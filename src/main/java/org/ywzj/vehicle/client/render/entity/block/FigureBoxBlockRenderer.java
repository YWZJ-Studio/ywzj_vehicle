package org.ywzj.vehicle.client.render.entity.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Optional;

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
        renderEntity(poseStack, figureBoxBlockEntity, bufferSource, packedLight);
    }

    public static void renderEntity(PoseStack poseStack, FigureBoxBlockEntity figureBoxBlockEntity, MultiBufferSource bufferSource, int packedLight) {
        Entity entity = figureBoxBlockEntity.getEntity();
        entity.moveTo(figureBoxBlockEntity.getBlockPos().getX() + 0.5,
                figureBoxBlockEntity.getBlockPos().getY(),
                figureBoxBlockEntity.getBlockPos().getZ() + 0.5,
                0, 0);
        poseStack.pushPose();
        {
            double length = 1f;
            if (entity instanceof AbstractVehicle vehicle) {
                Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(vehicle.getVehicleId());
                if (vehicleDataOptional.isPresent()) {
                    length = vehicleDataOptional.get().getStructureLength();
                }
            } else {
                length = entity.getBbHeight() * 2;
            }
            poseStack.translate(0.5, 0.2, 0.5);
            float scale = (float) (1 / length / 1.2);
            scale *= figureBoxBlockEntity.scale;
            poseStack.translate(figureBoxBlockEntity.xShift, figureBoxBlockEntity.yShift, figureBoxBlockEntity.zShift);
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Axis.YP.rotationDegrees(figureBoxBlockEntity.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(figureBoxBlockEntity.xRot));
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack itemStack = itemEntity.getItem();
                ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
                BakedModel bakedmodel = itemRenderer.getModel(itemStack, entity.level(), null, entity.getId());
                itemRenderer.render(itemEntity.getItem(), ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);
            } else {
                dispatcher.render(entity, 0.0, 0.0, 0.0, 0, 0, poseStack, bufferSource, packedLight);
            }
        }
        poseStack.popPose();
    }

}
