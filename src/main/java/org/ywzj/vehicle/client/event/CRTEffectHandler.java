package org.ywzj.vehicle.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.render.util.PostPassesGetter;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CRTEffectHandler {

    public static final ResourceLocation SHADER_LOCATION = new ResourceLocation("ywzj_vehicle:shaders/post/crt.json");

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide()) {
            PostChain effect = Minecraft.getInstance().gameRenderer.currentEffect();
            if (LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.SCOPE) {
                if (effect == null || !effect.getName().equals("ywzj_vehicle:shaders/post/crt.json")) {
                    Minecraft.getInstance().gameRenderer.loadEffect(SHADER_LOCATION);
                }
            } else if (effect != null && effect.getName().equals("ywzj_vehicle:shaders/post/crt.json")) {
                Minecraft.getInstance().gameRenderer.shutdownEffect();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PostChain effect = Minecraft.getInstance().gameRenderer.currentEffect();
            if (effect != null && effect.getName().equals("ywzj_vehicle:shaders/post/crt.json")) {
                if (effect instanceof PostPassesGetter getter) {
                    Minecraft mc = Minecraft.getInstance();
                    for (PostPass pass : getter.getPasses()) {
                        pass.getEffect().safeGetUniform("Time").set((float) (System.currentTimeMillis() % 100000) / 1000f);
                        pass.getEffect().safeGetUniform("Resolution").set((float) mc.getWindow().getWidth(), (float) mc.getWindow().getHeight());
                    }
                }
            }
        }
    }

}
