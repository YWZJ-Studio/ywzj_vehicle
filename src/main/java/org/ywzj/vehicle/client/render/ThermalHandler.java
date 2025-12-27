package org.ywzj.vehicle.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThermalHandler implements ResourceManagerReloadListener {

    private static final ResourceLocation THERMAL_EFFECT = new ResourceLocation("ywzj_vehicle", "shaders/post/thermal.json");
    private static boolean isActive = false;
    private static PostChain thermalChain;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean seeThroughWalls = true;

    public static void setSeeThroughWalls(boolean seeThrough) {
        seeThroughWalls = seeThrough;
    }

    public static void setActive(boolean active) {
        if (isActive != active) {
            isActive = active;
            if (!active) {
                cleanup();
            }
        }
    }

    private static void cleanup() {
        if (thermalChain != null) {
            thermalChain.close();
            thermalChain = null;
        }
    }

    public static boolean isActive() {
        return isActive;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        cleanup();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        setActive(false);
        setSeeThroughWalls(false);

        if (!isActive) return;

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            prepareAndRenderEntities(event.getPoseStack(), event.getPartialTick(), event.getFrustum(), event.getCamera());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            applyPostProcess(event.getPartialTick());
        }
    }

    private static boolean ensureChain(Minecraft mc) {
        if (thermalChain == null) {
            try {
                thermalChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), THERMAL_EFFECT);
                thermalChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
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
            thermalChain.resize(lastWidth, lastHeight);
        }
        return true;
    }

    private static void prepareAndRenderEntities(PoseStack poseStack, float partialTick, Frustum frustum, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (!ensureChain(mc)) return;

        // 获取 Buffer
        RenderTarget thermalBuffer = thermalChain.getTempTarget("thermal_buffer");
        if (thermalBuffer == null) {
            return;
        }

        thermalBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        thermalBuffer.clear(Minecraft.ON_OSX);
        if (!seeThroughWalls) {
            // 修复：如果主渲染目标启用了模板缓冲（例如使用了TACZ的瞄具），
            // 必须同步启用 thermalBuffer 的模板缓冲，否则 copyDepthFrom 会因格式不匹配而失败。
            if (mc.getMainRenderTarget().isStencilEnabled() && !thermalBuffer.isStencilEnabled()) {
                thermalBuffer.enableStencil();
            }

            try {
                thermalBuffer.copyDepthFrom(mc.getMainRenderTarget());
            } catch (Throwable ignored) {
                seeThroughWalls = true;
            }
        }
        thermalBuffer.bindWrite(true);

        Vec3 cameraPos = camera.getPosition();

        poseStack.pushPose();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0F, -1.0F);
        mc.getEntityRenderDispatcher().setRenderShadow(false);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isHotEntity(entity) && mc.getEntityRenderDispatcher().shouldRender(entity, frustum, cameraPos.x(), cameraPos.y(), cameraPos.z()) || entity.hasIndirectPassenger(mc.player)) {
                double lerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
                double lerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
                double lerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

                mc.getEntityRenderDispatcher().render(
                        entity,
                        lerpX - cameraPos.x,
                        lerpY - cameraPos.y,
                        lerpZ - cameraPos.z,
                        entity.getViewYRot(partialTick),
                        partialTick,
                        poseStack,
                        bufferSource,
                        15728880
                );
            }
        }

        bufferSource.endBatch();
        RenderSystem.disablePolygonOffset();
        poseStack.popPose();

        mc.getMainRenderTarget().bindWrite(true);
    }

    private static void applyPostProcess(float partialTick) {
        if (thermalChain == null) return;

        try {
            thermalChain.process(partialTick);
        } catch (Exception e) {
            e.printStackTrace();
            cleanup(); // 发生错误时清理，尝试下一帧重建
        }

        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    private static boolean isHotEntity(Entity entity) {
        if (entity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return false;
        }
//        return entity instanceof LivingEntity || entity.isOnFire();
        return true;
    }
}
