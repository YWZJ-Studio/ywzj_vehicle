package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.client.render.util.Color;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

public class FixedWingVehicleOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LocalVehiclePlayer.instance.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.vehicle;
        if (!(vehicle instanceof FixedWingVehicle fixedWingVehicle)
                || !LocalVehiclePlayer.instance.getPlayer().equals(fixedWingVehicle.getDriver())) {
            return;
        }
        float centerX = (float) screenWidth / 2;
        float centerY = (float) screenHeight / 2;
        LocalVehiclePlayer.ViewType viewType = LocalVehiclePlayer.instance.viewType;
        renderMainInfo(guiGraphics, centerX, centerY, fixedWingVehicle);
        if (viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            {
                RotaryWingVehicleOverlay.renderRollInfo(guiGraphics, partialTick, centerX - 95, centerY + 75, 0.7f, fixedWingVehicle, viewType);
            }
            pose.popPose();
        }
    }

    /**
     * 主信息
     */
    public static void renderMainInfo(GuiGraphics guiGraphics, float centerX, float centerY, FixedWingVehicle fixedWingVehicle) {
        int leftX = (int) (centerX - 120);
        int leftY = (int) (centerY - 21);
        // 信息
        var font = Minecraft.getInstance().font;
        int throttle = (int) fixedWingVehicle.getThrottleLevel();
        MutableComponent text = Component.translatable("ui.vehicle_rotary_wing.throttle_level")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN));
        if (throttle == 0 && fixedWingVehicle.controlUnit.backward && fixedWingVehicle.onGround()) {
            text.append(Component.translatable("ui.brake")
                    .withStyle(style -> style.withColor(ChatFormatting.RED)));
        } else {
            text.append(Component.translatable(throttle <= 100 ? String.valueOf(throttle) : "ui.war_emergency_power")
                    .withStyle(style -> style.withColor(throttle <= 100 ? ChatFormatting.GREEN : ChatFormatting.RED)));
        }
        guiGraphics.drawString(font,
                text,
                leftX, leftY + 12, Color.GREEN);
        int speed = (int) (fixedWingVehicle.getDeltaMovement().length() * 72); // 20 * 3.6 = 72
        guiGraphics.drawString(font,
                Component.translatable("ui.vehicle_rotary_wing.speed", speed),
                leftX, leftY + 24, Color.GREEN);
        guiGraphics.drawString(font,
                Component.translatable("ui.vehicle_rotary_wing.altitude", (int) fixedWingVehicle.getY()),
                leftX, leftY + 36, Color.GREEN);
        String fuelTime = secondsToHms(fixedWingVehicle.getEnergy() / fixedWingVehicle.energyInfo.energyConsumptionPerTick / 20);
        guiGraphics.drawString(font,
                Component.translatable("ui.vehicle_rotary_wing.fuel", fuelTime),
                leftX, leftY + 48, Color.GREEN);
    }

    public static String secondsToHms(float seconds) {
        boolean neg = seconds < 0;
        float abs = Math.abs(seconds);
        long whole = (long) abs;
        long hh = whole / 3600;
        long mm = (whole % 3600) / 60;
        long ss = whole % 60;
        return String.format("%s%02d:%02d:%02d", neg ? "-" : "", hh, mm, ss);
    }

}
