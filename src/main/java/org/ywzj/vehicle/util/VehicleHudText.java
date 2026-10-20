package org.ywzj.vehicle.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class VehicleHudText {

    public static final int LINE_HEIGHT = 12;

    public static int width(String content) {
        return content == null ? 0 : Minecraft.getInstance().font.width(content);
    }

    public static void telemetry(GuiGraphics graphics, Component label, Component value, float x, float y, int color) {
        drawString(graphics, label, x, y, org.ywzj.vehicle.client.render.util.Color.GREEN);
        float columnWidth = Math.max(54, Minecraft.getInstance().font.width(label) + 10);
        drawString(graphics, value, x + columnWidth, y, color);
    }

    public static String truncate(String content, int maxWidth) {
        if (width(content) <= maxWidth) {
            return content;
        }
        String suffix = "...";
        if (width(suffix) > maxWidth) {
            return "";
        }
        return Minecraft.getInstance().font.plainSubstrByWidth(content, maxWidth - width(suffix)) + suffix;
    }

    public static String endurance(float energy, float consumptionPerTick) {
        return consumptionPerTick > 0 ? secondsToHms(energy / consumptionPerTick / 20) : "--:--:--";
    }

    public static String secondsToHms(float seconds) {
        if (!Float.isFinite(seconds)) {
            return "--:--:--";
        }
        long whole = (long) Math.abs(seconds);
        return String.format(Locale.ROOT, "%s%02d:%02d:%02d", seconds < 0 ? "-" : "",
                whole / 3600, (whole % 3600) / 60, whole % 60);
    }

    public static void drawString(GuiGraphics graphics, String content, float x, float y, int color) {
        drawString(graphics, content, x, y, color, true);
    }

    public static void drawString(GuiGraphics graphics, String content, float x, float y, int color, boolean shadow) {
        if (content == null || content.isEmpty()) {
            return;
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.drawString(Minecraft.getInstance().font, content, 0, 0, color, shadow);
        } finally {
            graphics.pose().popPose();
        }
    }

    public static void drawString(GuiGraphics graphics, Component content, float x, float y, int color) {
        drawString(graphics, content, x, y, color, true);
    }

    public static void drawString(GuiGraphics graphics, Component content, float x, float y, int color, boolean shadow) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.drawString(Minecraft.getInstance().font, content, 0, 0, color, shadow);
        } finally {
            graphics.pose().popPose();
        }
    }

    public static void drawCenteredString(GuiGraphics graphics, String content, float x, float y, int color) {
        drawString(graphics, content, x - width(content) / 2f, y, color);
    }

    public static void drawCenteredString(GuiGraphics graphics, Component content, float x, float y, int color) {
        drawString(graphics, content, x - Minecraft.getInstance().font.width(content) / 2f, y, color);
    }

    public static void drawRightAlignedString(GuiGraphics graphics, Component content, float x, float y, int color) {
        drawString(graphics, content, x - Minecraft.getInstance().font.width(content), y, color);
    }

}
