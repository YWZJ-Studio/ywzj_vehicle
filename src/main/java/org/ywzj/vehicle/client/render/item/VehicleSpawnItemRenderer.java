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
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.item.VehicleSpawnItem;

import javax.annotation.Nonnull;

public class VehicleSpawnItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final SlotModel VEHICLE_SPAWN_ITEM_MODEL = new SlotModel();

    public VehicleSpawnItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack itemStack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (itemStack.getItem() instanceof VehicleSpawnItem) {
            CompoundTag tag = itemStack.getOrCreateTag();
            ResourceLocation vehicleId = YwzjVehicle.resourceLocation(tag.getString("vehicleId"));
            var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicleId).orElse(null);
            poseStack.pushPose();
            {
                if (display != null) {
                    ResourceLocation slotTexture = display.getSlotTexture();
                    if (slotTexture != null) {
                        poseStack.translate(0.5, 0.5, 0.5);
                        if (transformType != ItemDisplayContext.GUI) {
                            poseStack.mulPose(Axis.YN.rotationDegrees(-45f));
                        }
                        VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(slotTexture));
                        VEHICLE_SPAWN_ITEM_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                        poseStack.popPose();
                        return;
                    }
                }
                poseStack.translate(0.5, 0.5, 0.5);
                VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
                VEHICLE_SPAWN_ITEM_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            poseStack.popPose();
        }
    }

}
