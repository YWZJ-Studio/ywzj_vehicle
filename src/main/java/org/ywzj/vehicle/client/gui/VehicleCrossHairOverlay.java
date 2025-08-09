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
import org.ywzj.vehicle.entity.AbstractVehicle;
import org.ywzj.vehicle.util.VectorUtil;

@Mod.EventBusSubscriber
public class VehicleCrossHairOverlay implements IGuiOverlay {
    private static double screenXO = 0;
    private static double screenYO = 0;
    private static double screenX = 0;
    private static double screenY = 0;

    private static double screenAimXO = 0;
    private static double screenAimYO = 0;
    private static double screenAimX = 0;
    private static double screenAimY = 0;

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
                double x = Mth.lerp(partialTick, screenXO, screenX);
                double y = Mth.lerp(partialTick, screenYO, screenY);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x, y, 0);
                guiGraphics.fill(-1, -1, 1, 1, 0xFF00FF00);
                guiGraphics.pose().popPose();

                double x1 = Mth.lerp(partialTick, screenAimXO, screenAimX);
                double y1 = Mth.lerp(partialTick, screenAimYO, screenAimY);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x1, y1, 0);
                guiGraphics.fill(-2, -2, 2, 2, 0xFFFF0000);
                guiGraphics.pose().popPose();
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
            var weapon = !vehicle.weaponUnits.isEmpty() ? vehicle.weaponUnits.get(0) : null;
            if (weapon != null) {

                Vec3 start = vehicle.position().add(0, 2.5, 0);
                Vec3 end = start.add(calculateViewVector(weapon.xRot, weapon.yRot).normalize().scale(128));

                var r = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                Vec3 hitPos = r.getLocation();
                Vec3 screenPos = VectorUtil.worldToScreen(hitPos);
                if (screenPos.z >= 0) {
                    screenXO = screenX;
                    screenYO = screenY;
                    screenX = screenPos.x;
                    screenY = screenPos.y;
                }

                start = vehicle.position().add(0, 2.5, 0);
                end = start.add(calculateViewVector(weapon.aimXRot, weapon.aimYRot).normalize().scale(128));

                r = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                hitPos = r.getLocation();
                screenPos = VectorUtil.worldToScreen(hitPos);
                if (screenPos.z >= 0) {
                    screenAimXO = screenAimX;
                    screenAimYO = screenAimY;
                    screenAimX = screenPos.x;
                    screenAimY = screenPos.y;
                }

            }
        }
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

}
