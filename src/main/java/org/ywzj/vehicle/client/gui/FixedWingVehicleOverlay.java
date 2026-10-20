package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.util.VehicleHudText;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

public class FixedWingVehicleOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        if (!LocalVehiclePlayer.instance.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.vehicle;
        if (!(vehicle instanceof FixedWingVehicle fixedWingVehicle)
                || !LocalVehiclePlayer.instance.getPlayer().equals(fixedWingVehicle.getDriver())) {
            return;
        }
        try (VehicleHudLayout layout = VehicleHudLayout.begin(guiGraphics)) {
            float centerX = layout.width() / 2;
            float centerY = VehicleHudLayout.CENTER_Y;
            LocalVehiclePlayer.ViewType viewType = LocalVehiclePlayer.instance.viewType;
            renderMainInfo(guiGraphics, centerX, centerY, fixedWingVehicle);
            if (viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                {
                    RotaryWingVehicleOverlay.renderRollInfo(guiGraphics, partialTick,
                            VehicleHudLayout.operatorAttitudeX(centerX), VehicleHudLayout.OPERATOR_ATTITUDE_Y,
                            1f, fixedWingVehicle, viewType);
                }
                pose.popPose();
            }
        }
    }

    /**
     * 主信息
     */
    public static void renderMainInfo(GuiGraphics guiGraphics, float centerX, float centerY, FixedWingVehicle fixedWingVehicle) {
        float leftX = VehicleHudLayout.telemetryX(centerX);
        float leftY = VehicleHudLayout.TELEMETRY_Y;
        int row = VehicleHudLayout.ROW_HEIGHT;
        int throttle = (int) fixedWingVehicle.getThrottleLevel();
        boolean braking = throttle == 0 && fixedWingVehicle.controlUnit.backward && fixedWingVehicle.onGround();
        Component throttleText = braking ? Component.translatable("ui.brake")
                : throttle > 100 ? Component.translatable("ui.war_emergency_power") : Component.literal(throttle + " %");
        VehicleHudText.telemetry(guiGraphics, Component.translatable("ui.vehicle_hud.throttle"), throttleText, leftX, leftY,
                braking || throttle > 100 ? Color.RED : Color.GREEN);
        int speed = (int) (fixedWingVehicle.getDeltaMovement().length() * 72); // 20 * 3.6 = 72
        VehicleHudText.telemetry(guiGraphics, Component.translatable("ui.vehicle_hud.speed"), Component.literal(speed + " km/h"),
                leftX, leftY + row, Color.GREEN);
        VehicleHudText.telemetry(guiGraphics, Component.translatable("ui.vehicle_hud.altitude"), Component.literal((int) fixedWingVehicle.getY() + " m"),
                leftX, leftY + row * 2, Color.GREEN);
        String fuelTime = VehicleHudText.endurance(fixedWingVehicle.getEnergy(), fixedWingVehicle.energyInfo.energyConsumptionPerTick);
        VehicleHudText.telemetry(guiGraphics, Component.translatable("ui.vehicle_hud.fuel"), Component.literal(fuelTime),
                leftX, leftY + row * 3, Color.GREEN);
    }

    public static String secondsToHms(float seconds) {
        return VehicleHudText.secondsToHms(seconds);
    }

}
