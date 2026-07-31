package org.ywzj.vehicle.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.HandedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.AbstractGeoItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.ywzj.vehicle.client.render.animation.item.RepairItemAnimationInstance;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.InternalAssets;
import org.ywzj.vehicle.util.MathUtil;

import static net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
import static net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
import static org.ywzj.vehicle.client.resource.InternalAssets.REPAIR_TOOL_SLOT_TEXTURE;

public class RepairItemRenderer extends AbstractGeoItemRenderer<HandedBedrockModel> {

    @Nullable
    @Override
    public Pair<HandedBedrockModel, RenderType> getModelAndRenderType(ItemStack stack) {
        return Pair.of(
                ClientAssetsManager.INSTANCE.getInternalAssets().getRepairToolModel(),
                RenderType.entityCutout(InternalAssets.REPAIR_TOOL_TEXTURE)
        );
    }

    @Override
    public @Nullable Pair<HandedBedrockModel, RenderType> getLodModelAndRenderType(ItemStack stack) {
        return getModelAndRenderType(stack);
    }

    @Nullable
    @Override
    public ResourceLocation getSlotTexture(ItemStack stack) {
        return REPAIR_TOOL_SLOT_TEXTURE;
    }

    @Override
    public void renderFirstPerson(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int light, float partialTick) {
        // 取消掉默认的位移，全部由定位组接管
        if (ctx == FIRST_PERSON_LEFT_HAND) {
            return;
        }
        render(stack, ctx, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY, partialTick);
    }

    @Override
    protected void beforeRender(PoseStack poseStack, ItemDisplayContext ctx, HandedBedrockModel model, ItemStack stack, float partialTicks) {
        super.beforeRender(poseStack, ctx, model, stack, partialTicks);
        model.setRenderHand(ctx.firstPerson());
        if (ctx == FIRST_PERSON_RIGHT_HAND) {
            var ani = FirstPersonRenderHandler.getActiveAnimationInstance();
            if (ani != null) {
                model.applyPose(ani.getCachedPose());
                applyFirstPersonPositioningTransform(poseStack, model);
            }
        }
    }

    @Override
    protected void afterRender(PoseStack poseStack, ItemDisplayContext ctx, HandedBedrockModel model, ItemStack stack, MultiBufferSource bufferSource, int light, float partialTicks) {
        model.applyPose(model.getBindPose());
    }

    @Override
    public void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event, ItemStack stack, Quaternionf animateRot, float partialTicks) {

    }

    @Override
    public void applyItemInHandCameraAnimation(PoseStack poseStack, ItemStack stack, Quaternionf animateRot, float partialTicks) {

    }

    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, HandedBedrockModel model) {
        Matrix4f idleViewMatrix = new Matrix4f(model.getBone("camera").getGlobalTransform()).invert();
        MathUtil.mulMatrix(poseStack, idleViewMatrix);
    }

    @Override
    public @Nullable IFPAnimationInstance createAnimationInstance(ItemStack stack, Entity entity) {
        return new RepairItemAnimationInstance(stack,
                ClientAssetsManager.INSTANCE.getInternalAssets().getRepairToolAnimations(),
                ClientAssetsManager.INSTANCE.getInternalAssets().getRepairToolModel()
        );
    }

    @Override
    public long getPutAwayDuration(ItemStack stack) {
        return 215;
    }

    @Override
    public boolean blockViewBobbing() {
        return false;
    }

}
