package org.ywzj.vehicle.client.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RenderLivingHandler {

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent event) {
        if (event.getEntity().getVehicle() instanceof AbstractVehicle vehicle) {
            if (vehicle.uav) {
                event.setCanceled(true);
            }
        }
    }

}
