package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.apache.commons.lang3.tuple.Pair;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.entity.OBBEntity;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.RenderHelper;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

public class VehicleOverlay implements IGuiOverlay {

    public static int color = 0xFF00FF00;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        renderLookAt(guiGraphics, partialTick);
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        if (!localVehiclePlayer.onVehicle()) {
            return;
        }
        AbstractVehicle vehicle = localVehiclePlayer.getVehicle();
//        int centerX = screenWidth / 2;
//        int centerY = screenHeight / 2;
//        renderCrew(guiGraphics, centerX, centerY, vehicle);
        if (localVehiclePlayer.viewType != LocalVehiclePlayer.ViewType.THIRD_PERSON) {
            renderCompassBar(guiGraphics, screenWidth, vehicle);
        }
        renderBaseInfo(guiGraphics, screenWidth, screenHeight, vehicle);
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

    private void renderBaseInfo(GuiGraphics guiGraphics, int screenWidth, int screenHeight, AbstractVehicle vehicle) {
        renderHealth(guiGraphics, 60, screenHeight - 20, 100, 10, vehicle, 1);
    }

    private void renderLookAt(GuiGraphics guiGraphics, float partialTick) {
        double showVehicleInfoDistance = AllConfigs.server.showVehicleInfoDistance.get();
        if (showVehicleInfoDistance <= 0) {
            return;
        }
        LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
        float rot = 0;
        if (localVehiclePlayer.onVehicle()) {
            rot = localVehiclePlayer.viewType != LocalVehiclePlayer.ViewType.SCOPE ? LocalVehiclePlayer.CAMERA_UPWARD_ANGLE : 0;
        }
        Player player = localVehiclePlayer.getPlayer();
        float xRot = player.getXRot() - rot;
        float yRot = player.getYRot();
        Vec3 start = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 end = start.add(VectorUtil
                .calculateViewVector(xRot, yRot)
                .normalize()
                .scale(LocalVehiclePlayer.renderDistance()));
        Pair<OBBEntity, Vec3> hitResult = VectorUtil.hitObbPosition(player, start, end);
        if (hitResult != null) {
            OBBEntity obbEntity = hitResult.getLeft();
            if (obbEntity instanceof AbstractVehicle vehicle) {
                float distance = player.distanceTo(vehicle);
                if (distance > showVehicleInfoDistance) {
                    return;
                }
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                {
                    float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.5f * Math.max((512 - distance) / 512, 0.1), 0.33, 1);
                    AABB aabb = vehicle.getBoundingBox();
                    Vec3 pos = new Vec3(Mth.lerp(partialTick, vehicle.xo, vehicle.getX()),
                            Mth.lerp(partialTick, vehicle.yo, vehicle.getY()) + aabb.maxY - aabb.minY,
                            Mth.lerp(partialTick, vehicle.zo, vehicle.getZ()));
                    Vec3 posScreen = VectorUtil.worldToScreen(pos);
                    renderHealth(guiGraphics, posScreen.x, posScreen.y, 90, 5, vehicle, size);
                }
                poseStack.popPose();
            }
        }
    }

    public void renderHealth(GuiGraphics guiGraphics, double x, double y, int barWidth, int barHeight, AbstractVehicle vehicle, float size) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            Font font = Minecraft.getInstance().font;
            int bgColor = 0xAA000000;
            float barHalfWidth = (float) barWidth / 2;
            float barHalfHeight = (float) barHeight / 2;
            poseStack.translate(x, y + barHalfHeight - 8, 0);
            poseStack.scale(size, size, size);
            guiGraphics.drawCenteredString(font, vehicle.getVehicleType().getName(), 0, -14, 0xFFFFFFFF);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight, barHalfWidth, barHalfHeight, 0, bgColor);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth - 1, -barHalfHeight, -barHalfWidth, barHalfHeight, 0, 0xFF999999);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), barHalfWidth, -barHalfHeight, barHalfWidth + 1, barHalfHeight, 0, 0xFF999999);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight - 1, barHalfWidth, -barHalfHeight, 0, 0xFF999999);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, barHalfHeight, barHalfWidth, barHalfHeight + 1, 0, 0xFF999999);
            float health = vehicle.getHealth();
            float maxHealth = vehicle.getMaxHealth();
            float percent = Math.max(0, Math.min(1, health / maxHealth));
            int red, green;
            if (vehicle.isDestroyed()) {
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
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -barHalfWidth, -barHalfHeight, -barHalfWidth + filledWidth, barHalfHeight, 0, barColor);
            poseStack.pushPose();
            {
                String text = String.format("%.0f/%.0f", health, maxHealth);
                guiGraphics.drawCenteredString(font, text, 0, -4, 0xFFFFFFFF);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void renderDirection(GuiGraphics graphics, PoseStack poseStack, Font font, int x, String s) {
        graphics.vLine(x * 4, 0, 8, color);
        poseStack.translate(1f, 0, 0);
        graphics.drawCenteredString(font, s, x * 4, 12, color);
        poseStack.translate(-1f, 0, 0);
    }

}
