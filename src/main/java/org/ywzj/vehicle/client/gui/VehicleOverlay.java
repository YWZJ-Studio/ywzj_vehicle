package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class VehicleOverlay implements IGuiOverlay {

    public static int color = 0xFF00FF00;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        if (!instance.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = instance.getVehicle();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        renderCrew(guiGraphics, centerX, centerY, vehicle);
        if (instance.viewType != LocalVehiclePlayer.ViewType.THIRD_PERSON) {
            renderCompassBar(guiGraphics, screenWidth, vehicle);
        }
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
            PartUnit<?> partUnit = null;
            if (playerId != null) {
                entity = LocalVehiclePlayer.instance.getPlayer().level().getEntity(playerId);
                if (entity instanceof LivingEntity livingEntity) {
                    partUnit = vehicle.getOwnOperatorUnit(livingEntity);
                }
            }
            String info = "[]";
            if (entity != null) {
                info = "[" + entity.getDisplayName().getString() + "] " + (partUnit == null ? "" : partUnit.getName().getString());
            }
            guiGraphics.drawString(Minecraft.getInstance().font, info, x, y, color);
            y += 10;
        }
    }

    /**
     * 罗盘
     */
    public static void renderCompassBar(GuiGraphics guiGraphics, int screenWidth, AbstractVehicle vehicle) {
        PartUnit<?> partUnit = vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer());
        if (partUnit instanceof WeaponUnit weaponUnit) {
            float yaw = weaponUnit.worldRot().y;
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
                                    guiGraphics.vLine(x * 4, 0, 6, color);
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

    public static void renderDirection(GuiGraphics graphics, PoseStack poseStack, Font font, int x, String s) {
        graphics.vLine(x * 4, 0, 8, color);
        poseStack.translate(1f, 0, 0);
        graphics.drawCenteredString(font, s, x * 4, 12, color);
        poseStack.translate(-1f, 0, 0);
    }

}
