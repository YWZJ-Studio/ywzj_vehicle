package org.ywzj.vehicle.client.handler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.client.render.animation.item.RepairItemAnimationInstance;
import org.ywzj.vehicle.client.render.item.AbstractGeoItemRenderer;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class FirstPersonHandler {

    public static float zRot;

    @SubscribeEvent
    public static void onRenderOverlay(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) {
            return;
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON
                && player.getVehicle() instanceof AbstractVehicle) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (LocalVehiclePlayer.instance.onVehicle()) {
            if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE
                    || LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.OPERATOR) {
                event.setRoll(zRot);
            }
        }
    }

    public static RepairItemAnimationInstance instance;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // 如果存在动画实例，则我们不再关心事件里的物品
        if (instance != null) {
            // 获取 TransformType
            ItemDisplayContext transformType;
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                transformType = ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            } else {
                transformType = ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            }

            ItemStack stack = instance.getItemStack();
            if (IClientItemExtensions.of(stack.getItem()).getCustomRenderer() instanceof AbstractGeoItemRenderer<?> renderer) {
                renderer.renderFirstPerson(player, stack, transformType, event.getPoseStack(), event.getMultiBufferSource(),
                        event.getPackedLight(), event.getPartialTick());
                event.setCanceled(true);
            }
        }

    }

    @SubscribeEvent
    public static void tickAnimation(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (instance != null) {
            instance.tick(event.renderTickTime);
        }
    }
}
