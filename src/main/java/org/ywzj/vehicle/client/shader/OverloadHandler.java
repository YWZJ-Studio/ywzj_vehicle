package org.ywzj.vehicle.client.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.render.util.PostPassesGetter;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@EventBusSubscriber(value = Dist.CLIENT)
public class OverloadHandler implements ResourceManagerReloadListener {

    public static final ResourceLocation CRT_EFFECT_PATH = YwzjVehicle.resourceLocation("ywzj_vehicle:shaders/post/overload.json");
    private static PostChain postChain;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean active = false;

    public static void setActive(boolean active) {
        if (OverloadHandler.active != active) {
            OverloadHandler.active = active;
            if (!active) {
                cleanup();
            }
        }
    }

    private static void cleanup() {
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        cleanup();
    }

    private static boolean ensureChain(Minecraft mc) {
        if (postChain == null) {
            try {
                postChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CRT_EFFECT_PATH);
                postChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
                lastWidth = mc.getWindow().getWidth();
                lastHeight = mc.getWindow().getHeight();
            } catch (Exception e) {
                e.printStackTrace();
                active = false;
                return false;
            }
        }
        if (lastWidth != mc.getWindow().getWidth() || lastHeight != mc.getWindow().getHeight()) {
            lastWidth = mc.getWindow().getWidth();
            lastHeight = mc.getWindow().getHeight();
            postChain.resize(lastWidth, lastHeight);
        }
        return true;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            ensureChain(minecraft);
            postChain.process(event.getPartialTick().getGameTimeDeltaPartialTick(true));
            if (postChain instanceof PostPassesGetter getter) {
                for (PostPass pass : getter.getPasses()) {
                    float stamina = LocalVehiclePlayer.instance.stamina;
                    if (stamina < 70) {
                        pass.getEffect().safeGetUniform("Progress").set((70 - stamina) / 70);
                        pass.getEffect().safeGetUniform("Type").set(2);
                    } else if (stamina > 110) {
                        pass.getEffect().safeGetUniform("Progress").set((stamina - 110) / 50);
                        pass.getEffect().safeGetUniform("Type").set(1);
                    } else {
                        pass.getEffect().safeGetUniform("Progress").set(0f);
                    }
                }
            }
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    public static boolean isActive() {
        return active;
    }

}
