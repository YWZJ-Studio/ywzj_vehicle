package org.ywzj.vehicle.client.event;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mod.EventBusSubscriber
public class FirstPersonHandHandler {

    @SubscribeEvent
    public static void onRenderOverlay(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON
                && player.getVehicle() instanceof AbstractVehicle vehicle) {
            event.setCanceled(true);
        }
    }

}
