package org.ywzj.vehicle.client.event;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.entity.AbstractVehicle;

@Mod.EventBusSubscriber
public class OverlayHandler {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON
                && player.getVehicle() instanceof AbstractVehicle vehicle) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
                event.setCanceled(true);
            }
        }
    }
}
