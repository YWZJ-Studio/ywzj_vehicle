package org.ywzj.vehicle.handler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.ywzj.vehicle.util.HitboxHelper;

@EventBusSubscriber
public class HitboxHelperEvent {

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() == false) {
            HitboxHelper.onPlayerTick(event.getEntity());
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HitboxHelper.clearCache(event.getEntity());
    }

}
