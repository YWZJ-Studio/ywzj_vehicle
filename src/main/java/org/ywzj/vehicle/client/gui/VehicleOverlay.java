package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
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
import org.ywzj.vehicle.util.VehicleHudText;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
import org.ywzj.vehicle.vehicle.passenger.WarningReceiver;
import org.ywzj.vehicle.vehicle.pojo.WarnType;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        try (VehicleHudLayout layout = VehicleHudLayout.begin(guiGraphics)) {
            float leftX = VehicleHudLayout.crewX(layout.width() / 2);
            int rowHeight = VehicleHudText.LINE_HEIGHT;
            int maxRows = Math.max(1, (int) ((VehicleHudLayout.HEIGHT - 18 - VehicleHudText.LINE_HEIGHT
                    - VehicleHudLayout.CREW_Y) / rowHeight) + 1);
            int columns = Math.max(1, (vehicle.seats.size() + maxRows - 1) / maxRows);
            int rows = Math.max(1, (vehicle.seats.size() + columns - 1) / columns);
            int columnWidth = Math.min(80, (int) ((layout.width() / 2 - 72 - leftX) / columns));
            float topY = Math.min(VehicleHudLayout.CREW_Y,
                    VehicleHudLayout.HEIGHT - 18 - VehicleHudText.LINE_HEIGHT - (rows - 1) * rowHeight);
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
                float x = leftX + index / rows * columnWidth;
                float y = topY + index % rows * rowHeight;
                VehicleHudText.drawString(guiGraphics, VehicleHudText.truncate(info, columnWidth - 8), x, y, Color.GREEN);
            }
        }
    }

    /**
     * 罗盘
     */
    public static void renderCompassBar(GuiGraphics guiGraphics, int screenWidth) {
        try (VehicleHudLayout layout = VehicleHudLayout.begin(guiGraphics)) {
            float yaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            {
                RenderSystem.enableBlend();
                float centerX = layout.width() / 2;
                poseStack.translate(centerX, 32, 0);
                poseStack.pushPose();
                {
                    float hudScale = guiGraphics.guiHeight() / VehicleHudLayout.HEIGHT;
                    guiGraphics.enableScissor((int) ((centerX - 120) * hudScale), (int) (30 * hudScale),
                            (int) ((centerX + 120) * hudScale), (int) (72 * hudScale));
                    poseStack.translate(-yaw * 4 - 0.5f, 0, 0);
                    for (int x = -225; x <= 225; x += 5) {
                        switch (x) {
                            case -135, 225 -> renderDirection(guiGraphics, poseStack, x, "NE");
                            case -90 -> renderDirection(guiGraphics, poseStack, x, "E");
                            case -45 -> renderDirection(guiGraphics, poseStack, x, "SE");
                            case 0 -> renderDirection(guiGraphics, poseStack, x, "S");
                            case 45 -> renderDirection(guiGraphics, poseStack, x, "SW");
                            case 90 -> renderDirection(guiGraphics, poseStack, x, "W");
                            case 135, -225 -> renderDirection(guiGraphics, poseStack, x, "NW");
                            case 180, -180 -> renderDirection(guiGraphics, poseStack, x, "N");
                            default -> {
                                if (x % 15 == 0) {
                                    int s = x > 180 ? x - 360 : (x < -180 ? x + 360 : x);
                                    renderDirection(guiGraphics, poseStack, x, s + "");
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
    }

    private void renderBaseInfo(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.translate((float) screenWidth / 2, screenHeight + 16, 0);
            renderHealth(guiGraphics, 0, -20, 120, 5, vehicle, 1);

            float energyCapacity = vehicle.energyInfo.energyCapacity;
            float fuelPercent = energyCapacity > 0 ? vehicle.getEnergy() / energyCapacity * 100 : 0;
            float powerPercent = vehicle.getPower();
            renderFuelAndPower(guiGraphics, fuelPercent, powerPercent);
        }
        poseStack.popPose();
    }

    private void renderFuelAndPower(GuiGraphics guiGraphics, float fuelPercent, float powerPercent) {
        PoseStack poseStack = guiGraphics.pose();
        Component fuelText = Component.translatable("ui.vehicle_hud.fuel").append(String.format(java.util.Locale.ROOT, " %.1f%%", fuelPercent));
        Component powerText = Component.translatable("ui.vehicle_hud.power").append(String.format(java.util.Locale.ROOT, " %.1f%%", powerPercent));
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        try {
            poseStack.translate(64, -29, 0);
            poseStack.scale(0.4f, 0.4f, 1);
            guiGraphics.drawString(font, fuelText, 0, 0,
                    fuelPercent < 5 ? Color.RED : Color.WHITE, true);
            guiGraphics.drawString(font, powerText, 0, font.lineHeight,
                    powerPercent < 30 ? Color.RED : Color.WHITE, true);
        } finally {
            poseStack.popPose();
        }
    }

    public void renderTips(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        Set<Component> alerts = new LinkedHashSet<>();
        WarningReceiver warningReceiver = localVehiclePlayer.vehicle.warningReceiver;
        if (warningReceiver != null) {
            if (warningReceiver.targets.values().stream().anyMatch(target -> target.warnType() == WarnType.MISSILE_LAUNCH)) {
                alerts.add(Component.translatable("ui.missile_incoming"));
            }
            if (warningReceiver.targets.values().stream().anyMatch(target -> target.warnType() == WarnType.RADAR_LOCK)) {
                alerts.add(Component.translatable("ui.being_locked"));
            }
        }
        if (localVehiclePlayer.lostControl) {
            alerts.add(Component.translatable("ui.lost_control"));
        }
        if (localVehiclePlayer.endureTick > 5
                && (localVehiclePlayer.displayG >= 2 || localVehiclePlayer.displayG <= -1)) {
            alerts.add(Component.translatable("ui.overload", String.format(java.util.Locale.ROOT, "%.1f", localVehiclePlayer.displayG)).append("G"));
        }
        if (!localVehiclePlayer.missiles.isEmpty()) {
            alerts.add(Component.translatable("ui.homing").withStyle(ChatFormatting.GREEN));
        }
        long now = System.currentTimeMillis();
        tips.entrySet().removeIf(tip -> now - tip.getKey() >= 3000);
        tips.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).forEach(alerts::add);
        if (alerts.isEmpty()) {
            return;
        }
        try (VehicleHudLayout layout = VehicleHudLayout.begin(guiGraphics)) {
            float centerX = layout.width() / 2;
            float centerY = VehicleHudLayout.CENTER_Y;
            guiGraphics.pose().pushPose();
            try {
                float topY = centerY - 45 - (alerts.size() - 1) * VehicleHudLayout.ROW_HEIGHT * 1.2f;
                guiGraphics.pose().translate(centerX, topY, 0);
                guiGraphics.pose().scale(1.2f, 1.2f, 1);
                int row = 0;
                for (Component alert : alerts) {
                    VehicleHudText.drawCenteredString(guiGraphics, alert, 0, row * VehicleHudLayout.ROW_HEIGHT, Color.RED);
                    row++;
                }
            } finally {
                guiGraphics.pose().popPose();
            }
        }
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

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int maxNameWidth = 0;
        for (var weapon : weapons) {
            maxNameWidth = Math.max(maxNameWidth, VehicleHudText.width(weaponUnit.proxyWeapon(weapon).getDisplayName().getString()));
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
                poseStack.translate(listX + 4, cardY + (cardHeight - mc.font.lineHeight) / 2f + 1, 0);
                VehicleHudText.drawString(guiGraphics, name, 0, 0, textColor, true);
                if (i == weapons.size() - 1) {
                    poseStack.translate(-4, 16, 0);
                    VehicleHudText.drawString(guiGraphics, Component.translatable("tips.sneak_use_modding_tool"), 0, 0, Color.WHITE, true);
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

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int cardWidth = VehicleHudText.width(displayId) + 6;
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
            poseStack.translate(listX + 4, listY + (float) mc.font.lineHeight / 2, 0);
            VehicleHudText.drawString(guiGraphics, decorationDisplay.getDisplayId().toString(), 0, 0, Color.WHITE, true);
            poseStack.translate(-4, 16, 0);
            VehicleHudText.drawString(guiGraphics, Component.translatable("tips.edit_decoration"), 0, 0, Color.WHITE, true);
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

    public static void renderDirection(GuiGraphics graphics, PoseStack poseStack, int x, String s) {
        graphics.vLine(x * 4, 0, 8, Color.GREEN);
        poseStack.translate(1f, 0, 0);
        VehicleHudText.drawCenteredString(graphics, s, x * 4, 12, Color.GREEN);
        poseStack.translate(-1f, 0, 0);
    }

}
