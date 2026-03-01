package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
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
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
        if (!(vehicle instanceof FixedWingVehicle rotaryWingVehicle)
                || !LocalVehiclePlayer.instance.getPlayer().equals(rotaryWingVehicle.getDriver())) {
            return;
        }
        float centerX = (float) screenWidth / 2;
        float centerY = (float) screenHeight / 2;
        LocalVehiclePlayer.ViewType viewType = LocalVehiclePlayer.instance.viewType;
        renderMainInfo(guiGraphics, centerX, centerY, rotaryWingVehicle);
//        renderHeightInfo(guiGraphics, centerX, centerY, rotaryWingVehicle);
        if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON || viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            {
                if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
                    pose.translate(centerX, centerY, 0);
                } else if (viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                    pose.translate(centerX + 75, centerY + 55, 0);
                    pose.scale(0.7f, 0.7f, 0.7f);
                }
//                renderSpeedInfo(guiGraphics, rotaryWingVehicle);
//                renderRollInfo(guiGraphics, centerX, centerY, rotaryWingVehicle, viewType);
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
        Component text = Component.literal("节流阀：")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN))
                .append(Component.literal(throttle <= 100 ? String.valueOf(throttle) : "加力")
                        .withStyle(style -> style.withColor(throttle <= 100 ? ChatFormatting.GREEN : ChatFormatting.RED))
                );
        guiGraphics.drawString(font,
                text,
                leftX, leftY + 12, Color.GREEN);
        int speed = (int) (new Vec3(fixedWingVehicle.getDeltaMovement().x, 0, fixedWingVehicle.getDeltaMovement().z).length() * 72); // 20 * 3.6 = 72
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

//    /**
//     * 速度信息
//     */
//    public static void renderSpeedInfo(GuiGraphics guiGraphics, RotaryWingVehicle rotaryWingVehicle) {
//        Vec3 v = rotaryWingVehicle.relativeRotDirection(rotaryWingVehicle.getDeltaMovement(), true);
//        float arrowLength = (float) (15.0f * v.length() / rotaryWingVehicle.maxAirSpeed);
//        float arrowSize = 4.0f;
//        PoseStack pose = guiGraphics.pose();
//        pose.pushPose();
//        {
//            RenderSystem.enableBlend();
//            RenderSystem.defaultBlendFunc();
//            RenderSystem.setShader(GameRenderer::getPositionColorShader);
//
//            pose.mulPose(Axis.ZP.rotation((float) -Math.toRadians(rotaryWingVehicle.getZRot())));
//            pose.mulPose(Axis.ZP.rotation((float) Math.atan2(-v.x, v.z)));
//
//            // 速度线主干
//            pose.pushPose();
//            {
//                pose.scale(0.5f, 1f, 0.5f);
//                guiGraphics.fill(-1, 0, 1, (int) -arrowLength, 0xFF00FF00);
//            }
//            pose.popPose();
//
//            // 速度线箭头
//            float endY = -arrowLength - 3;
//            BufferBuilder buf = Tesselator.getInstance().getBuilder();
//            buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
//            buf.vertex(pose.last().pose(), 0, endY, 0).color(0, 255, 0, 255).endVertex();
//            buf.vertex(pose.last().pose(), -arrowSize / 2, endY + arrowSize, 0).color(0, 255, 0, 255).endVertex();
//            buf.vertex(pose.last().pose(), arrowSize / 2, endY + arrowSize, 0).color(0, 255, 0, 255).endVertex();
//            Tesselator.getInstance().end();
//
//            RenderSystem.disableBlend();
//        }
//        pose.popPose();
//    }
//
//    /**
//     * 滚转信息
//     */
//    public static void renderRollInfo(GuiGraphics guiGraphics, float centerX, float centerY, RotaryWingVehicle rotaryWingVehicle, LocalVehiclePlayer.ViewType viewType) {
//        PoseStack pose = guiGraphics.pose();
//        pose.pushPose();
//        {
//            if (viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
//                guiGraphics.fill(-17, 0, -13, 1, 0xFF00FF00);
//                guiGraphics.fill(-12, 0, -8, 1, 0xFF00FF00);
//                guiGraphics.fill(-7, 0, -3, 1, 0xFF00FF00);
//                guiGraphics.fill(3, 0, 7, 1, 0xFF00FF00);
//                guiGraphics.fill(8, 0, 12, 1, 0xFF00FF00);
//                guiGraphics.fill(13, 0, 17, 1, 0xFF00FF00);
//                pose.mulPose(Axis.ZP.rotation((float) Math.toRadians(rotaryWingVehicle.getZRot())));
//                pose.scale(1f, 0.6f, 1f);
//                guiGraphics.fill(-11, 0, -2, 1, 0xFF00FF00);
//                guiGraphics.fill(-3, 0, -2, 3, 0xFF00FF00);
//                guiGraphics.fill(2, 0, 3, 3, 0xFF00FF00);
//                guiGraphics.fill(2, 0, 11, 1, 0xFF00FF00);
//            } else if (viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
//                pose.pushPose();
//                {
//                    float arrowSize = 6.0f;
//                    RenderSystem.enableBlend();
//                    RenderSystem.defaultBlendFunc();
//                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
//                    BufferBuilder buf = Tesselator.getInstance().getBuilder();
//                    buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
//                    buf.vertex(pose.last().pose(), -19, 0, 0).color(0, 255, 0, 255).endVertex();
//                    buf.vertex(pose.last().pose(), -19 -arrowSize / 2, -arrowSize / 2, 0).color(0, 255, 0, 255).endVertex();
//                    buf.vertex(pose.last().pose(), -19 -arrowSize / 2, arrowSize / 2, 0).color(0, 255, 0, 255).endVertex();
//                    Tesselator.getInstance().end();
//                    RenderSystem.disableBlend();
//                }
//                pose.popPose();
//                double xRot = -rotaryWingVehicle.getXRot();
//                int range = 50;
//                float interval = 5;
//                float gap = range / interval;
//                int baseY = -range;
//                double value = xRot + range / gap * interval;
//                int rot = (int) (Math.floor(value / interval) * interval);
//                double mod = ((xRot % interval) + interval) % interval;
//                pose.translate(0, mod / interval * gap, 0);
//                pose.mulPose(Axis.ZP.rotation((float) -Math.toRadians(rotaryWingVehicle.getZRot())));
//                guiGraphics.enableScissor(0, (int) (centerY + 21), guiGraphics.guiWidth(), (int) (centerY + 89));
//                while (baseY <= range) {
//                    guiGraphics.fill(-17, baseY, -13, baseY + 1, 0xFF00FF00);
//                    guiGraphics.fill(-12, baseY, -8, baseY + 1, 0xFF00FF00);
//                    guiGraphics.fill(-7, baseY, -3, baseY + 1, 0xFF00FF00);
//                    guiGraphics.fill(3, baseY, 7, baseY + 1, 0xFF00FF00);
//                    guiGraphics.fill(8, baseY, 12, baseY + 1, 0xFF00FF00);
//                    guiGraphics.fill(13, baseY, 17, baseY + 1, 0xFF00FF00);
//                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, rot + "", 32, baseY - 3, 0x00FF00);
//                    baseY += gap;
//                    rot -= interval;
//                }
//                guiGraphics.disableScissor();
//            }
//        }
//        pose.popPose();
//    }

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
