package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Math;
import org.joml.Matrix4f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ServerHitVehicleEvent;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class VehicleHitIndicatorOverlay implements LayeredDraw.Layer {

    private static final ResourceLocation HIT_COMMON = YwzjVehicle.modLocation("textures/ui/hit_common.png");
    private static final ResourceLocation HIT_VEHICLE = YwzjVehicle.modLocation("textures/ui/hit_vehicle.png");
    private static final float MAX_OFFSET = 0.6f;
    private static final long KEEP_TIME = 300;
    private static boolean hitVehicle;
    private static long hitTimestamp = -1L;
    private static long killTimestamp = -1L;
    public static List<ServerHitVehicleEvent> events = new ArrayList<>();
    public static long lastHitTime = System.currentTimeMillis();

    @SubscribeEvent(receiveCanceled = true)
    public static void onRenderOverlay(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            renderHitMarker(event.getGuiGraphics());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        if (!AllConfigs.common.hitIndicator.get()) {
            return;
        }
        if (System.currentTimeMillis() - lastHitTime > 3000) {
            events.clear();
        }
        if (events.isEmpty()) {
            return;
        }
        Player player = LocalVehiclePlayer.instance.getPlayer();
        if (player == null) {
            return;
        }
        int entityId = events.get(0).entityId;
        Entity entity = player.level().getEntity(entityId);
        if (entity == null) {
            LocalVehiclePlayer.ServerEntity serverEntity = LocalVehiclePlayer.instance.serverEntities.get(entityId);
            if (serverEntity == null || serverEntity.entity == null) {
                return;
            }
            entity = serverEntity.entity;
        }
        Vec3 viewVec;
        float scale;
        ServerHitVehicleEvent topEvent = events.get(0);
        double damage = events.stream().mapToDouble(event -> event.damage).sum();
        if (entity instanceof AbstractVehicle vehicle) {
            viewVec = vehicle.relativeRotPos(topEvent.hitRelativePosition.add(vehicle.position()), false).subtract(entity.position());
            scale = Math.min(10, 8 / (vehicle.getMainCubeOBB().obb().extents().z * 2) * 10);
        } else {
            viewVec = VectorUtil.relativeRotPos(entity, topEvent.hitRelativePosition.add(entity.position()), false).subtract(entity.position());
            scale = Math.min(10, (float) (48 / entity.getBoundingBox().getSize()));
        }
        float pitch = (float) Math.toDegrees(Math.atan2(-viewVec.y, Math.sqrt(viewVec.x * viewVec.x + viewVec.z * viewVec.z)));
        float yaw = (float) Math.toDegrees(Math.atan2(viewVec.x, viewVec.z));
        guiGraphics.pose().pushPose();
        {
            double modelX = screenWidth - (double) screenWidth / 8;
            double modelY = (double) screenHeight / 2;
            guiGraphics.pose().translate(modelX, modelY + (double) screenHeight / 5, 0);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, topEvent.message, 0, -55, Color.WHITE);
            RenderHelper.drawCenteredString(guiGraphics, Minecraft.getInstance().font, String.format("-%.2f", damage), 0, -45, Color.RED);

            Vec3 root = new Vec3(0, 0, 0);
            guiGraphics.pose().rotateAround(Axis.XP.rotationDegrees(pitch + 180), (float) root.x, (float) root.y, (float) root.z);
            guiGraphics.pose().rotateAround(Axis.YP.rotationDegrees(yaw), (float) root.x, (float) root.y, (float) root.z);

            guiGraphics.pose().last().pose().mul((new Matrix4f()).scaling(scale, scale, -scale));
            Lighting.setupForEntityInInventory();
            EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

            entityRenderDispatcher.setRenderShadow(false);
            Entity finalEntity = entity;
            RenderSystem.runAsFancy(() -> {
                entityRenderDispatcher.render(finalEntity, 0, 0,0, finalEntity.getYRot(), 1.0F, guiGraphics.pose(), guiGraphics.bufferSource(), 15728880);
                for (ServerHitVehicleEvent hitVehicleEvent : events) {
                    Vec3 start;
                    Vec3 end;
                    if (finalEntity instanceof AbstractVehicle vehicle) {
                        start = vehicle.relativeRotPos(hitVehicleEvent.hitRelativePosition.add(vehicle.position()), false).subtract(finalEntity.position());
                        end = start.subtract(vehicle.relativeRotDirection(hitVehicleEvent.hitRelativeVector, false).normalize().scale(3));
                    } else {
                        start = VectorUtil.relativeRotPos(finalEntity, hitVehicleEvent.hitRelativePosition.add(finalEntity.position()), false).subtract(finalEntity.position());
                        end = start.subtract(VectorUtil.relativeRotDirection(finalEntity, hitVehicleEvent.hitRelativeVector, false).normalize().scale(3));
                    }
                    renderRedLine(guiGraphics, start, end);
                }
            });
            guiGraphics.flush();
            entityRenderDispatcher.setRenderShadow(true);
        }
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
        if (entity instanceof AbstractVehicle vehicle && !vehicle.equals(player.getVehicle())) {
            AABB aabb = vehicle.getBoundingBox();
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            Vec3 pos = new Vec3(Mth.lerp(partialTick, vehicle.xo, vehicle.getX()),
                    Mth.lerp(partialTick, vehicle.yo, vehicle.getY()) + aabb.maxY - aabb.minY,
                    Mth.lerp(partialTick, vehicle.zo, vehicle.getZ()));
            Vec3 screenPos = VectorUtil.worldToScreen(pos);
            double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(vehicle.getEyePosition());
            float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.5f * Math.max((512 - distance) / 512, 0.1), 0.66, 1);
            VehicleOverlay.renderHealth(guiGraphics, screenPos.x, screenPos.y, 90, 5, vehicle, size);
        }
    }

    private void renderRedLine(GuiGraphics guiGraphics, Vec3 start, Vec3 end) {
        PoseStack pose = guiGraphics.pose();
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        float r = 1.0f, g = 0.0f, b = 0.0f, a = 1.0f;
        float offset = 0.01f;
        for (int i = -1; i <= 2; i++) {
            for (int j = -1; j <= 2; j++) {
                lineConsumer.addVertex(matrix, (float) (start.x + i * offset), (float) start.y, (float) (start.z + j * offset))
                        .setColor(r, g, b, a)
                        .setNormal(0, 1, -100);
                lineConsumer.addVertex(matrix, (float) (end.x + i * offset), (float) end.y, (float) (end.z + j * offset))
                        .setColor(r, g, b, a)
                        .setNormal(0, 1, -100);
            }
        }
    }

    private static void renderHitMarker(GuiGraphics graphics) {
        long remainHitTime = System.currentTimeMillis() - hitTimestamp;
        long remainKillTime = System.currentTimeMillis() - killTimestamp;
        float fadeTime;
        float diffusion = 1f;
        boolean kill = false;
        if (remainKillTime > KEEP_TIME) {
            if (remainHitTime > KEEP_TIME) {
                return;
            } else {
                fadeTime = remainHitTime;
            }
        } else {
            fadeTime = remainKillTime;
            diffusion = 2f;
            kill = true;
        }
        float progress = fadeTime / ((float) KEEP_TIME * diffusion);
        progress = Math.min(progress, 1.0f);
        float eased = 1.0f - (float) java.lang.Math.pow(1.0f - progress, 3);
        float offset = eased * MAX_OFFSET * diffusion;
        float alpha = 1.0f - (float) java.lang.Math.pow(progress, 1.5);

        float scale = 2f;
        int size = (int) (16 * scale);
        int sizeHalf = size / 2;

        double x = VehicleAimAtOverlay.getScreenAimX() - sizeHalf;
        double y = VehicleAimAtOverlay.getScreenAimY() - sizeHalf;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(x, y, 0);

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            if (kill) {
                RenderSystem.setShaderColor(1F, 0f, 0f, alpha);
            } else {
                RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
            }

            ResourceLocation hitMarker = hitVehicle ? HIT_VEHICLE : HIT_COMMON;

            // 左上
            poseStack.pushPose();
            poseStack.translate(-offset, -offset, 0);
            graphics.blit(hitMarker, 0, 0, 0, 0, sizeHalf, sizeHalf, size, size);
            poseStack.popPose();

            // 右上
            poseStack.pushPose();
            poseStack.translate(sizeHalf + offset, -offset, 0);
            graphics.blit(hitMarker, 0, 0, sizeHalf, 0, sizeHalf, sizeHalf, size, size);
            poseStack.popPose();

            // 左下
            poseStack.pushPose();
            poseStack.translate(-offset, sizeHalf + offset, 0);
            graphics.blit(hitMarker, 0, 0, 0, sizeHalf, sizeHalf, sizeHalf, size, size);
            poseStack.popPose();

            // 右下
            poseStack.pushPose();
            poseStack.translate(sizeHalf + offset, sizeHalf + offset, 0);
            graphics.blit(hitMarker, 0, 0, sizeHalf, sizeHalf, sizeHalf, sizeHalf, size, size);
            poseStack.popPose();

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.disableBlend();
        }
        poseStack.popPose();
    }

    public static void markHitTimestamp(boolean hitVehicle, boolean kill) {
        if (kill) {
            VehicleHitIndicatorOverlay.killTimestamp = System.currentTimeMillis();
        } else {
            VehicleHitIndicatorOverlay.hitTimestamp = System.currentTimeMillis();
        }
        VehicleHitIndicatorOverlay.hitVehicle = hitVehicle;
    }

}
