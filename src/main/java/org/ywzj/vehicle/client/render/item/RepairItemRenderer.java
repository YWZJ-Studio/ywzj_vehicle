package org.ywzj.vehicle.client.render.item;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.handler.FirstPersonHandler;
import org.ywzj.vehicle.client.render.model.HandedBedrockModel;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.InternalAssets;

import static net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
import static net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

public class RepairItemRenderer extends AbstractGeoItemRenderer<HandedBedrockModel> {
    private static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);


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
        return null;
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
            if (FirstPersonHandler.instance != null) {
                model.applyPose(BLENDER.blend(model.getBindPose(), FirstPersonHandler.instance.getCurrentPose()));
                applyFirstPersonPositioningTransform(poseStack, model);
            }
        }
    }

    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, HandedBedrockModel model) {
        Matrix4f idleViewMatrix = new Matrix4f(model.getBone("camera").getGlobalTransform());
        poseStack.mulPoseMatrix(idleViewMatrix.invert());
    }
}
