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
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlocks;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.item.FigureBoxItem;

import javax.annotation.Nonnull;

import static org.ywzj.vehicle.item.FigureBoxItem.ENTITY_DATA;
import static org.ywzj.vehicle.item.FigureBoxItem.ENTITY_ID;

public class FigureBoxItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final SlotModel VEHICLE_SLOT_MODEL = new SlotModel();
    private final FigureBoxBlockEntity figureBoxBlockEntity = new FigureBoxBlockEntity(BlockPos.ZERO, Blocks.AIR.defaultBlockState());

    public FigureBoxItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
        figureBoxBlockEntity.yRot = -45;
    }

    @Override
    public void renderByItem(@Nonnull ItemStack itemStack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (itemStack.getItem() instanceof FigureBoxItem) {
            BlockRenderDispatcher blockDispatcher = Minecraft.getInstance().getBlockRenderer();
            BlockState state = AllBlocks.FIGURE_BOX_BLOCK.get().defaultBlockState();
            poseStack.pushPose();
            {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(20));
                poseStack.mulPose(Axis.YP.rotationDegrees(-15));
                poseStack.scale(0.8f, 0.8f, 0.8f);
                poseStack.translate(-0.5, -0.5, -0.5);
                boolean isGui = true;
                if (transformType != ItemDisplayContext.GUI) {
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    poseStack.translate(0.3f, 1.2f, 0.5f);
                    isGui = false;
                }
                blockDispatcher.renderSingleBlock(state, poseStack, pBuffer, pPackedLight, pPackedOverlay, ModelData.EMPTY, null);
                CompoundTag tag = itemStack.getOrCreateTag();
                if (tag.contains(ENTITY_ID) && tag.contains(ENTITY_DATA)) {
                    String entityId = tag.getString(ENTITY_ID);
                    CompoundTag entityData = tag.getCompound(ENTITY_DATA);
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(YwzjVehicle.resourceLocation(entityId));
                    if (type != null) {
                        Entity entity = type.create(Minecraft.getInstance().level);
                        entity.load(entityData);
                        if (entity instanceof AbstractVehicle vehicle && isGui) {
                            ResourceLocation vehicleId = vehicle.getVehicleId();
                            BaseDisplay display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicleId).orElse(null);
                            if (display != null) {
                                ResourceLocation slotTexture = display.getSlotTexture();
                                if (slotTexture != null) {
                                    AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(slotTexture);
                                    if (texture != MissingTextureAtlasSprite.getTexture()) {
                                        poseStack.translate(0.5, 0.6, 0);
                                        VertexConsumer buffer = pBuffer.getBuffer(RenderType.text(slotTexture));
                                        VEHICLE_SLOT_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                                        poseStack.popPose();
                                        return;
                                    }
                                }
                            }
                        }
                        figureBoxBlockEntity.setEntity(entity);
                        BlockEntityRenderer<? super FigureBoxBlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(figureBoxBlockEntity);
                        if (renderer != null) {
                            renderer.render(figureBoxBlockEntity, 0.0f, poseStack, pBuffer, pPackedLight, pPackedOverlay);
                        }
                    }
                }
            }
            poseStack.popPose();
        }
    }

}
