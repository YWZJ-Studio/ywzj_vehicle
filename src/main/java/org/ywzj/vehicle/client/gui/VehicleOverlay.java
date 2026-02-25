package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.apache.commons.lang3.tuple.Pair;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.NoneVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VehicleOverlay implements IGuiOverlay {

    public static ConcurrentHashMap<Long, Component> tips = new ConcurrentHashMap<>();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        renderLookAt(guiGraphics, partialTick);
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        if (!localVehiclePlayer.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = localVehiclePlayer.getVehicle();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        renderCrew(guiGraphics, centerX, centerY, vehicle);
        if (localVehiclePlayer.viewType != LocalVehiclePlayer.ViewType.THIRD_PERSON) {
            renderCompassBar(guiGraphics, screenWidth);
        }
        if (!(vehicle instanceof NoneVehicle)) {
            renderBaseInfo(guiGraphics, screenWidth, screenHeight, vehicle);
        }
        renderTips(guiGraphics, screenWidth, screenHeight, vehicle);
    }

    /**
     * 乘员组
     */
    public static void renderCrew(GuiGraphics guiGraphics, int centerX, int centerY, AbstractVehicle vehicle) {
        int x = centerX - 140;
        int y = centerY + guiGraphics.guiHeight() / 5;
        for (int index = 0; index < vehicle.seats.size(); index++) {
            Integer playerId = vehicle.seats.get(index).passengerId;
            Entity entity = null;
//            PartUnit<?> partUnit = null;
            if (playerId != null) {
                entity = LocalVehiclePlayer.instance.getPlayer().level().getEntity(playerId);
//                if (entity instanceof LivingEntity livingEntity) {
//                    partUnit = vehicle.getOwnOperatorUnit(livingEntity);
//                }
            }
            String info = "[]";
            if (entity != null) {
                info = "[" + entity.getDisplayName().getString() + "]";
            }
            guiGraphics.drawString(Minecraft.getInstance().font, info, x, y, Color.GREEN);
            y += 10;
        }
    }

    /**
     * 罗盘
     */
    public static void renderCompassBar(GuiGraphics guiGraphics, int screenWidth) {
        float yaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();
        Font font = Minecraft.getInstance().font;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            RenderSystem.enableBlend();
            float centerX = screenWidth / 2f;
            poseStack.translate(centerX - 0.5f, 12, 0);
            poseStack.pushPose();
            {
                guiGraphics.enableScissor((int) (centerX - 120), 0, (int) (centerX + 120), 40);
                poseStack.translate(-yaw * 4, 0, 0);
                for (int x = -225; x <= 225; x += 5) {
                    switch (x) {
                        case -135, 225 -> renderDirection(guiGraphics, poseStack, font, x, "NE");
                        case -90 -> renderDirection(guiGraphics, poseStack, font, x, "E");
                        case -45 -> renderDirection(guiGraphics, poseStack, font, x, "SE");
                        case 0 -> renderDirection(guiGraphics, poseStack, font, x, "S");
                        case 45 -> renderDirection(guiGraphics, poseStack, font, x, "SW");
                        case 90 -> renderDirection(guiGraphics, poseStack, font, x, "W");
                        case 135, -225 -> renderDirection(guiGraphics, poseStack, font, x, "NW");
                        case 180, -180 -> renderDirection(guiGraphics, poseStack, font, x, "N");
                        default -> {
                            if (x % 15 == 0) {
                                int s = x > 180 ? x - 360 : (x < -180 ? x + 360 : x);
                                renderDirection(guiGraphics, poseStack, font, x, s + "");
                            } else {
                                guiGraphics.vLine(x * 4, 0, 6, Color.GREEN);
                            }
                        }
                    }
                }
                guiGraphics.disableScissor();
            }
            poseStack.popPose();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(poseStack.last().pose(), 0, 23, 0).color(0, 255, 0, 255).endVertex();
            buf.vertex(poseStack.last().pose(), -3, 29, 0).color(0, 255, 0, 255).endVertex();
            buf.vertex(poseStack.last().pose(), 3, 29, 0).color(0, 255, 0, 255).endVertex();
            Tesselator.getInstance().end();
            RenderSystem.disableBlend();
        }
        poseStack.popPose();
    }

    private void renderBaseInfo(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate((float) screenWidth / 2, screenHeight + 5, 0);
            renderHealth(guiGraphics, 0, -20, 120, 10, vehicle, 1);
            poseStack.scale(0.7f, 0.7f, 0.7f);
            float fuelPercent = vehicle.getEnergy() / vehicle.energyInfo.energyCapacity * 100;
            float powerPercent = vehicle.getPower();
            guiGraphics.drawString(Minecraft.getInstance().font,
                    "FUEL:", 90, -40, 0xFFFFFFFF);
            guiGraphics.drawString(Minecraft.getInstance().font,
                    String.format("%.1f%%", fuelPercent),
                    118, -40, fuelPercent < 5 ? 0xFFFF0000 : 0xFFFFFFFF);
            guiGraphics.drawString(Minecraft.getInstance().font,
                    "POWER:",
                    90, -30, 0xFFFFFFFF);
            guiGraphics.drawString(Minecraft.getInstance().font,
                    String.format("%.1f%%", powerPercent),
                    124, -30, powerPercent < 30 ? 0xFFFF0000 : 0xFFFFFFFF);
        }
        poseStack.popPose();
    }

    public void renderTips(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        guiGraphics.pose().pushPose();
        {
            guiGraphics.pose().translate(centerX, centerY, 0);
            if (LocalVehiclePlayer.instance.controllingMissiles.stream().anyMatch(Entity::isAlive)) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("ui.homing"), 0, -45, Color.GREEN);
                tips.clear();
            }
            if (vehicle.warningReceiver != null) {
                if (vehicle.warningReceiver.missileLaunchWarn) {
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("ui.missile_incoming"), 0, -55, Color.RED);
                } else if (vehicle.warningReceiver.radarLockWarn) {
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("ui.being_locked"), 0, -55, Color.RED);
                }
            }
            if (!tips.isEmpty()) {
                for (Map.Entry<Long, Component> tip : tips.entrySet()) {
                    if (tip.getKey() + 3000 < System.currentTimeMillis()) {
                        tips.remove(tip.getKey());
                        continue;
                    }
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, tip.getValue(), 0, -45, Color.RED);
                }
            }
        }
        guiGraphics.pose().popPose();
    }

    private void renderLookAt(GuiGraphics guiGraphics, float partialTick) {
        double showVehicleInfoDistance = AllConfigs.server.showVehicleInfoDistance.get();
        if (showVehicleInfoDistance <= 0) {
            return;
        }
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        float rot = 0;
        if (localVehiclePlayer.onVehicle()) {
            rot = localVehiclePlayer.viewType != LocalVehiclePlayer.ViewType.SCOPE ? LocalVehiclePlayer.CAMERA_UPWARD_ANGLE : 0;
        }
        Player player = localVehiclePlayer.getPlayer();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        float xRot = camera.getXRot() - rot;
        float yRot = camera.getYRot();
        Vec3 start = camera.getPosition();
        Vec3 end = start.add(VectorUtil
                .rotToVec(xRot, yRot)
                .normalize()
                .scale(LocalVehiclePlayer.renderDistance()));
        Pair<Entity, Vec3> hitResult = VectorUtil.hitObbPosition(player, start, end);
        if (hitResult != null) {
            Entity entity = hitResult.getLeft();
            if (entity instanceof AbstractVehicle vehicle && !vehicle.equals(player.getVehicle())) {
                double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(vehicle.getEyePosition());
                if (distance > showVehicleInfoDistance) {
                    return;
                }
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                {
                    float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.5f * Math.max((512 - distance) / 512, 0.1), 0.66, 1);
                    VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
                    Vec3 pos = new Vec3(Mth.lerp(partialTick, vehicle.xo, vehicle.getX()),
                            Mth.lerp(partialTick, vehicle.yo, vehicle.getY()) + mainCubeOBB.getHeight(),
                            Mth.lerp(partialTick, vehicle.zo, vehicle.getZ()));
                    Vec3 screenPos = VectorUtil.worldToScreen(pos);
                    renderHealth(guiGraphics, screenPos.x, screenPos.y, 90, 5, vehicle, size);
                }
                poseStack.popPose();
            }
        }
    }

    public void renderHealth(GuiGraphics guiGraphics, double x, double y, int barWidth, int barHeight, AbstractVehicle vehicle, float size) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            Font font = Minecraft.getInstance().font;
            int bgColor = 0xAA000000;
            float barHalfWidth = (float) barWidth / 2;
            float barHalfHeight = (float) barHeight / 2;
            poseStack.translate(x, y + barHalfHeight - 8, 0);
            poseStack.scale(size, size, size);
            guiGraphics.drawCenteredString(font, vehicle.getDisplayName(), 0, -14, 0xFFFFFFFF);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight, barHalfWidth, barHalfHeight, -512, bgColor);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth - 1, -barHalfHeight, -barHalfWidth, barHalfHeight, -512, 0xFF999999);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), barHalfWidth, -barHalfHeight, barHalfWidth + 1, barHalfHeight, -512, 0xFF999999);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight - 1, barHalfWidth, -barHalfHeight, -512, 0xFF999999);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, barHalfHeight, barHalfWidth, barHalfHeight + 1, -512, 0xFF999999);

            float health = vehicle.getHealth();
            float maxHealth = vehicle.getMaxHealth();
            float uiHealth = vehicle.uiHealth;
            int hurtTime = vehicle.hurtTime;

            float percent = maxHealth > 0 ? Math.max(0, Math.min(1, health / maxHealth)) : 0f;
            float hurtT = Math.max(0f, Math.min(1f, hurtTime / 10f));
            float rawLastPercent = maxHealth > 0 ? Math.max(0, Math.min(1, uiHealth / maxHealth)) : percent;
            float lastPercent = Mth.lerp(hurtT, percent, rawLastPercent);
            float healthDiff = uiHealth - health;

            int red, green;
            if (vehicle.isDestroyed()) {
                red = 255;
                green = 64;
            } else {
                if (percent > 0.5f) {
                    red = (int) (255 * (1.0f - (percent - 0.5f) * 2f));
                    green = 255;
                } else {
                    red = 255;
                    green = (int) (255 * (percent * 2f));
                }
            }
            int barColor = (0xFF << 24) | (red << 16) | (green << 8); // ARGB

            int filledWidth = (int) (barWidth * percent);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight, -barHalfWidth + filledWidth, barHalfHeight, -512, barColor);
            int lastFilledWidth = (int) (barWidth * lastPercent);
            if (lastFilledWidth != filledWidth) {
                RenderHelper.fill(
                        guiGraphics, RenderType.guiOverlay(),
                        -barHalfWidth + Math.min(filledWidth, lastFilledWidth), -barHalfHeight,
                        -barHalfWidth + Math.max(filledWidth, lastFilledWidth), barHalfHeight,
                        -512, 0xFFFFFFFF
                );
            }

            poseStack.pushPose();
            {
                String text = String.format("%.0f/%.0f", health, maxHealth);
                poseStack.translate(0, -3.5, 0);
                RenderHelper.drawCenteredString(guiGraphics, font, text, 0, 0, 0xFFFFFFFF);
                if (healthDiff > 0) {
                    RenderHelper.drawCenteredString(guiGraphics, font, "-" + String.format("%.2f", healthDiff), barWidth / 2, 1 + barHeight, 0xFFFF0000);
                } else if (healthDiff < 0 && !vehicle.isDestroyed()) {
                    RenderHelper.drawCenteredString(guiGraphics, font, "+" + String.format("%.2f", -healthDiff), barWidth / 2, 1 + barHeight, 0xFF00FF00);
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void renderDirection(GuiGraphics graphics, PoseStack poseStack, Font font, int x, String s) {
        graphics.vLine(x * 4, 0, 8, Color.GREEN);
        poseStack.translate(1f, 0, 0);
        RenderHelper.drawCenteredString(graphics, font, s, x * 4, 12, Color.GREEN);
        poseStack.translate(-1f, 0, 0);
    }

}
