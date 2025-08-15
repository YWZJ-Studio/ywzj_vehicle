package org.ywzj.vehicle.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;

@Mod.EventBusSubscriber
public class VehicleCrossHairOverlay implements IGuiOverlay {

    private static double screenXO = 0;
    private static double screenYO = 0;
    private static double screenX = 0;
    private static double screenY = 0;
    private static boolean show = true;

    private static double screenAimXO = 0;
    private static double screenAimYO = 0;
    private static double screenAimX = 0;
    private static double screenAimY = 0;
    private static boolean showAim = true;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            var weapon = !vehicle.weaponUnits.isEmpty() ? vehicle.weaponUnits.get(0) : null;
            if (weapon != null) {
                if (show) {
                    double x = Mth.lerp(partialTick, screenXO, screenX);
                    double y = Mth.lerp(partialTick, screenYO, screenY);

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    drawCircle(guiGraphics, 0 ,0, 5, 0xFFFFFFFF);
                    guiGraphics.pose().popPose();
                }

                if (showAim) {
                    double x = Mth.lerp(partialTick, screenAimXO, screenAimX);
                    double y = Mth.lerp(partialTick, screenAimYO, screenAimY);

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    drawCircle(guiGraphics, 0 ,0, 10, 0xFFFFFFFF);
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
            // todo: 根据玩家获取操控武器
            var weapon = !vehicle.weaponUnits.isEmpty() ? vehicle.weaponUnits.get(0) : null;
            if (weapon != null) {
                Vec3 start = player.position().add(0, 2.5, 0);

                Vector2f v1 = weapon.worldRot();
                Vec3 screenPos1 = getHitScreenPos(start, v1.x, v1.y, player);
                if (screenPos1.z >= 0) {
                    screenXO = screenX;
                    screenYO = screenY;
                    screenX = screenPos1.x;
                    screenY = screenPos1.y;
                    show = true;
                } else {
                    show = false;
                }

                Vector2f v2 = weapon.worldAimRot();
                Vec3 screenPos2 = getHitScreenPos(start, v2.x, v2.y, player);
                if (screenPos1.z >= 0) {
                    screenAimXO = screenAimX;
                    screenAimYO = screenAimY;
                    screenAimX = screenPos2.x;
                    screenAimY = screenPos2.y;
                    showAim = true;
                } else  {
                    showAim = false;
                }
            }
        }
    }

    private static @NotNull Vec3 getHitScreenPos(Vec3 start, float xRot, float yRot, Player player) {
        Vec3 end = start.add(calculateViewVector(xRot, yRot).normalize().scale(128));
        var result = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hitPos = result.getLocation();
        return VectorUtil.worldToScreen(hitPos);
    }

    public static Vec3 calculateViewVector(float pXRot, float pYRot) {
        float f = pXRot * ((float)Math.PI / 180F);
        float f1 = -pYRot * ((float)Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }

    private static void drawCircle(GuiGraphics guiGraphics, int x, int y, int r, int color) {
        float c = 2 * 3.1415f / 32;
        for (int i = 0; i < 32; i += 1) {
            float rx = Mth.cos(c * i) * r;
            float ry = Mth.sin(c * i) * r;
            guiGraphics.fill((int) (x + rx), (int) (y + ry), (int) (x + rx) + 1, (int) (y + ry) + 1, color);
        }
    }

}
