package org.ywzj.vehicle.client.event;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class OverlayHandler {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON
                && player.getVehicle() instanceof AbstractVehicle) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
                event.setCanceled(true);
            } else if (event.getOverlay().id().equals(VanillaGuiOverlay.MOUNT_HEALTH.id())) {
                event.setCanceled(true);
            } else if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
                event.setCanceled(true);
            }
        }
    }

}
