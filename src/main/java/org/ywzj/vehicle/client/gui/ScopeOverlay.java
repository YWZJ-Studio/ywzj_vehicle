package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.HelicopterVehicle;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PartUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import static org.ywzj.vehicle.util.RenderHelper.drawRect;
import static org.ywzj.vehicle.util.RenderHelper.drawSquare;

public class ScopeOverlay implements IGuiOverlay {

    public static float fov;
    public static int color = 0xFF00FF00;

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
        // 准心
        guiGraphics.pose().pushPose();
        {
            guiGraphics.pose().translate(centerX, centerY, 0);
            drawSquare(guiGraphics, 0, 0, 5, color);
            guiGraphics.fill(0, -32, 1, -8, color);
            guiGraphics.fill(0, 8, 1, 32, color);
            guiGraphics.fill(-32, 0, -8, 1, color);
            guiGraphics.fill(32, 0, 8, 1, color);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                    (LocalVehiclePlayer.instance.outOfRangeFinding ? ">" : "")
                            + (int) LocalVehiclePlayer.instance.aimLocationDistance + " 格", 0, 40, color);
            if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, "x" + String.format("%.1f", weaponUnit.getZoom()), 32, 16, color);
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, weaponUnit.isStabilizerOn() ? "稳定器开" : "", 36, 28, color);
            }
        }
        guiGraphics.pose().popPose();
        // 成员组
        int x = centerX - 140;
        int y = centerY + guiGraphics.guiHeight() / 5;
        for (int index = 0; index < vehicle.seats.size(); index++) {
            Integer playerId = vehicle.seats.get(index).passengerId;
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
            guiGraphics.drawString(Minecraft.getInstance().font, info, x, y, color);
            y += 10;
        }
        // 稳定器锁定的位置
        renderAimLockTarget(guiGraphics);
        // 罗盘
        renderCompassBar(guiGraphics, screenWidth, vehicle);
    }

    public void renderGroundVehicle(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {

    }

    public void renderHelicopter(GuiGraphics guiGraphics, int screenWidth, int screenHeight, HelicopterVehicle vehicle) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        // 主信息
        HelicopterControlOverlay.renderMainInfo(guiGraphics, centerX, centerY, vehicle);
        // 高度信息
        HelicopterControlOverlay.renderHeightInfo(guiGraphics, centerX, centerY, vehicle);
        guiGraphics.pose().pushPose();
        {
            guiGraphics.pose().translate(centerX, centerY, 0);
            if (!LocalVehiclePlayer.instance.controllingMissileIds.isEmpty()) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, "激光制导中", 0, -45, color);
            }
            if (vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer()) instanceof WeaponUnit weaponUnit) {
                //todo 导弹配置里有射界，暂时写死
                if (LocalVehiclePlayer.instance.controllingMissileIds.isEmpty()) {
                    if (Math.abs(weaponUnit.yRot) > 15 || weaponUnit.xRot < -11.5 || weaponUnit.xRot > 33.5) {
                        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "超出导弹射界", 0, -45, color);
                    }
                }
                guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
                int baseX = 0;
                int baseY = 140;
                float xRotRange = weaponUnit.getXRotMax() - weaponUnit.getXRotMin();
                float yRotRange = weaponUnit.getYRotMax() - weaponUnit.getYRotMin();
                drawRect(guiGraphics, baseX, baseY, (int) yRotRange, (int) xRotRange, color, 1f);
                //todo 导弹配置里有射界，暂时写死
                drawRect(guiGraphics, baseX, baseY - 5, 30, 45, color, 1f);
                int x = (int) (weaponUnit.yRot - weaponUnit.getYRotMin());
                int y = (int) (weaponUnit.xRot - weaponUnit.getXRotMin());
                baseX -= (int) (yRotRange / 2);
                baseY -= (int) (xRotRange / 2);
                guiGraphics.fill(baseX + x, baseY + y - 8, baseX + x + 1, baseY + y - 2, color);
                guiGraphics.fill(baseX + x, baseY + y + 3, baseX + x + 1, baseY + y + 9, color);
                guiGraphics.fill(baseX + x - 8, baseY + y, baseX + x - 2, baseY + y + 1, color);
                guiGraphics.fill(baseX + x + 3, baseY + y, baseX + x + 9, baseY + y + 1, color);
            }
        }
        guiGraphics.pose().popPose();
    }

    public static void renderAimLockTarget(GuiGraphics guiGraphics) {
        WeaponUnit weaponUnit = LocalVehiclePlayer.instance.getWeaponUnit();
        if (weaponUnit != null && weaponUnit.isStabilizerOn()) {
            if (weaponUnit.getAimLockEntity() != null) {
                Entity entity = weaponUnit.getAimLockEntity();
                AABB aabb = entity.getBoundingBox();
                Vec3 screenPos = VectorUtil.worldToScreen(aabb.getCenter());
                guiGraphics.pose().pushPose();
                {
                    guiGraphics.pose().translate(screenPos.x, screenPos.y, 0);
                    RenderHelper.drawSquare(guiGraphics, 0, 0, 15, 0xFF00FF00);
                }
                guiGraphics.pose().popPose();
            }
            Vec3 pos = weaponUnit.getAimLockPosition();
            Vec3 screenPos = VectorUtil.worldToScreen(pos);
            guiGraphics.pose().pushPose();
            {
                guiGraphics.pose().translate(screenPos.x, screenPos.y, 0);
                RenderHelper.drawCross(guiGraphics, 0, 0, 10, 0xFF00FF00);
            }
            guiGraphics.pose().popPose();
        }
    }

    private void renderCompassBar(GuiGraphics guiGraphics, int screenWidth, AbstractVehicle vehicle) {
        PartUnit partUnit = vehicle.getOwnOperatorUnit(LocalVehiclePlayer.instance.getPlayer());
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

    private void renderDirection(GuiGraphics graphics, PoseStack poseStack, Font font, int x, String s) {
        graphics.vLine(x * 4, 0, 8, color);
        poseStack.translate(1f, 0, 0);
        graphics.drawCenteredString(font, s, x * 4, 12, color);
        poseStack.translate(-1f, 0, 0);
    }

}
