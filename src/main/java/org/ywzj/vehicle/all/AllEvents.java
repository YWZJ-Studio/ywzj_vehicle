package org.ywzj.vehicle.all;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.OBBEntity;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.event.HitVehicleEvent;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerHitVehicleEvent;
import org.ywzj.vehicle.vehicle.VehicleCapabilityProvider;

public class AllEvents {

    @Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AllForgeEvents {

        @SubscribeEvent
        public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity().isPassenger() && event.getEntity().getVehicle() instanceof AbstractVehicle) {
                event.getEntity().stopRiding();
            }
        }

        @SubscribeEvent
        public static void onPlayerTeleport(EntityTeleportEvent event) {
            if (event.getEntity() instanceof Player player) {
                if (player.isPassenger() && player.getVehicle() instanceof AbstractVehicle) {
                    player.stopRiding();
                }
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            if (event.getEntity().getVehicle() instanceof AbstractVehicle vehicle) {
                if (vehicle.uav) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof AbstractVehicle) {
                event.addCapability(new ResourceLocation(YwzjVehicle.MOD_ID, "vehicle_capability"), new VehicleCapabilityProvider());
            }
        }

        @SubscribeEvent
        public static void onHitVehicleEvent(HitVehicleEvent event) {
            ServerPlayer serverPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(event.shooterUuid);
            if (serverPlayer != null) {
                Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ServerHitVehicleEvent(event));
            }
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent livingAttackEvent) {
            //todo 完善伤害过滤
            if (livingAttackEvent.getEntity() instanceof OBBEntity && !"genericKill".equals(livingAttackEvent.getSource().getMsgId())) {
                livingAttackEvent.setCanceled(true);
            }
        }

    }

}
