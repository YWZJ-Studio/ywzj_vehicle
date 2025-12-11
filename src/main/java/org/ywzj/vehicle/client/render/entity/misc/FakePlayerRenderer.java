package org.ywzj.vehicle.client.render.entity.misc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
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
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.client.render.model.FakePlayerModel;
import org.ywzj.vehicle.entity.misc.FakePlayer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakePlayerRenderer extends LivingEntityRenderer<FakePlayer, FakePlayerModel> {

    private static final Map<String, ResourceLocation> SKIN_CACHE = new HashMap<>();

    public FakePlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new FakePlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5f);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    public void render(FakePlayer entity, float pEntityYaw, float pPartialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int pPackedLight) {
        float scale = 0.9375f;
        poseStack.scale(scale, scale, scale);
        super.render(entity, pEntityYaw, pPartialTicks, poseStack, bufferSource, pPackedLight);
    }

    @Override
    protected boolean shouldShowName(FakePlayer entity) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(FakePlayer fakePlayer) {
        String playerName = fakePlayer.getName().getString();

        if (SKIN_CACHE.containsKey(playerName)) {
            return SKIN_CACHE.get(playerName);
        }

        Minecraft mc = Minecraft.getInstance();
        GameProfile profile = new GameProfile(null, playerName);

        mc.getSkinManager().registerSkins(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                SKIN_CACHE.put(playerName, location);
            }
        }, true);

        // 默认皮肤（用于首次渲染时皮肤尚未加载完成）
        ResourceLocation defaultSkin = DefaultPlayerSkin.getDefaultSkin(UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8)));
        SKIN_CACHE.putIfAbsent(playerName, defaultSkin);

        return SKIN_CACHE.get(playerName);
    }

}
