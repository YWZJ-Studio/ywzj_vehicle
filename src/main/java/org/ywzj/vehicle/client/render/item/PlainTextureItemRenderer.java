package org.ywzj.vehicle.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.SlotModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.item.PlainTextureItem;

import javax.annotation.Nonnull;

public class PlainTextureItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final SlotModel PLAIN_TEXTURE_ITEM_MODEL = new SlotModel();

    public PlainTextureItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack itemStack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (itemStack.getItem() instanceof PlainTextureItem) {
            CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            ResourceLocation textureLocation = YwzjVehicle.resourceLocation(tag.getString("textureLocation"));
            poseStack.pushPose();
            {
                poseStack.translate(0.5, 0.5, 0.5);
                VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(textureLocation));
                PLAIN_TEXTURE_ITEM_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay);
            }
            poseStack.popPose();
        }
    }

}
