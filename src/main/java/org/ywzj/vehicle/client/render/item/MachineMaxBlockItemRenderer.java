package org.ywzj.vehicle.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.ywzj.vehicle.item.MachineMaxBlockItem;
import org.ywzj.vehicle.resource.BedrockModelLoader;

import javax.annotation.Nonnull;

import static org.ywzj.vehicle.client.render.entity.block.MachineMaxBlockRenderer.MACHINE_MAX_BLOCK_MODEL;
import static org.ywzj.vehicle.client.render.entity.block.MachineMaxBlockRenderer.MACHINE_MAX_BLOCK_TEXTURE;

public class MachineMaxBlockItemRenderer extends BlockEntityWithoutLevelRenderer {

    public MachineMaxBlockItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack itemStack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource bufferSource, int pPackedLight, int pPackedOverlay) {
        if (itemStack.getItem() instanceof MachineMaxBlockItem) {
            BedrockModel machineMaxBlockModel = BedrockModelLoader.getModel(MACHINE_MAX_BLOCK_MODEL);
            poseStack.pushPose();
            {
                if (transformType == ItemDisplayContext.FIXED) {
                    poseStack.translate(0.5, 0.15f, 0.5);
                    poseStack.scale(0.7F, 0.7F, 0.7F);
                } else if (transformType == ItemDisplayContext.GUI) {
                    poseStack.translate(0.5f, 0.15f, 0.5f);
                    poseStack.mulPose(Axis.XP.rotationDegrees(20));
                    poseStack.mulPose(Axis.YP.rotationDegrees(165));
                    poseStack.scale(0.7F, 0.7F, 0.7F);
                } else {
                    poseStack.translate(0.5, 0.3f, 0.5);
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                }
                machineMaxBlockModel.renderToBuffer(poseStack, bufferSource,
                        RenderType.entityCutout(MACHINE_MAX_BLOCK_TEXTURE),
                        BedrockModelRenderTypes.polyMeshCutout(MACHINE_MAX_BLOCK_TEXTURE),
                        pPackedLight,
                        OverlayTexture.pack(0f, false)
                );
            }
            poseStack.popPose();
        }
    }

}
