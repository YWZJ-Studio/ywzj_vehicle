package org.ywzj.vehicle.handler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.client.gui.VehicleScopeOverlay;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.util.ZoomSmoother;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import static org.ywzj.vehicle.util.MathUtil.magnificationToFov;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class LocalVehiclePlayerEvent {

    private static ZoomSmoother worldFovSmoother;

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
            return;
        }
        Entity entity = event.getCamera().getEntity();
        if (entity instanceof LivingEntity livingEntity && entity.equals(LocalVehiclePlayer.instance.getPlayer())) {
            if (worldFovSmoother == null) {
                worldFovSmoother = new ZoomSmoother(event.getFOV());
            }
            if (entity.getVehicle() instanceof AbstractVehicle vehicle
                    && vehicle.getOwnOperatorUnit(livingEntity) instanceof WeaponUnit weaponUnit) {
                float fov;
                if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                    float targetFov = (float) magnificationToFov(weaponUnit.getZoom(), event.getFOV());
                    fov = (float) worldFovSmoother.update(targetFov);
                } else if (vehicle.isViewZoomed()) {
                    float targetFov = (float) magnificationToFov(2f, event.getFOV());
                    fov = (float) worldFovSmoother.update(targetFov);
                } else {
                    fov = (float) worldFovSmoother.update(event.getFOV());
                }
                VehicleScopeOverlay.fov = fov;
                event.setFOV(fov);
            } else {
                float fov = (float) worldFovSmoother.update(event.getFOV());
                event.setFOV(fov);
            }
        } else {
            worldFovSmoother = null;
        }
    }

}
