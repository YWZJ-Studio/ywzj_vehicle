package org.ywzj.vehicle.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.HelicopterVehicle;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.PartUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class VehicleCrossHairOverlay implements IGuiOverlay {

    private static double screenHitXO = 0;
    private static double screenHitYO = 0;
    private static double screenHitX = 0;
    private static double screenHitY = 0;
    private static boolean showHit = true;

    private static double helicopterScreenHitXO = 0;
    private static double helicopterScreenHitYO = 0;
    private static double helicopterScreenHitX = 0;
    private static double helicopterScreenHitY = 0;
    private static boolean helicopterShowHit = true;

    private static double screenAimXO = 0;
    private static double screenAimYO = 0;
    private static double screenAimX = 0;
    private static double screenAimY = 0;
    private static boolean showAim = true;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            boolean isHelicopter = vehicle instanceof HelicopterVehicle;
            int color = isHelicopter ?  0xFF00FF00 : 0xFFFFFFFF;
            PartUnit operatorUnit = vehicle.getOwnOperatorUnit(player);
            if (operatorUnit != null) {
                if (showAim) {
                    double x = Mth.lerp(partialTick, screenAimXO, screenAimX);
                    double y = Mth.lerp(partialTick, screenAimYO, screenAimY);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    drawCircle(guiGraphics, 0 ,0, 10, color);
                    guiGraphics.pose().popPose();
                }
                if (showHit) {
                    double x = Mth.lerp(partialTick, screenHitXO, screenHitX);
                    double y = Mth.lerp(partialTick, screenHitYO, screenHitY);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    if (isHelicopter) {
                        drawSquare(guiGraphics, 0 ,0, 5, color);
                    } else {
                        drawCircle(guiGraphics, 0 ,0, 5, color);
                    }
                    guiGraphics.pose().popPose();
                }
                if (helicopterShowHit) {
                    double x = Mth.lerp(partialTick, helicopterScreenHitXO, helicopterScreenHitX);
                    double y = Mth.lerp(partialTick, helicopterScreenHitYO, helicopterScreenHitY);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    drawReticle(guiGraphics, 0 ,0, 15, 1, color);
                    guiGraphics.pose().popPose();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            if (vehicle.getOwnOperatorUnit(player) instanceof WeaponUnit weaponUnit) {
                // 瞄准位置
                Vec3 aimScreenPos = getHitScreenPos(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
                        LocalVehiclePlayer.instance.cameraAimRotX,
                        LocalVehiclePlayer.instance.cameraAimRotY,
                        player);
                if (aimScreenPos.z >= 0) {
                    screenAimXO = screenAimX;
                    screenAimYO = screenAimY;
                    screenAimX = aimScreenPos.x;
                    screenAimY = aimScreenPos.y;
                    showAim = true;
                } else  {
                    showAim = false;
                }
                // 瞄准落点
                Vec2 rot = weaponUnit.worldRot();
                Vec3 screenHitPos = getHitScreenPos(weaponUnit.ammoSpawnPosition(), rot.x, rot.y, player);
                if (screenHitPos.z >= 0) {
                    screenHitXO = screenHitX;
                    screenHitYO = screenHitY;
                    screenHitX = screenHitPos.x;
                    screenHitY = screenHitPos.y;
                    showHit = true;
                } else {
                    showHit = false;
                }
                // 机身瞄准落点
                if (vehicle instanceof HelicopterVehicle) {
                    Vec3 helicopterScreenHitPos = getHitScreenPos(weaponUnit.ammoSpawnPosition(), vehicle.getXRot() - 10, vehicle.getYRot(), player);
                    if (helicopterScreenHitPos.z >= 0) {
                        helicopterScreenHitXO = helicopterScreenHitX;
                        helicopterScreenHitYO = helicopterScreenHitY;
                        helicopterScreenHitX = helicopterScreenHitPos.x;
                        helicopterScreenHitY = helicopterScreenHitPos.y;
                        helicopterShowHit = true;
                    } else {
                        helicopterShowHit = false;
                    }
                }
            }
        }
    }

    private static @NotNull Vec3 getHitScreenPos(Vec3 start, float xRot, float yRot, Player player) {
        Vec3 end = start.add(VectorUtil.calculateViewVector(xRot, yRot).normalize().scale(128));
        var result = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hitPos = result.getLocation();
        return VectorUtil.worldToScreen(hitPos);
    }

    public static void drawCircle(GuiGraphics guiGraphics, int x, int y, int r, int color) {
        float c = 2 * 3.1415f / 32;
        for (int i = 0; i < 32; i += 1) {
            float rx = Mth.cos(c * i) * r;
            float ry = Mth.sin(c * i) * r;
            guiGraphics.fill((int) (x + rx), (int) (y + ry), (int) (x + rx) + 1, (int) (y + ry) + 1, color);
        }
    }

    public static void drawSquare(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        int half = size / 2;
        int left = x - half;
        int right = x + half;
        int top = y - half;
        int bottom = y + half;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(0.7f, 0.7f, 0.7f);
            for (int i = left; i <= right; i++) {
                guiGraphics.fill(i, top, i + 1, top + 1, color);
                guiGraphics.fill(i, bottom, i + 1, bottom + 1, color);
            }
            for (int j = top; j <= bottom; j++) {
                guiGraphics.fill(left, j, left + 1, j + 1, color);
                guiGraphics.fill(right, j, right + 1, j + 1, color);
            }
        }
        poseStack.popPose();
    }

    public static void drawReticle(GuiGraphics guiGraphics, int x, int y, int size, int thickness, int color) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        {
            poseStack.scale(0.5f, 0.5f, 0.5f);
            if (thickness < 1) thickness = 1;
            int top = y;
            int bottom = y + size;
            // 竖线
            int vHalf = thickness / 2;
            int vLeft = x - vHalf;
            int vRight = vLeft + thickness;
            guiGraphics.fill(vLeft, top, vRight, bottom, color);
            // 横线
            int halfLen = Math.max(1, (int)(size * 0.45));
            int segments = 4;
            int gap = size / segments; // 每段间距
            int hHalf = thickness / 2;
            for (int i = 0; i <= segments; i++) {
                int yPos = top + gap * i;
                int left = x - halfLen;
                int right = x + halfLen;
                guiGraphics.fill(left + 1, yPos - hHalf, right, yPos + (thickness - hHalf), color);
                halfLen -= 1;
            }
        }
        poseStack.popPose();
    }

}
