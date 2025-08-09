package org.ywzj.vehicle.client.event;

import net.minecraft.client.Camera;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CameraHandler extends Camera {
    @SubscribeEvent
    public static void on(ViewportEvent.ComputeCameraAngles event) {

    }
}
