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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.apache.commons.lang3.tuple.Pair;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.NoneVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.Map;
import java.util.Optional;
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
        renderTips(guiGraphics, screenWidth, screenHeight);
    }

    /**
     * 乘员组
     */
    public static void renderCrew(GuiGraphics guiGraphics, int centerX, int centerY, AbstractVehicle vehicle) {
        int x = centerX - 160;
        int y = centerY + guiGraphics.guiHeight() / 5;
        for (int index = 0; index < vehicle.seats.size(); index++) {
            Integer playerId = vehicle.seats.get(index).passengerId;
            Entity entity = null;
            if (playerId != null) {
                entity = LocalVehiclePlayer.instance.getPlayer().level().getEntity(playerId);
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
                    "FUEL:", 90, -40, Color.WHITE);
            guiGraphics.drawString(Minecraft.getInstance().font,
                    String.format("%.1f%%", fuelPercent),
                    118, -40, fuelPercent < 5 ? Color.RED : Color.WHITE);
            guiGraphics.drawString(Minecraft.getInstance().font,
                    "POWER:",
                    90, -30, Color.WHITE);
            guiGraphics.drawString(Minecraft.getInstance().font,
                    String.format("%.1f%%", powerPercent),
                    124, -30, powerPercent < 30 ? Color.RED : Color.WHITE);
        }
        poseStack.popPose();
    }

    public void renderTips(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        guiGraphics.pose().pushPose();
        {
            guiGraphics.pose().translate(centerX, centerY, 0);
            if (localVehiclePlayer.lostControl) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                        Component.translatable("ui.lost_control"),
                        0, -45, Color.RED);
            } else if (localVehiclePlayer.endureTick > 5
                    && (LocalVehiclePlayer.instance.currentG >= 2 || LocalVehiclePlayer.instance.currentG <= -1)) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                        Component.translatable("ui.overload", String.format("%.1f", localVehiclePlayer.currentG)).append("G"),
                        0, -45, Color.RED);
            } else if (!localVehiclePlayer.missiles.isEmpty()) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                        Component.translatable("ui.homing"),
                        0, -45, Color.GREEN);
                tips.clear();
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
                    AABB aabb = vehicle.getBoundingBox();
                    Vec3 pos = new Vec3(Mth.lerp(partialTick, vehicle.xo, vehicle.getX()),
                            Mth.lerp(partialTick, vehicle.yo, vehicle.getY()) + aabb.maxY - aabb.minY,
                            Mth.lerp(partialTick, vehicle.zo, vehicle.getZ()));
                    Vec3 screenPos = VectorUtil.worldToScreen(pos);
                    renderHealth(guiGraphics, screenPos.x, screenPos.y, 90, 5, vehicle, size);
                }
                poseStack.popPose();
                Vec3 eyePosition = player.getEyePosition();
                PartUnit<?> partUnit = VectorUtil.hitPartUnit(vehicle, eyePosition, eyePosition.add(player.getLookAngle().scale(4)));
                if (partUnit instanceof WeaponUnit weaponUnit && weaponUnit.isInteractive()) {
                    renderWeaponList(guiGraphics, weaponUnit);
                } else if (partUnit instanceof DecorationUnit decorationUnit) {
                    renderDecorationTips(guiGraphics, decorationUnit);
                }
            }
        }
    }

    /**
     * 可选武器列表
     */
    private void renderWeaponList(GuiGraphics guiGraphics, WeaponUnit weaponUnit) {
        var weapons = weaponUnit.weapons;
        if (weapons.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int maxNameWidth = 0;
        for (var weapon : weapons) {
            maxNameWidth = Math.max(maxNameWidth, font.width(weaponUnit.proxyWeapon(weapon).getDisplayName().getString()));
        }
        int cardWidth = maxNameWidth + 8;
        int cardHeight = 14;
        int cardPadding = 2;
        int listX = centerX + 24;
        int currentWeaponIndex = weaponUnit.getCurrentWeaponIndex();
        int totalHeight = weapons.size() * (cardHeight + cardPadding) - cardPadding;
        int listY = centerY - totalHeight / 2;
        RenderHelper.fill(guiGraphics, RenderType.guiOverlay(),
                listX - 2, listY - 2,
                listX + cardWidth + 2, listY + totalHeight + 2,
                -512, Color.BG_DARK);
        PoseStack poseStack = guiGraphics.pose();
        for (int i = 0; i < weapons.size(); i++) {
            var weapon = weaponUnit.proxyWeapon(weapons.get(i));
            int cardY = listY + i * (cardHeight + cardPadding);
            boolean selected = (i == currentWeaponIndex);
            int cardBg = selected ? Color.BG_SELECTED : Color.BG_DARK;
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(),
                    listX, cardY,
                    listX + cardWidth, cardY + cardHeight,
                    -512, cardBg);
            String name = weapon.getDisplayName().getString();
            int textColor = selected ? Color.GREEN : Color.WHITE;
            poseStack.pushPose();
            {
                poseStack.translate(listX + 4, cardY + (cardHeight - font.lineHeight) / 2f + 1, 0);
                guiGraphics.drawString(font, name, 0, 0, textColor, true);
                if (i == weapons.size() - 1) {
                    poseStack.translate(-4, 16, 0);
                    guiGraphics.drawString(font, Component.translatable("tips.sneak_use_modding_tool"), 0, 0, Color.WHITE, true);
                }
            }
            poseStack.popPose();
        }
    }

    private void renderDecorationTips(GuiGraphics guiGraphics, DecorationUnit decorationUnit) {
        Optional<BaseDisplay> decorationDisplayOptional = ClientAssetsManager.INSTANCE.getDecorationDisplay(decorationUnit.getDisplayId());
        if (decorationDisplayOptional.isEmpty()) {
            return;
        }
        BaseDisplay decorationDisplay =  decorationDisplayOptional.get();
        String displayId = decorationDisplay.getDisplayId().toString();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int cardWidth = font.width(displayId) + 6;
        int cardHeight = 64;
        int cardPadding = 16;
        int listX = centerX + 32;
        int listY = centerY - cardHeight / 2 + 16;
        RenderHelper.fill(guiGraphics, RenderType.guiOverlay(),
                listX - 2, listY - 2,
                listX + cardWidth + 2, listY + cardPadding + 2,
                -512, Color.BG_DARK);
        RenderHelper.fill(guiGraphics, RenderType.guiOverlay(),
                listX, listY,
                listX + cardWidth, listY + cardPadding,
                -512, Color.BG_DARK);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate(listX + 4, listY + (float) font.lineHeight / 2, 0);
            guiGraphics.drawString(font, decorationDisplay.getDisplayId().toString(), 0, 0, Color.WHITE, true);
            poseStack.translate(-4, 16, 0);
            guiGraphics.drawString(font, Component.translatable("tips.edit_decoration"), 0, 0, Color.WHITE, true);
        }
        poseStack.popPose();
    }

    public static void renderHealth(GuiGraphics guiGraphics, double x, double y, int barWidth, int barHeight, AbstractVehicle vehicle, float size) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            Font font = Minecraft.getInstance().font;
            int nameColor = Color.WHITE;
            float barHalfWidth = (float) barWidth / 2;
            float barHalfHeight = (float) barHeight / 2;
            poseStack.translate(x, y + barHalfHeight - 8, 0);
            poseStack.scale(size, size, size);
            Team team = vehicle.getTeam();
            if (team != null) {
                Integer teamColor = team.getColor().getColor();
                if (teamColor != null) {
                    nameColor = teamColor;
                }
            }
            guiGraphics.drawCenteredString(font, vehicle.getDisplayName(), 0, -14, nameColor);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight, barHalfWidth, barHalfHeight, -512, Color.BG_DARK);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth - 1, -barHalfHeight, -barHalfWidth, barHalfHeight, -512, Color.GRAY);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), barHalfWidth, -barHalfHeight, barHalfWidth + 1, barHalfHeight, -512, Color.GRAY);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight - 1, barHalfWidth, -barHalfHeight, -512, Color.GRAY);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, barHalfHeight, barHalfWidth, barHalfHeight + 1, -512, Color.GRAY);

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
                        -512, Color.WHITE
                );
            }

            poseStack.pushPose();
            {
                String text = String.format("%.0f/%.0f", health, maxHealth);
                poseStack.translate(0, -3.5, 0);
                RenderHelper.drawCenteredString(guiGraphics, font, text, 0, 0, Color.WHITE);
                if (healthDiff > 0) {
                    RenderHelper.drawCenteredString(guiGraphics, font, "-" + String.format("%.2f", healthDiff), barWidth / 2, 1 + barHeight, Color.RED);
                } else if (healthDiff < 0 && !vehicle.isDestroyed()) {
                    RenderHelper.drawCenteredString(guiGraphics, font, "+" + String.format("%.2f", -healthDiff), barWidth / 2, 1 + barHeight, Color.GREEN);
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
