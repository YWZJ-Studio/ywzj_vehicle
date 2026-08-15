package org.ywzj.vehicle.client.render.util;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;
import org.ywzj.vehicle.vehicle.collision.ContactSynthesis;
import org.ywzj.vehicle.vehicle.collision.SectionCollision;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the baked {@link SectionCollision} snapshots into something drawable, for the collision
 * debug overlay.
 * <p>
 * The greedy merge lives in {@link SectionCollision} itself and is what the collision query
 * consumes, so the overlay draws the same boxes physics collides against rather than a parallel
 * reconstruction of them.
 */
@OnlyIn(Dist.CLIENT)
public final class CollisionMeshDebug {

    /**
     * A merged run of collision geometry, in world coordinates. Sub-block now that boxes follow
     * real collision shapes, so a slab floor draws half a block tall.
     */
    public record MeshBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

        /** Whether this box reaches into a block cell at all — it no longer has to fill one. */
        public boolean containsBlock(int x, int y, int z) {
            return maxX > x && minX < x + 1 && maxY > y && minY < y + 1 && maxZ > z && minZ < z + 1;
        }

    }

    /** One section's worth of drawable data. */
    public record SectionMesh(long sectionKey, SectionCollision.Kind kind, List<MeshBox> boxes) {}

    private CollisionMeshDebug() {}

    /**
     * Collects the baked sections within {@code sectionRadius} of a block position that actually
     * hold geometry. Sections the cache never prepared, and sections it proved empty, are omitted
     * — an absent box is itself the information that nothing was baked there.
     */
    public static List<SectionMesh> meshesAround(Level level, BlockPos around, int sectionRadius) {
        ChunkCollisionCache cache = ChunkCollisionCache.of(level);
        int centreX = SectionPos.blockToSectionCoord(around.getX());
        int centreY = SectionPos.blockToSectionCoord(around.getY());
        int centreZ = SectionPos.blockToSectionCoord(around.getZ());

        List<SectionMesh> meshes = new ArrayList<>();
        for (int x = centreX - sectionRadius; x <= centreX + sectionRadius; x++) {
            for (int y = centreY - sectionRadius; y <= centreY + sectionRadius; y++) {
                for (int z = centreZ - sectionRadius; z <= centreZ + sectionRadius; z++) {
                    long key = SectionPos.asLong(x, y, z);
                    SectionCollision snapshot = cache.snapshotAt(key);
                    if (snapshot == null || snapshot.isEmpty()) {
                        continue;
                    }
                    meshes.add(new SectionMesh(key, snapshot.kind(), boxesFor(key, snapshot)));
                }
            }
        }
        return meshes;
    }

    private static List<MeshBox> boxesFor(long sectionKey, SectionCollision snapshot) {
        int originX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey));
        int originY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey));
        int originZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey));
        long[] packedBoxes = snapshot.collisionBoxes();
        List<MeshBox> boxes = new ArrayList<>(packedBoxes.length);
        for (long packed : packedBoxes) {
            double x = originX + SectionCollision.boxMinX(packed);
            double y = originY + SectionCollision.boxMinY(packed);
            double z = originZ + SectionCollision.boxMinZ(packed);
            boxes.add(new MeshBox(x, y, z,
                    x + SectionCollision.boxSizeX(packed),
                    y + SectionCollision.boxSizeY(packed),
                    z + SectionCollision.boxSizeZ(packed)));
        }
        return boxes;
    }

    /**
     * Block positions currently registering a contact against any nearby vehicle, packed with
     * {@link BlockPos#asLong}.
     * <p>
     * Runs whichever query is configured — inverted or grid — so the highlight shows what the
     * physics is genuinely reacting to. Recomputed client-side rather than synced, so there is no
     * round trip and no interpolation lag against the boxes it is highlighting.
     */
    public static LongSet contactedBlocks(Level level, List<AbstractVehicle> vehicles) {
        LongSet contacted = new LongOpenHashSet();
        if (vehicles.isEmpty()) {
            return contacted;
        }
        boolean inverted = AllConfigs.common.invertedCollisionQuery.get();
        ChunkCollisionCache cache = ChunkCollisionCache.of(level);
        ChunkCollisionCache.Cursor cursor = cache.cursor();
        List<AABB> boxes = new ArrayList<>();
        List<VehicleCubeOBB.CubePoint> contacts = new ArrayList<>();

        for (AbstractVehicle vehicle : vehicles) {
            VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
            if (mainCubeOBB == null) {
                continue;
            }
            Vector3f[] axes = mainCubeOBB.obb().getAxes();
            if (inverted) {
                boxes.clear();
                contacts.clear();
                cache.collectBoxes(vehicle.getBoundingBox().inflate(2.0), boxes);
                ContactSynthesis.collect(mainCubeOBB, axes, boxes,
                        ContactSynthesis.blocks(cursor), contacts);
                for (VehicleCubeOBB.CubePoint contact : contacts) {
                    net.minecraft.world.phys.Vec3 blockPos = contact.cubePointContext.blockPos();
                    contacted.add(BlockPos.asLong(
                            Mth.floor(blockPos.x), Mth.floor(blockPos.y), Mth.floor(blockPos.z)));
                }
            } else {
                for (VehicleCubeOBB.CubePoint point : mainCubeOBB.cubePoints()) {
                    Vector3f worldPos = point.worldPos(axes);
                    int x = Mth.floor(worldPos.x);
                    int y = Mth.floor(worldPos.y);
                    int z = Mth.floor(worldPos.z);
                    if (cursor.collisionAt(x, y, z) != null) {
                        contacted.add(BlockPos.asLong(x, y, z));
                    }
                }
            }
        }
        return contacted;
    }

    /** Kept for the overlay toggle; merged boxes are now cached on the snapshots themselves. */
    public static void clear() {}

}
