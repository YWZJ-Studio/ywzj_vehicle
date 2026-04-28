package org.ywzj.vehicle.client.render.entity.misc;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.client.render.model.FakePlayerModel;
import org.ywzj.vehicle.entity.misc.FakePlayer;

public class FakePlayerRenderer extends LivingEntityRenderer<FakePlayer, FakePlayerModel> {

    public FakePlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new FakePlayerModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    public void render(FakePlayer entity, float pEntityYaw, float pPartialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int pPackedLight) {
        poseStack.pushPose();
        {
            float scale = 0.9375f;
            poseStack.scale(scale, scale, scale);
            super.render(entity, pEntityYaw, pPartialTicks, poseStack, bufferSource, pPackedLight);
        }
        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(FakePlayer entity) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(FakePlayer entity) {
        String name = entity.getName().getString();
        Minecraft mc = Minecraft.getInstance();
        GameProfile profile = new GameProfile(entity.getUUID(), name);
        return mc.getSkinManager().getInsecureSkin(profile).texture();
    }

}
