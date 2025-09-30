package org.ywzj.vehicle.all;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;

public class AllEvents {

    @Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AllForgeEvents {

        @SubscribeEvent
        public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity().isPassenger()) {
                event.getEntity().stopRiding();
            }
        }

        @SubscribeEvent
        public void onPlayerTeleport(EntityTeleportEvent event) {
            if (event.getEntity() instanceof Player player) {
                if (player.isPassenger()) {
                    player.stopRiding();
                }
            }
        }

//        @SubscribeEvent
//        public static void onLivingAttack(LivingAttackEvent livingAttackEvent) {
//            //todo 服务器限定
//            if (livingAttackEvent.getEntity() instanceof OBBEntity && !"genericKill".equals(livingAttackEvent.getSource().getMsgId())) {
//                livingAttackEvent.setCanceled(true);
//            }
//        }

    }

}
