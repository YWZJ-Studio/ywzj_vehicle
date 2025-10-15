package org.ywzj.vehicle.handler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.client.gui.ScopeOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import static org.ywzj.vehicle.util.MathUtil.magnificationToFov;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class LocalVehiclePlayerEvent {

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.END) {
            if (event.player == LocalVehiclePlayer.instance.getPlayer()) {
                LocalVehiclePlayer.instance.tick();
            }
        }
    }

    @SubscribeEvent
    public static void applyScopeMagnification(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) {
            return; // 只修改世界渲染的 fov，因此如果是手部渲染 fov 事件，则返回
        }
        Entity entity = event.getCamera().getEntity();
        if (entity instanceof LivingEntity livingEntity && entity.equals(LocalVehiclePlayer.instance.getPlayer())) {
            if (entity.getVehicle() instanceof AbstractVehicle vehicle
                    && vehicle.getOwnOperatorUnit(livingEntity) instanceof WeaponUnit weaponUnit
                    && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                ScopeOverlay.fov = (float) magnificationToFov(1 + (weaponUnit.getZoom() - 1), event.getFOV());
                event.setFOV(ScopeOverlay.fov);
            }
        }
    }

}
