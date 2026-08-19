package org.ywzj.vehicle.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllKeys;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;
import org.ywzj.vehicle.vehicle.collision.SectionCollision;
import org.ywzj.vehicle.vehicle.structure.OBB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Draws collision cache snapshots in three overlays: section bounds tinted by storage class,
 * collision boxes in green, and any box holding a vehicle contact in red.
 * Sampled sections are drawn to see what the cache actually baked; absent sections are themselves
 * the point, as they are skipped by the broad-phase gate.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CollisionMeshRenderer {

    /** Sections either side of the camera to draw. Kept small; this is a lot of line geometry. */
    private static final int SECTION_RADIUS = 2;

    /** Columns either side of a vehicle to sample for the gradient field. */
    private static final int GRADIENT_RADIUS = 8;

    /**
     * Overlay mode cycled by the toggle key.
     * Boxes and gradient are drawn separately; the binding cycles through all modes.
     */
    public enum Mode {

        OFF("off"),
        BOXES("collision boxes"),
        GRADIENT("ground gradient"),
        BOTH("boxes + gradient");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        boolean boxes() {
            return this == BOXES || this == BOTH;
        }

        boolean gradient() {
            return this == GRADIENT || this == BOTH;
        }

    }

    private static final Mode[] MODES = Mode.values();

    private static Mode mode = Mode.OFF;

    private CollisionMeshRenderer() {}

    public static boolean isEnabled() {
        return mode != Mode.OFF;
    }

    public static Mode mode() {
        return mode;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (AllKeys.TOGGLE_COLLISION_DEBUG.consumeClick()) {
            mode = MODES[(mode.ordinal() + 1) % MODES.length];
            if (mode == Mode.OFF) {
                CollisionMeshDebug.clear();
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("Vehicle collision overlay: " + mode.label), true);
            }
        }
        if (mode == Mode.OFF) {
            return;
        }
        // The client never runs tickPhysics; prepare the cache here so the overlay shows what
        // physics actually sees rather than an empty world.
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity cameraEntity = minecraft.getCameraEntity();
        if (level == null || cameraEntity == null) {
            return;
        }
        double reach = SECTION_RADIUS * 16.0 + 8.0;
        ChunkCollisionCache.of(level).prepare(level, cameraEntity.getBoundingBox().inflate(reach));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (mode == Mode.OFF || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Vec3 cameraPos = event.getCamera().getPosition();
        BlockPos around = BlockPos.containing(cameraPos);
        Frame frame = frameFor(level, around);
        if (frame.meshes.isEmpty() && frame.fields.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(DebugRenderTypes.LINES_NO_DEPTH);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        if (mode.boxes()) {
            for (CollisionMeshDebug.SectionMesh mesh : frame.meshes) {
                drawSectionBounds(poseStack, buffer, mesh);
                for (CollisionMeshDebug.MeshBox box : mesh.boxes()) {
                    boolean touching = frame.touching.contains(box);
                    LevelRenderer.renderLineBox(poseStack, buffer,
                            box.minX(), box.minY(), box.minZ(),
                            box.maxX(), box.maxY(), box.maxZ(),
                            touching ? 1.0F : 0.1F,
                            touching ? 0.1F : 1.0F,
                            0.1F,
                            touching ? 1.0F : 0.55F);
                }
            }
        }
        if (mode.gradient()) {
            for (GradientMeshDebug.Field field : frame.fields) {
                drawGradientField(poseStack, buffer, field);
            }
        }

        poseStack.popPose();
        bufferSource.endBatch(DebugRenderTypes.LINES_NO_DEPTH);
    }

    /**
     * Draws each column's drivable surface and the trimmed sweep hull.
     * Column height is tinted by what the climb path verdicts, showing whether the vehicle
     * agrees with what the terrain actually is.
     */
    private static void drawGradientField(PoseStack poseStack, VertexConsumer buffer,
                                          GradientMeshDebug.Field field) {
        for (GradientMeshDebug.Cell cell : field.cells()) {
            float red;
            float green;
            float alpha;
            switch (cell.verdict()) {
                case WALL -> {
                    red = 1.0F;
                    green = 0.15F;
                    alpha = 0.95F;
                }
                case SLOPE -> {
                    // Ramp green through amber as the step cost approaches the maximum rise.
                    float t = (float) Mth.clamp(cell.rise() / field.maxUpStep(), 0.0, 1.0);
                    red = t;
                    green = 1.0F;
                    alpha = 0.45F + 0.35F * t;
                }
                default -> {
                    red = 0.25F;
                    green = 0.7F;
                    alpha = 0.3F;
                }
            }
            double x = cell.x();
            double z = cell.z();
            double y = cell.top() + 0.01;
            line(poseStack, buffer, x, y, z, x + 1, y, z, red, green, 0.2F, alpha);
            line(poseStack, buffer, x + 1, y, z, x + 1, y, z + 1, red, green, 0.2F, alpha);
            line(poseStack, buffer, x + 1, y, z + 1, x, y, z + 1, red, green, 0.2F, alpha);
            line(poseStack, buffer, x, y, z + 1, x, y, z, red, green, 0.2F, alpha);
        }
        drawOBB(poseStack, buffer, field.sweepHull(), 0.3F, 0.6F, 1.0F, 0.9F);
    }

    /** The 12 edges of a rotated box; LevelRenderer only outlines axis-aligned boxes. */
    private static void drawOBB(PoseStack poseStack, VertexConsumer buffer, OBB obb,
                                float red, float green, float blue, float alpha) {
        Vector3f[] v = obb.getVertices();
        int[] edges = {0, 1, 1, 5, 5, 4, 4, 0, 3, 2, 2, 6, 6, 7, 7, 3, 0, 3, 1, 2, 5, 6, 4, 7};
        for (int i = 0; i < edges.length; i += 2) {
            Vector3f a = v[edges[i]];
            Vector3f b = v[edges[i + 1]];
            line(poseStack, buffer, a.x, a.y, a.z, b.x, b.y, b.z, red, green, blue, alpha);
        }
    }

    private static void line(PoseStack poseStack, VertexConsumer buffer,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             float red, float green, float blue, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0e-6F) {
            return;
        }
        nx /= length;
        ny /= length;
        nz /= length;
        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
    }

    private record Frame(long gameTime, long cameraSection, Mode mode,
                         List<CollisionMeshDebug.SectionMesh> meshes,
                         Set<CollisionMeshDebug.MeshBox> touching,
                         List<GradientMeshDebug.Field> fields) {}

    private static Frame lastFrame;

    /**
     * Gathers the drawable set once per tick to avoid recomputing contacts every frame.
     */
    private static Frame frameFor(ClientLevel level, BlockPos around) {
        long gameTime = level.getGameTime();
        long cameraSection = SectionPos.asLong(around);
        // Mode is part of the key: without it, cycling mid-tick reuses a frame gathered for the
        // previous mode and the new overlay is missing for a frame.
        if (lastFrame != null && lastFrame.gameTime == gameTime
                && lastFrame.cameraSection == cameraSection && lastFrame.mode == mode) {
            return lastFrame;
        }

        List<AbstractVehicle> vehicles = nearbyVehicles(level, around);
        List<GradientMeshDebug.Field> fields = new ArrayList<>();
        if (mode.gradient()) {
            for (AbstractVehicle vehicle : vehicles) {
                GradientMeshDebug.Field field =
                        GradientMeshDebug.around(level, vehicle, GRADIENT_RADIUS);
                if (field != null) {
                    fields.add(field);
                }
            }
        }
        if (!mode.boxes()) {
            lastFrame = new Frame(gameTime, cameraSection, mode, List.of(), Set.of(), fields);
            return lastFrame;
        }

        List<CollisionMeshDebug.SectionMesh> meshes =
                CollisionMeshDebug.meshesAround(level, around, SECTION_RADIUS);
        Set<CollisionMeshDebug.MeshBox> touching = Collections.newSetFromMap(new IdentityHashMap<>());
        LongSet contacted = CollisionMeshDebug.contactedBlocks(level, vehicles);
        if (!contacted.isEmpty()) {
            // Walk contacts to find their boxes; contacts are few, merged boxes are many.
            for (long packed : contacted) {
                BlockPos pos = BlockPos.of(packed);
                for (CollisionMeshDebug.SectionMesh mesh : meshes) {
                    for (CollisionMeshDebug.MeshBox box : mesh.boxes()) {
                        if (box.containsBlock(pos.getX(), pos.getY(), pos.getZ())) {
                            touching.add(box);
                        }
                    }
                }
            }
        }

        lastFrame = new Frame(gameTime, cameraSection, mode, meshes, touching, fields);
        return lastFrame;
    }

    /**
     * Draws section outlines tinted by storage class to show the palette gate working.
     */
    private static void drawSectionBounds(PoseStack poseStack, VertexConsumer buffer,
                                          CollisionMeshDebug.SectionMesh mesh) {
        long key = mesh.sectionKey();
        int x = SectionPos.sectionToBlockCoord(SectionPos.x(key));
        int y = SectionPos.sectionToBlockCoord(SectionPos.y(key));
        int z = SectionPos.sectionToBlockCoord(SectionPos.z(key));
        float red = mesh.kind() == SectionCollision.Kind.UNIFORM ? 0.3F : 0.55F;
        float green = mesh.kind() == SectionCollision.Kind.UNIFORM ? 0.6F : 0.55F;
        float blue = 1.0F;
        LevelRenderer.renderLineBox(poseStack, buffer, x, y, z, x + 16.0, y + 16.0, z + 16.0,
                red, green, blue, 0.25F);
    }

    private static List<AbstractVehicle> nearbyVehicles(ClientLevel level, BlockPos around) {
        List<AbstractVehicle> vehicles = new ArrayList<>();
        double reach = (SECTION_RADIUS + 1) * 16.0;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof AbstractVehicle vehicle
                    && vehicle.distanceToSqr(around.getX(), around.getY(), around.getZ()) < reach * reach) {
                vehicles.add(vehicle);
            }
        }
        return vehicles;
    }

}
