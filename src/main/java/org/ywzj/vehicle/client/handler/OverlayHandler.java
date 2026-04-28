package org.ywzj.vehicle.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.Set;

@EventBusSubscriber(value = Dist.CLIENT)
public class OverlayHandler {

    private static final Set<ResourceLocation> HOTBAR_ELEMENTS = Set.of(
            VanillaGuiLayers.CROSSHAIR,
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.VEHICLE_HEALTH,
            VanillaGuiLayers.JUMP_METER,
            VanillaGuiLayers.FOOD_LEVEL
    );

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON
                && player.getVehicle() instanceof AbstractVehicle
                && HOTBAR_ELEMENTS.contains(event.getName())) {
            event.setCanceled(true);
        }
    }

}
