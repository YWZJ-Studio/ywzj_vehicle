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
import org.ywzj.vehicle.entity.vehicle.HelicopterVehicle;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PartUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

public class ScopeOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!LocalVehiclePlayer.instance.onVehicle() || LocalVehiclePlayer.instance.viewType != LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
        AbstractVehicle vehicle = LocalVehiclePlayer.instance.getVehicle();
        if (vehicle instanceof WheeledVehicle || vehicle instanceof TrackedVehicle) {
            renderGroundVehicle(guiGraphics, screenWidth, screenHeight, vehicle);
        } else if (vehicle instanceof HelicopterVehicle helicopterVehicle) {
            renderHelicopter(guiGraphics, screenWidth, screenHeight, helicopterVehicle);
        }
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        // 成员组
        int x = centerX - guiGraphics.guiWidth() / 3;
        int y = centerY + guiGraphics.guiHeight() / 8;
        for (int index = 0; index < vehicle.seats; index++) {
            Integer playerId = vehicle.passengerIdsBySeat.get(index);
            Entity entity = null;
            PartUnit partUnit = null;
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
            guiGraphics.drawString(Minecraft.getInstance().font, info, x, y, 0xFF00FF00);
            y += 10;
        }
    }

    public void renderGroundVehicle(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        // 屏幕中心点
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        // 十字线长度
        int size = 18;
        // 颜色 (ARGB) 红色不透明
        int color = 0xFFFF0000;
        // 画水平线
        guiGraphics.fill(centerX - size, centerY, centerX + size + 1, centerY + 1, color);
        // 画垂直线
        guiGraphics.fill(centerX, centerY - size, centerX + 1, centerY + size + 1, color);
    }

    public void renderHelicopter(GuiGraphics guiGraphics, int screenWidth, int screenHeight, HelicopterVehicle vehicle) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        HelicopterControlOverlay.renderMainInfo(guiGraphics, centerX, centerY, vehicle);
        HelicopterControlOverlay.renderHeightInfo(guiGraphics, centerX, centerY, vehicle);
        guiGraphics.pose().pushPose();
        {
            guiGraphics.pose().translate(centerX, centerY, 0);
            VehicleCrossHairOverlay.drawSquare(guiGraphics, 0, 0, 5, 0xFF00FF00);
            guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
            guiGraphics.fill(0, -32, 1, -8, 0xFF00FF00);
            guiGraphics.fill(0, 8, 1, 32, 0xFF00FF00);
            guiGraphics.fill(-32, 0, -8, 1, 0xFF00FF00);
            guiGraphics.fill(32, 0, 8, 1, 0xFF00FF00);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, (int) LocalVehiclePlayer.instance.aimLocationDistance + " 格", 0, 40, 0xFF00FF00);
        }
        guiGraphics.pose().popPose();
        renderCompassBar(guiGraphics, screenWidth, vehicle);
    }

    private void renderCompassBar(GuiGraphics guiGraphics, int screenWidth, HelicopterVehicle vehicle) {
        PartUnit partUnit = vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer());
        if (partUnit instanceof WeaponUnit weaponUnit) {
            float yaw = weaponUnit.worldRot().y;
            Font font = Minecraft.getInstance().font;
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            {
                RenderSystem.enableBlend();
                float centerX = screenWidth / 2f;
                poseStack.translate(centerX - 0.5f, 0, 0);

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
                                    guiGraphics.vLine(x * 4, 0, 6, 0xFF00FF00);
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

    private void renderDirection(GuiGraphics graphics, PoseStack poseStack, Font font, int x, String s) {
        graphics.vLine(x * 4, 0, 8, 0xFF00FF00);
        poseStack.translate(1f, 0, 0);
        graphics.drawCenteredString(font, s, x * 4, 12, 0xFF00FF00);
        poseStack.translate(-1f, 0, 0);
    }

}
