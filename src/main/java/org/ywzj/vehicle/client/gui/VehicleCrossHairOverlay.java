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
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

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
        if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (player.getVehicle() instanceof AbstractVehicle vehicle) {
            WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(player);
            if (weaponUnit != null) {
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
            WeaponUnit weaponUnit = vehicle.getOwnWeaponUnit(player);
            if (weaponUnit != null) {
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
                Vector2f rot = weaponUnit.worldRot();
                Vec3 hitScreenPos = getHitScreenPos(weaponUnit.ammoSpawnPosition(), rot.x, rot.y, player);
                if (hitScreenPos.z >= 0) {
                    screenXO = screenX;
                    screenYO = screenY;
                    screenX = hitScreenPos.x;
                    screenY = hitScreenPos.y;
                    show = true;
                } else {
                    show = false;
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

    private static void drawCircle(GuiGraphics guiGraphics, int x, int y, int r, int color) {
        float c = 2 * 3.1415f / 32;
        for (int i = 0; i < 32; i += 1) {
            float rx = Mth.cos(c * i) * r;
            float ry = Mth.sin(c * i) * r;
            guiGraphics.fill((int) (x + rx), (int) (y + ry), (int) (x + rx) + 1, (int) (y + ry) + 1, color);
        }
    }

}
