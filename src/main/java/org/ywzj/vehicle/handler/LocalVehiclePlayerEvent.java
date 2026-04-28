package org.ywzj.vehicle.handler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.ywzj.vehicle.client.gui.VehicleScopeOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import static org.ywzj.vehicle.util.MathUtil.magnificationToFov;

@EventBusSubscriber(value = Dist.CLIENT)
public class LocalVehiclePlayerEvent {

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            if (event.getEntity() == LocalVehiclePlayer.instance.getPlayer()) {
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
                    && vehicle.getOwnOperatorUnit(livingEntity) instanceof WeaponUnit weaponUnit) {
                if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                    VehicleScopeOverlay.fov = (float) magnificationToFov(1 + (weaponUnit.getZoom() - 1), event.getFOV());
                    event.setFOV(VehicleScopeOverlay.fov);
                } else if (vehicle.isViewZoomed()) {
                    VehicleScopeOverlay.fov = (float) magnificationToFov(2f, event.getFOV());
                    event.setFOV(VehicleScopeOverlay.fov);
                }
            }
        }
    }

}
