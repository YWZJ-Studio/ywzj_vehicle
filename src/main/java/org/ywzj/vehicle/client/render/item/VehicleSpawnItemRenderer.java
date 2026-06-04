package org.ywzj.vehicle.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.SlotModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.item.VehicleSpawnItem;

import javax.annotation.Nonnull;

import static org.ywzj.vehicle.api.entity.ICustomVehicle.TAG_VEHICLE_ID;

public class VehicleSpawnItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final SlotModel VEHICLE_SPAWN_ITEM_MODEL = new SlotModel();

    public VehicleSpawnItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack itemStack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (itemStack.getItem() instanceof VehicleSpawnItem) {
            CompoundTag tag = itemStack.getOrCreateTag();
            ResourceLocation vehicleId = YwzjVehicle.resourceLocation(tag.getString(TAG_VEHICLE_ID));
            var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicleId).orElse(null);
            poseStack.pushPose();
            {
                poseStack.translate(0.5, 0.5, 0.5);
                if (display != null) {
                    ResourceLocation slotTexture = display.getSlotTexture();
                    if (slotTexture != null) {
                        if (transformType != ItemDisplayContext.GUI) {
                            poseStack.mulPose(Axis.YN.rotationDegrees(-45f));
                        }
                        renderExperimentalMark(transformType, vehicleId, poseStack, pBuffer, pPackedLight);
                        VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(slotTexture));
                        VEHICLE_SPAWN_ITEM_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                        poseStack.popPose();
                        return;
                    }
                }
                VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
                VEHICLE_SPAWN_ITEM_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                renderExperimentalMark(transformType, vehicleId, poseStack, pBuffer, pPackedLight);
            }
            poseStack.popPose();
        }
    }

    private void renderExperimentalMark(@Nonnull ItemDisplayContext transformType, ResourceLocation vehicleId, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight) {
        if (transformType != ItemDisplayContext.GUI) {
            return;
        }
        var vehicleData = CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId).orElse(null);
        if (vehicleData == null || !vehicleData.isExperimental()) {
            return;
        }
        Matrix4f matrix4f = poseStack.last().pose();
        VertexConsumer backgroundBuffer = pBuffer.getBuffer(RenderType.gui());
        backgroundBuffer.vertex(matrix4f, -0.5f, 0.5f, 0).color(0.25F, 0.25F, 0.25F, 0.6F).endVertex();
        backgroundBuffer.vertex(matrix4f, -0.5f, -0.5f, 0).color(0.25F, 0.25F, 0.25F, 0.6F).endVertex();
        backgroundBuffer.vertex(matrix4f, 0.5f, -0.5f, 0).color(0.25F, 0.25F, 0.25F, 0.6F).endVertex();
        backgroundBuffer.vertex(matrix4f, 0.5f, 0.5f, 0).color(0.25F, 0.25F, 0.25F, 0.6F).endVertex();
        var font = Minecraft.getInstance().font;
        Component text = Component.translatable("tips.experimental");
        poseStack.pushPose();
        {
            poseStack.translate(-0.45, 0.45f, 0);
            float scale = 0.03F;
            poseStack.scale(scale, -scale, scale);
            Matrix4f textMatrix = poseStack.last().pose();
            font.drawInBatch(text, 0, 0, 0xFFFF0000, true,
                    textMatrix, pBuffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                    0, pPackedLight);
        }
        poseStack.popPose();
    }

}
