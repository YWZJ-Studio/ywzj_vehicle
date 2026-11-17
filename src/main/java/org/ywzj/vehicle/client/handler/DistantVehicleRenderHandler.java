package org.ywzj.vehicle.client.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.client.render.DistantVehicleState;
import org.ywzj.vehicle.client.render.DistantVehicleState.Snapshot;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ServerBroadcastEntities;

import java.util.*;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, value = Dist.CLIENT)
public final class DistantVehicleRenderHandler {

    private static final Map<Integer, DistantVehicleState> VEHICLES = new HashMap<>();
    private static final Set<UUID> RENDERED = new HashSet<>();
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(262144));
    private static ClientLevel level;
    private static int clientTicks;
    private static int interval = 5;
    private static long lastServerTick = Long.MIN_VALUE;
    private static double clockRate = 1;
    private static double lastReceiveTime;

    public static void receive(ServerBroadcastEntities message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(message.dimension)) {
            return;
        }
        ensureLevel(mc.level);
        if (message.tick <= lastServerTick) {
            return;
        }
        double now = clientTicks + mc.getFrameTime();
        double oldTime = renderTime(now);
        if (lastServerTick != Long.MIN_VALUE && now > lastReceiveTime) {
            double rate = Mth.clamp((message.tick - lastServerTick) / (now - lastReceiveTime), 0.05, 1.25);
            clockRate += (rate - clockRate) * 0.25;
        }
        interval = message.interval;
        lastServerTick = message.tick;
        lastReceiveTime = now;
        Set<Integer> present = new HashSet<>();
        if (AllConfigs.server.extendedRenderDistance.get() > 0) {
            for (ServerBroadcastEntities.BroadcastEntity entity : message.entities) {
                if (entity.renderData() == null) {
                    continue;
                }
                Snapshot snapshot = Snapshot.of(entity);
                present.add(snapshot.id());
                DistantVehicleState state = VEHICLES.get(snapshot.id());
                if (state == null || !state.latest().uuid().equals(snapshot.uuid())
                        || !state.latest().type().equals(snapshot.type())
                        || !state.latest().vehicleId().equals(snapshot.vehicleId())) {
                    state = new DistantVehicleState();
                    VEHICLES.put(snapshot.id(), state);
                }
                state.accept(message.tick, snapshot, oldTime, renderTime(now), now, interval);
            }
        }
        VEHICLES.keySet().retainAll(present);
    }

    private static double renderTime(double now) {
        // 留出一个同步周期及少量抖动余量，按服务端时间在已知快照间播放。
        return lastServerTick + (now - lastReceiveTime) * clockRate - interval - Math.min(2, interval * 0.25);
    }

    private static void ensureLevel(ClientLevel current) {
        if (level != current) {
            VEHICLES.clear();
            RENDERED.clear();
            lastServerTick = Long.MIN_VALUE;
            clientTicks = 0;
            clockRate = 1;
            level = current;
        }
    }

    public static void onVehicleRendered(AbstractVehicle vehicle, float partialTick) {
        if (vehicle.remote || level != vehicle.level()) {
            return;
        }
        RENDERED.add(vehicle.getUUID());
        DistantVehicleState state = VEHICLES.get(vehicle.getId());
        if (state != null && state.latest().uuid().equals(vehicle.getUUID())) {
            state.rememberTracked(vehicle, partialTick, clientTicks + partialTick);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (level != null && !mc.isPaused()) {
            clientTicks++;
            if (clientTicks - lastReceiveTime > Math.max(100, interval * 5L)) {
                VEHICLES.clear();
                lastServerTick = Long.MIN_VALUE;
                clockRate = 1;
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() == level) {
            ensureLevel(null);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            RENDERED.clear();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        double distance = AllConfigs.server.extendedRenderDistance.get();
        if (level == null || mc.player == null || distance <= 0 || VEHICLES.isEmpty()
                || event.getCamera().getFluidInCamera() != FogType.NONE
                || mc.player.hasEffect(MobEffects.BLINDNESS) || mc.player.hasEffect(MobEffects.DARKNESS)) {
            return;
        }
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        double far = projection.m32() / (projection.m22() + 1.0);
        if (!Double.isFinite(far) || far <= 0) {
            return;
        }
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        try {
            double now = clientTicks + event.getPartialTick();
            Vec3 camera = event.getCamera().getPosition();
            for (DistantVehicleState state : VEHICLES.values()) {
                UUID uuid = state.latest().uuid();
                if (RENDERED.contains(uuid) || mc.player.getRootVehicle().getUUID().equals(uuid)
                        || (mc.getCameraEntity() != null && mc.getCameraEntity().getRootVehicle().getUUID().equals(uuid))) {
                    continue;
                }
                var tracked = level.getEntity(state.latest().id());
                if (tracked instanceof AbstractVehicle vehicle && vehicle.getUUID().equals(uuid)
                        && vehicle.isInvisible()) {
                    continue;
                }
                DistantVehicleState.Pose pose = state.renderPose(renderTime(now), now, interval);
                Vec3 offset = pose.position().subtract(camera);
                if (offset.lengthSqr() > distance * distance) {
                    continue;
                }
                float radius = state.latest().radius();
                // 远处模型等比例压入原投影的远裁剪面，保留屏幕尺寸和已渲染地形的深度遮挡。
                float scale = (float) Math.min(1, far * 0.8 / (offset.length() + radius));
                Vec3 renderOffset = offset.scale(scale);
                AABB bounds = AABB.ofSize(camera.add(renderOffset), radius * scale * 2,
                        radius * scale * 2, radius * scale * 2);
                if (!event.getFrustum().isVisible(bounds)) {
                    continue;
                }
                AbstractVehicle vehicle = state.vehicle(level);
                if (vehicle == null) {
                    continue;
                }
                state.applyPose(vehicle, pose, clientTicks);
                int light = level.hasChunkAt(vehicle.blockPosition())
                        ? LevelRenderer.getLightColor(level, vehicle.blockPosition()) : LightTexture.pack(0, 15);
                PoseStack stack = event.getPoseStack();
                stack.pushPose();
                try {
                    stack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
                    stack.scale(scale, scale, scale);
                    mc.getEntityRenderDispatcher().getRenderer(vehicle).render(vehicle, vehicle.getYRot(),
                            event.getPartialTick(), stack, BUFFERS, light);
                } finally {
                    stack.popPose();
                }
            }
        } finally {
            try {
                BUFFERS.endBatch();
            } finally {
                RenderSystem.setShaderFogStart(fogStart);
                RenderSystem.setShaderFogEnd(fogEnd);
                RENDERED.clear();
            }
        }
    }

}
