package org.ywzj.vehicle.client.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.render.util.PostPassesGetter;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CrtHandler implements ResourceManagerReloadListener {

    public static final ResourceLocation CRT_EFFECT_PATH = YwzjVehicle.resourceLocation("ywzj_vehicle:shaders/post/crt.json");
    private static PostChain postChain;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean isActive = false;

    public static void setActive(boolean active) {
        if (isActive != active) {
            isActive = active;
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
                isActive = false;
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
        if (!isActive) {
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            Minecraft minecraft = Minecraft.getInstance();
            ensureChain(minecraft);
            postChain.process(event.getPartialTick());
            if (postChain instanceof PostPassesGetter getter) {
                for (PostPass pass : getter.getPasses()) {
                    pass.getEffect().safeGetUniform("Time").set((float) (System.currentTimeMillis() % 100000) / 1000f);
                    pass.getEffect().safeGetUniform("Resolution").set((float) minecraft.getWindow().getWidth(), (float) minecraft.getWindow().getHeight());
                }
            }
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

}
