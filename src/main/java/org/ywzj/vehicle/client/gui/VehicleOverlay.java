package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllKeys;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.NoneVehicle;
import org.ywzj.vehicle.network.message.ServerHitVehicleEvent;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class VehicleOverlay implements LayeredDraw.Layer {

    public static ConcurrentHashMap<Long, Component> tips = new ConcurrentHashMap<>();

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        renderLookAt(guiGraphics, partialTick);
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        if (!localVehiclePlayer.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = localVehiclePlayer.vehicle;
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
            BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.addVertex(poseStack.last().pose(), 0, 23, 0).setColor(0, 255, 0, 255);
            buf.addVertex(poseStack.last().pose(), -3, 29, 0).setColor(0, 255, 0, 255);
            buf.addVertex(poseStack.last().pose(), 3, 29, 0).setColor(0, 255, 0, 255);
            BufferUploader.drawWithShader(buf.buildOrThrow());
            RenderSystem.disableBlend();
        }
        poseStack.popPose();
    }

    private void renderBaseInfo(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate((float) screenWidth / 2, screenHeight + 16, 0);
            renderHealth(guiGraphics, 0, -20, 120, 5, vehicle, 1);
            Font font = Minecraft.getInstance().font;
            float energyCapacity = vehicle.energyInfo.energyCapacity;
            float fuelPercent = energyCapacity > 0 ? vehicle.getEnergy() / energyCapacity * 100 : 0;
            float powerPercent = vehicle.getPower();
            renderFuelAndPower(guiGraphics, font, fuelPercent, powerPercent);
        }
        poseStack.popPose();
    }

    private void renderFuelAndPower(GuiGraphics guiGraphics, Font font, float fuelPercent, float powerPercent) {
        PoseStack poseStack = guiGraphics.pose();
        String fuelText = String.format("FUEL %.1f%%", fuelPercent);
        String powerText = String.format("POWER %.1f%%", powerPercent);
        float scale = 0.4F;
        poseStack.pushPose();
        {
            poseStack.translate(64, -29, 0);
            poseStack.scale(scale, scale, 1);
            guiGraphics.drawString(font, fuelText, 0, 0,
                    fuelPercent < 5 ? Color.RED : Color.WHITE, true);
            guiGraphics.drawString(font, powerText, 0, font.lineHeight,
                    powerPercent < 30 ? Color.RED : Color.WHITE, true);
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
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        Player player = instance.getPlayer();
        AbstractVehicle vehicle = instance.lookAtVehicle;
        PartUnit<?> partUnit = instance.lookAtPartUnit;
        if (vehicle != null) {
            double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(instance.lookAtVehicle.getEyePosition());
            if (distance > showVehicleInfoDistance) {
                return;
            }
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            {
                AABB aabb = vehicle.getBoundingBox();
                Vec3 pos = new Vec3(Mth.lerp(partialTick, vehicle.xo, vehicle.getX()),
                        Mth.lerp(partialTick, vehicle.yo, vehicle.getY()) + aabb.maxY - aabb.minY,
                        Mth.lerp(partialTick, vehicle.zo, vehicle.getZ()));
                Vec3 screenPos = VectorUtil.worldToScreen(pos);
                if (screenPos.z >= 0) {
                    float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.5f * Math.max((512 - distance) / 512, 0.1), 0.66, 1);
                    renderHealth(guiGraphics, screenPos.x, screenPos.y, 90, 5, vehicle, size);
                    Item mainHandItem = player.getMainHandItem().getItem();
                    if (partUnit != null && partUnit.getHealth() >= 0
                            && (AllKeys.INSPECT_VEHICLE.isDown() || mainHandItem == AllItems.REPAIR_TOOL.get() || mainHandItem == AllItems.MODDING_TOOL.get())) {
                        renderPartHealth(guiGraphics, partUnit, partialTick, size);
                    }
                }
            }
            poseStack.popPose();
            if (partUnit instanceof WeaponUnit weaponUnit && weaponUnit.isInteractive()) {
                renderWeaponList(guiGraphics, weaponUnit);
            } else if (partUnit instanceof DecorationUnit decorationUnit) {
                renderDecorationTips(guiGraphics, decorationUnit);
            }
        }
        if (!VehicleHitIndicatorOverlay.events.isEmpty()) {
            ServerHitVehicleEvent event = VehicleHitIndicatorOverlay.events.get(0);
            Entity hitEntity = player.level().getEntity(event.entityId);
            if (hitEntity == null || hitEntity == vehicle) {
                return;
            }
            if (hitEntity instanceof AbstractVehicle hitVehicle && !hitVehicle.equals(player.getVehicle())) {
                AABB aabb = hitVehicle.getBoundingBox();
                Vec3 pos = new Vec3(Mth.lerp(partialTick, hitVehicle.xo, hitVehicle.getX()),
                        Mth.lerp(partialTick, hitVehicle.yo, hitVehicle.getY()) + aabb.maxY - aabb.minY,
                        Mth.lerp(partialTick, hitVehicle.zo, hitVehicle.getZ()));
                Vec3 screenPos = VectorUtil.worldToScreen(pos);
                if (screenPos.z >= 0) {
                    double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(hitVehicle.getEyePosition());
                    float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.5f * org.joml.Math.max((512 - distance) / 512, 0.1), 0.66, 1);
                    VehicleOverlay.renderHealth(guiGraphics, screenPos.x, screenPos.y, 90, 5, hitVehicle, size);
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
        int nameColor = Color.WHITE;
        Team team = vehicle.getTeam();
        if (team != null) {
            Integer teamColor = team.getColor().getColor();
            if (teamColor != null) {
                nameColor = teamColor;
            }
        }
        renderHealth(guiGraphics, x, y, barWidth, barHeight, vehicle.getDisplayName(), nameColor,
                vehicle.getHealth(), vehicle.getMaxHealth(), vehicle.healthO, vehicle.hurtTick,
                vehicle.isDestroyed(), size);
    }

    private static void renderPartHealth(GuiGraphics guiGraphics, PartUnit<?> partUnit, float partialTick, float size) {
        int nameColor = Color.WHITE;
        Team team = partUnit.getVehicle().getTeam();
        if (team != null) {
            Integer teamColor = team.getColor().getColor();
            if (teamColor != null) {
                nameColor = teamColor;
            }
        }
        VehicleCubeOBB largestCube = partUnit.getLargestCube();
        if (largestCube == null) {
            return;
        }
        Vec3 position = largestCube.position;
        if (position == null) {
            position = new Vec3(largestCube.obb().center());
        } else if (largestCube.positionO != null) {
            position = largestCube.positionO.lerp(position, partialTick);
        }
        Vec3 screenPos = VectorUtil.worldToScreen(position);
        if (screenPos.z < 0) {
            return;
        }
        renderHealth(guiGraphics, screenPos.x, screenPos.y, 60, 4, partUnit.getName(), nameColor,
                partUnit.getHealth(), partUnit.getMaxHealth(), partUnit.healthO, partUnit.hurtTick,
                partUnit.isDestroyed(), size);
    }

    private static void renderHealth(GuiGraphics guiGraphics, double x, double y, int barWidth, int barHeight,
                                     Component name, int nameColor, float health, float maxHealth,
                                     float healthO, int hurtTick, boolean destroyed, float size) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            Font font = Minecraft.getInstance().font;
            float barHalfWidth = (float) barWidth / 2;
            float barHalfHeight = (float) barHeight / 2;
            poseStack.translate(x, y + barHalfHeight - 8, 0);
            poseStack.scale(size, size, size);
            guiGraphics.drawCenteredString(font, name, 0, -14, nameColor);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight, barHalfWidth, barHalfHeight, -512, Color.BG_DARK);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth - 1, -barHalfHeight, -barHalfWidth, barHalfHeight, -512, Color.GRAY);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), barHalfWidth, -barHalfHeight, barHalfWidth + 1, barHalfHeight, -512, Color.GRAY);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight - 1, barHalfWidth, -barHalfHeight, -512, Color.GRAY);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, barHalfHeight, barHalfWidth, barHalfHeight + 1, -512, Color.GRAY);

            float percent = maxHealth > 0 ? Math.max(0, Math.min(1, health / maxHealth)) : 0f;
            float hurtT = Math.max(0f, Math.min(1f, hurtTick / 10f));
            float rawLastPercent = maxHealth > 0 ? Math.max(0, Math.min(1, healthO / maxHealth)) : percent;
            float lastPercent = Mth.lerp(hurtT, percent, rawLastPercent);

            int red, green;
            if (destroyed) {
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
                float healthDiff = healthO - health;
                if (healthDiff > 0) {
                    RenderHelper.drawCenteredString(guiGraphics, font, "-" + String.format("%.2f", healthDiff), barWidth / 2, 1 + barHeight, Color.RED);
                } else if (healthDiff < 0 && !destroyed) {
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
