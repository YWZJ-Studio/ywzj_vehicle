package org.ywzj.vehicle.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.compat.IrisCompat;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.DecorationUnit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT)
public final class VehicleDecorationQueue {

    private static final List<Entry> PENDING = new ArrayList<>();
    private static final Map<ResourceLocation, List<Entry>> GROUPED = new LinkedHashMap<>();
    private static final PoseStack SCRATCH = new PoseStack();

    private VehicleDecorationQueue() {
    }

    private record Entry(DecorationUnit unit, Matrix4f pose, int packedLight) {
    }

    public static void enqueue(AbstractVehicle vehicle, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Map<String, DecorationUnit> units = vehicle.getDecorationUnits();
        if (units.isEmpty() || vehicle.getModelInstance() == null) {
            return;
        }
        if (IrisCompat.isRenderingShadowPass()) {
            for (DecorationUnit unit : units.values()) {
                unit.render(poseStack, bufferSource, packedLight);
            }
            return;
        }
        Matrix4f base = new Matrix4f(poseStack.last().pose());
        for (DecorationUnit unit : units.values()) {
            PENDING.add(new Entry(unit, base, packedLight));
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            PENDING.clear();
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            flush();
        }
    }

    private static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Entry entry : PENDING) {
            ResourceLocation texture = textureOf(entry.unit);
            if (texture == null) {
                continue;
            }
            GROUPED.computeIfAbsent(texture, key -> new ArrayList<>()).add(entry);
        }
        PENDING.clear();
        if (GROUPED.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        for (List<Entry> group : GROUPED.values()) {
            for (Entry entry : group) {
                render(entry, bufferSource, true, false);
            }
        }
        for (List<Entry> group : GROUPED.values()) {
            for (Entry entry : group) {
                render(entry, bufferSource, false, true);
            }
        }
        bufferSource.endLastBatch();
        GROUPED.clear();
    }

    private static void render(Entry entry, MultiBufferSource bufferSource, boolean base, boolean special) {
        SCRATCH.pushPose();
        try {
            SCRATCH.mulPose(entry.pose);
            entry.unit.render(SCRATCH, bufferSource, entry.packedLight, base, special);
        } finally {
            SCRATCH.popPose();
        }
    }

    private static ResourceLocation textureOf(DecorationUnit unit) {
        BaseDisplay display = ClientAssetsManager.INSTANCE.getDecorationDisplay(unit.getDisplayId()).orElse(null);
        return display == null ? null : display.getTexture();
    }

}
