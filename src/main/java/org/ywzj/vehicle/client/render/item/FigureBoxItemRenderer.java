package org.ywzj.vehicle.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.SlotModel;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlocks;
import org.ywzj.vehicle.blockentity.FigureBoxBlockEntity;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.item.FigureBoxItem;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class FigureBoxItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final SlotModel VEHICLE_SLOT_MODEL = new SlotModel();
    private final FigureBoxBlockEntity figureBoxBlockEntity = new FigureBoxBlockEntity(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
    private final LoadingCache<ItemStack, Entity> entityCache = CacheBuilder.newBuilder()
            .weakKeys()
            .maximumSize(50)
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Entity load(ItemStack stack) {
                    CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    String entityId = tag.getString("entityId");
                    CompoundTag entityData = tag.getCompound("entityData");
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(YwzjVehicle.resourceLocation(entityId));
                    if (type != null) {
                        Entity entity = type.create(Minecraft.getInstance().level);
                        if (entity != null) {
                            entity.load(entityData);
                        }
                        return entity;
                    }
                    return null;
                }
            });

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
                CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                if (tag.contains("entityId") && tag.contains("entityData")) {
                    try {
                        Entity entity = entityCache.getUnchecked(itemStack);
                        if (entity == null) {
                            poseStack.popPose();
                            return;
                        }
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
                                        VEHICLE_SLOT_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay);
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
                    } catch (Exception ignore) {
                    }
                }
            }
            poseStack.popPose();
        }
    }

}
