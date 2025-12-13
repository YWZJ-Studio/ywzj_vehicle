package org.ywzj.vehicle.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.maydaymemory.mae.basic.YXZRotationView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.util.RenderDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ViewportEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Quaternionf;
import org.joml.Vector3fc;
import org.ywzj.vehicle.client.render.model.PositionableModel;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 渲染基岩版模型的抽象 BEWLR，包含了一些基础实现，如摄像机动画的应用、定位组的应用。
 *
 * @param <M> 基岩版模型
 */
public abstract class AbstractGeoItemRenderer<M extends BedrockModel> extends BlockEntityWithoutLevelRenderer {
    public static final String FP_CAMERA_BONE_NAME = "camera";
    private static final SlotModel SLOT_MODEL = new SlotModel();

    public AbstractGeoItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Nullable
    public abstract Pair<M, RenderType> getModelAndRenderType(ItemStack stack);

    @Nullable
    public abstract Pair<M, RenderType> getLodModelAndRenderType(ItemStack stack);

    @Nullable
    public abstract ResourceLocation getSlotTexture(ItemStack stack);

    /**
     * 应用摄像机动画对世界的变换（只有旋转生效）
     */
    public void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event, ItemStack stack, Quaternionf animateRot, float multiplier) {
        Quaternionf initialRotation = new Quaternionf().rotateYXZ(-event.getYaw(), -event.getPitch(), -event.getRoll());
        YXZRotationView rotationView = new YXZRotationView(initialRotation.mul(animateRot));
        Vector3fc eulerAngle = rotationView.asEulerAngle();
        event.setYaw(-eulerAngle.y());
        event.setPitch(-eulerAngle.x());
        event.setRoll(-eulerAngle.z());
    }

    /**
     * 应用摄像机动画对手持物品的变换（只有旋转生效）
     */
    public void applyItemInHandCameraAnimation(PoseStack poseStack, ItemStack stack, Quaternionf animateRot, float multiplier) {
        poseStack.mulPose(animateRot);
    }

    /**
     * 渲染模型前调用。默认会应用定位组变换。可以用于施加动画的影响。
     */
    protected void beforeRender(PoseStack poseStack, ItemDisplayContext ctx, M model, ItemStack stack, float partialTicks) {
        if (ctx == ItemDisplayContext.GROUND) {
            poseStack.translate(0.5, 0.3125, 0.5);
        } else if (!ctx.firstPerson()) {
            poseStack.translate(0.5, 0.5, 0.5);
        }
        if (model instanceof PositionableModel positionableBedrockModel) {
            positionableBedrockModel.applyTransform(poseStack, ctx);
        }
    }

    /**
     * 渲染模型后调用。可以做一些清理工作，例如将 bind pose 应用给模型以清除动画影响。默认什么都不会做。
     */
    protected void afterRender(PoseStack poseStack, ItemDisplayContext ctx, M model, ItemStack stack, MultiBufferSource bufferSource,
                               int light, float partialTicks) {
    }

    /**
     * 进行第一人称下的渲染
     */
    public void renderFirstPerson(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int light, float partialTick) {
        // 默认的左右手位移
        int i = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 1 : -1;
        poseStack.translate((float) i * 0.5F, -0.75F, -0.75F);
        render(stack, ctx, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY, partialTick);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                             int light, int overlay) {
        if (ctx.firstPerson()) {
            return;
        }
        render(stack, ctx, poseStack, bufferSource, light, overlay, Minecraft.getInstance().getPartialTick());
    }

    protected void render(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                          int light, int overlay, float partialTicks) {
        Pair<M, RenderType> modelAndRenderType = null;
        // 如果不在高模渲染距离内，则尝试获取低模，如果低模不存在，仍然用高模作为 fallback
        if (!RenderDistance.inRenderHighPolyModelDistance(poseStack) && !ctx.firstPerson()) {
            modelAndRenderType = getLodModelAndRenderType(stack);
            if (modelAndRenderType == null) {
                modelAndRenderType = getModelAndRenderType(stack);
            }
        } else {
            modelAndRenderType = getModelAndRenderType(stack);
        }
        if (ctx == ItemDisplayContext.GUI || modelAndRenderType == null) {
            renderSlot(stack, poseStack, bufferSource, light, overlay, modelAndRenderType);
            return;
        }
        poseStack.pushPose();
        M model = modelAndRenderType.getLeft();
        beforeRender(poseStack, ctx, model, stack, partialTicks);
        RenderType renderType = modelAndRenderType.getRight();
        model.renderToBuffer(poseStack, bufferSource.getBuffer(renderType), light, overlay);
        afterRender(poseStack, ctx, model, stack, bufferSource, light, partialTicks);
        poseStack.popPose();
    }

    public void renderSlot(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, Pair<M, RenderType> modelAndRenderType) {
        ResourceLocation slotTexture = getSlotTexture(stack);
        if (slotTexture != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0);
            SLOT_MODEL.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityTranslucent(slotTexture)), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        } else if (modelAndRenderType == null) {
            // 模型和 gui texture 都不存在，渲染 missing texture
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0);
            RenderType renderType1 = RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation());
            SLOT_MODEL.renderToBuffer(poseStack, bufferSource.getBuffer(renderType1), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }

    /**
     * 使用该渲染器的物品会阻止原版的viewBobbing，以便应用自定义的跑步/走路动画。
     * @return 是否阻止原版viewBobbing
     */
    public boolean blockViewBobbing() {
        return true;
    }
}
