package org.ywzj.vehicle.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.SlotModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.item.DecorationItem;

import javax.annotation.Nonnull;

public class DecorationItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final SlotModel DECORATION_ITEM_MODEL = new SlotModel();

    public DecorationItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack itemStack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (itemStack.getItem() instanceof DecorationItem) {
            CompoundTag tag = itemStack.getOrCreateTag();
            ResourceLocation decorationDisplayId = YwzjVehicle.resourceLocation(tag.getString(DecorationItem.TAG_DECORATION_DISPLAY_ID));
            var decorationDisplay = ClientAssetsManager.INSTANCE.getDecorationDisplay(decorationDisplayId).orElse(null);
            poseStack.pushPose();
            {
                poseStack.translate(0.5, 0.5, 0.5);
                if (decorationDisplay != null) {
                    ResourceLocation slotTexture = decorationDisplay.getSlotTexture();
                    VehicleBedrockModel model = decorationDisplay.getModel();
                    ResourceLocation texture = decorationDisplay.getTexture();
                    if (slotTexture != null) {
                        if (transformType != ItemDisplayContext.GUI) {
                            poseStack.mulPose(Axis.YN.rotationDegrees(-45f));
                        }
                        VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(slotTexture));
                        DECORATION_ITEM_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                        poseStack.popPose();
                        return;
                    } else if (model != null && texture != null && model.hasBakedModel()) {
                        model.renderToBufferBaked(poseStack, pBuffer, texture, pPackedLight);
                        poseStack.popPose();
                        return;
                    }
                }
                VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(YwzjVehicle.modLocation("textures/item/decoration_item.png")));
                DECORATION_ITEM_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack.popPose();
        }
    }

}
