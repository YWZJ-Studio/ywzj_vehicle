package org.ywzj.vehicle.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public final class VehicleHudLayout implements AutoCloseable {

    public static final float HEIGHT = 480;
    public static final int ROW_HEIGHT = 15;
    public static final float CENTER_Y = HEIGHT * 0.47f;
    public static final float TELEMETRY_Y = HEIGHT * 0.42f;
    public static final float OPERATOR_ATTITUDE_Y = TELEMETRY_Y + 4 * ROW_HEIGHT + 44;
    public static final float CREW_Y = HEIGHT * 0.70f;
    private final GuiGraphics graphics;
    private final float scale;

    private VehicleHudLayout(GuiGraphics graphics) {
        this.graphics = graphics;
        scale = graphics.guiHeight() / HEIGHT;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1);
    }

    public static VehicleHudLayout begin(GuiGraphics graphics) {
        return new VehicleHudLayout(graphics);
    }

    public float width() {
        return graphics.guiWidth() / scale;
    }

    public static float telemetryX(float centerX) {
        return centerX - Math.min(220, centerX * 0.56f);
    }

    public static float varioX(float centerX) {
        return centerX + Math.min(150, centerX * 0.38f);
    }

    public static float operatorAttitudeX(float centerX) {
        return telemetryX(centerX) + 22;
    }

    public static float crewX(float centerX) {
        return Math.max(24, telemetryX(centerX) - 48);
    }

    public float upperRightX() {
        float centerX = width() / 2;
        return centerX + Math.min(260, centerX * 0.68f);
    }

    public float weaponRightX() {
        float centerX = width() / 2;
        return centerX + Math.min(320, centerX * 0.88f);
    }

    @Override
    public void close() {
        graphics.pose().popPose();
    }

}
