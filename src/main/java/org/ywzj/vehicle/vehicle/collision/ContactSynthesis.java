package org.ywzj.vehicle.vehicle.collision;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.collision.CollisionProvider;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds contact points from real geometry instead of a fixed hull sampling grid,
 * generating contacts only over the area that actually touches rather than at every point on
 * the hull's surface. Contacts are still carried as hull-local positions and face IDs,
 * with multiple samples per box to preserve the contact footprint's extent in the support polygon.
 */
public final class ContactSynthesis {

    /**
     * Samples per axis across one contact footprint; a box contributes at most 16 contacts.
     */
    private static final int MAX_SAMPLES_PER_AXIS = 4;

    /** Outward offset applied to contact points on the hull face. */
    private static final float FACE_OFFSET = 0.001f;

    /** Tangential margin for footprint sampling to match the grid's edge coverage. */
    private static final float FACE_GAP = 0.1f;

    /**
     * How far below a contact to look for the cell that owns it. A fence or wall reaches half a
     * block above its own cell, so a hull resting on one lands in the empty cell above it.
     */
    private static final int OWNER_SEARCH_DEPTH = 3;

    /**
     * Speculative contact margin in blocks. Contacts are generated before geometric overlap
     * to prevent the sweep and contact phase from deadlocking when a hull is held just clear
     * of the ground; sized to cover the gap the sweep deliberately introduces.
     */
    public static final double CONTACT_MARGIN = 0.05;

    /**
     * How far inside the clipped footprint the extreme samples sit. The containment test below is
     * half-open like a block cell, so a sample placed exactly on the far edge belongs to the next
     * cell along and would be thrown away.
     */
    private static final float EDGE_INSET = 0.001f;

    /**
     * Interior rows added between the extremes on a footprint wider than a block. The support
     * polygon only needs the extremes, but contact-driven block breaking works from the cells
     * samples land in, so a wide face still has to put something between its corners.
     */
    private static final int MAX_INTERIOR_SAMPLES = 2;

    /**
     * Per-vehicle store of contact points, refilled each tick instead of reallocated. A hull on
     * broken ground generates hundreds of these, and they never outlive the tick that made them.
     *
     * <p>Points handed out here are recycled on the next {@link #reset}, so nothing may retain one
     * past the solve that produced it.
     */
    public static final class ContactPool {

        private final List<VehicleCubeOBB.CubePoint> points = new ArrayList<>();
        private int used;

        /** Hands every point back. Call once per solve, before any contact is generated. */
        public void reset() {
            used = 0;
        }

        VehicleCubeOBB.CubePoint take(VehicleCubeOBB hull, float lx, float ly, float lz,
                                      VehicleCubeOBB.CubeFace face) {
            if (used == points.size()) {
                points.add(new VehicleCubeOBB.CubePoint(hull, new Vector3f(), face));
            }
            VehicleCubeOBB.CubePoint point = points.get(used++);
            point.reuse(hull, lx, ly, lz, face);
            return point;
        }

        /** Takes back the point handed out last, when it turned out to touch nothing. */
        void releaseLast() {
            if (used > 0) {
                used--;
            }
        }

    }

    private ContactSynthesis() {}

    /**
     * Fills in what a candidate contact touched, or reports that it touched nothing.
     * Called once per generated point after it has been proven to lie inside the box.
     */
    @FunctionalInterface
    public interface ContactResolver {

        /**
         * @param worldPos the candidate position, owned by the caller and reused, so do not retain it
         * @return true to keep the point, having filled in its cubePointContext
         */
        boolean resolve(VehicleCubeOBB.CubePoint point, Vector3f worldPos, AABB box);

    }

    /**
     * Resolves contacts against world blocks, reading the snapshot the boxes themselves came from.
     */
    public static ContactResolver blocks(ChunkCollisionCache.Cursor cursor) {
        return (point, worldPos, box) -> {
            int blockX = Mth.floor(Mth.clamp(point.worldX(), box.minX, box.maxX - 1.0e-6));
            int blockZ = Mth.floor(Mth.clamp(point.worldZ(), box.minZ, box.maxZ - 1.0e-6));
            int from = Mth.floor(Mth.clamp(point.worldY(), box.minY, box.maxY - 1.0e-6));
            int to = Math.max(Mth.floor(box.minY), from - OWNER_SEARCH_DEPTH);
            // A box can stand taller than the cell that owns it, so the cell the contact lands in
            // is not always the one holding the block. Walk down to find it.
            for (int blockY = from; blockY >= to; blockY--) {
                BlockState state = cursor.collisionAt(blockX, blockY, blockZ);
                if (state == null) {
                    continue;
                }
                point.cubePointContext.setWorldCell(blockX, blockY, blockZ);
                point.cubePointContext.setBlockState(state);
                point.cubePointContext.setSurfaceY(cursor.collisionTop(blockX, blockY, blockZ));
                return true;
            }
            return false;
        };
    }

    /**
     * Resolves what a hull sample point is standing in, for the grid query.
     */
    public static boolean resolveColumn(ChunkCollisionCache.Cursor cursor,
                                        VehicleCubeOBB.CubePoint point) {
        VehicleCubeOBB.CubePointContext context = point.cubePointContext;
        int blockX = Mth.floor(point.worldX());
        int blockZ = Mth.floor(point.worldZ());
        int blockY = Mth.floor(point.worldY());
        BlockState state = cursor.collisionAt(blockX, blockY, blockZ);
        if (state == null) {
            blockY--;
            state = cursor.collisionAt(blockX, blockY, blockZ);
            // Margin applies here too; a point held above the surface by the sweep would be
            // judged as standing on nothing without it.
            if (state == null
                    || cursor.collisionTop(blockX, blockY, blockZ) + CONTACT_MARGIN <= point.worldY()) {
                return false;
            }
        }
        context.setWorldCell(blockX, blockY, blockZ);
        context.setBlockState(state);
        context.setSurfaceY(cursor.collisionTop(blockX, blockY, blockZ));
        return true;
    }

    /**
     * Resolves contacts against one provider session's own geometry.
     */
    public static ContactResolver provider(CollisionProvider.Session session) {
        return (point, worldPos, box) -> {
            CollisionProvider.Contact contact = session.contactAt(point, worldPos);
            if (contact == null) {
                return false;
            }
            point.cubePointContext.setProviderCell(contact.blockPos());
            point.cubePointContext.setBlockState(contact.state());
            // Providers report a position and a state but no geometry, so downstream falls back
            // to estimating the surface from the state.
            point.cubePointContext.setSurfaceY(Double.NaN);
            return true;
        };
    }

    /**
     * Appends contacts between the hull and each candidate box to out.
     * @param boxes world-space collision boxes overlapping the hull's bound
     * @param resolver decides what, if anything, each generated point touched
     */
    public static void collect(VehicleCubeOBB hull, OBB pose, Vector3f[] axes, List<AABB> boxes,
                               ContactResolver resolver, List<VehicleCubeOBB.CubePoint> out) {
        BoxBuffer buffer = new BoxBuffer(boxes.size());
        buffer.addAll(boxes);
        collect(hull, pose, axes, buffer, resolver, null, out);
    }

    /** As above, drawing its contact points from a pool. */
    public static void collect(VehicleCubeOBB hull, OBB pose, Vector3f[] axes, List<AABB> boxes,
                               ContactResolver resolver, @Nullable ContactPool pool,
                               List<VehicleCubeOBB.CubePoint> out) {
        BoxBuffer buffer = new BoxBuffer(boxes.size());
        buffer.addAll(boxes);
        collect(hull, pose, axes, buffer, resolver, pool, out);
    }

    /** As below, allocating a fresh point per contact; for callers with no pool of their own. */
    public static void collect(VehicleCubeOBB hull, OBB pose, Vector3f[] axes, BoxBuffer boxes,
                               ContactResolver resolver, List<VehicleCubeOBB.CubePoint> out) {
        collect(hull, pose, axes, boxes, resolver, null, out);
    }

    /**
     * Reads boxes as primitives instead of AABB objects.
     * @param pose the hull's OBB at the pose being sampled; a shadow copy whose centre may have
     *             advanced by substeps while the live OBB has not
     */
    public static void collect(VehicleCubeOBB hull, OBB pose, Vector3f[] axes, BoxBuffer boxes,
                               ContactResolver resolver, @Nullable ContactPool pool,
                               List<VehicleCubeOBB.CubePoint> out) {
        OBB obb = pose;
        Vector3f extents = obb.extents();
        Vector3f localMin = new Vector3f();
        Vector3f localMax = new Vector3f();
        Vector3f corner = new Vector3f();
        Vector3f cornerLocal = new Vector3f();
        Vector3f local = new Vector3f();
        float[] uSamples = new float[MAX_SAMPLES_PER_AXIS + 1];
        float[] vSamples = new float[MAX_SAMPLES_PER_AXIS + 1];
        // A hair above the plane rather than on it: PhysicsEngine compares against a double and
        // these are floats, so a sample sitting exactly on the boundary could round to the
        // ignored side and quietly undo the point of placing it.
        float skirtSample = (float) (hull.climbSkirt() + FACE_OFFSET);

        // Hoisted out of the loop: identical for every box, and building it per box was three
        // quaternion transforms and four objects each time. The frame also precomputes the
        // absolute-term table once, so the per-box test below is bare arithmetic.
        OBB.SatFrame frame = new OBB.SatFrame().set(obb.rotation());
        Vector3f centre = obb.center();
        for (int i = 0, size = boxes.size(); i < size; i++) {
            // Grown by the speculative margin; a box the hull is about to rest on counts as touching.
            // The true box is kept separate so the resolver can clamp the sample back into it.
            double boxMinX = boxes.minX(i) - CONTACT_MARGIN;
            double boxMinY = boxes.minY(i) - CONTACT_MARGIN;
            double boxMinZ = boxes.minZ(i) - CONTACT_MARGIN;
            double boxMaxX = boxes.maxX(i) + CONTACT_MARGIN;
            double boxMaxY = boxes.maxY(i) + CONTACT_MARGIN;
            double boxMaxZ = boxes.maxZ(i) + CONTACT_MARGIN;
            if (!OBB.intersectsBox(frame, centre.x, centre.y, centre.z, extents,
                    boxMinX, boxMinY, boxMinZ, boxMaxX, boxMaxY, boxMaxZ)) {
                continue;
            }
            // Only now is an object worth building. The broad phase is inflated well past the
            // hull, so most candidates are rejected above and never become one; the resolver's
            // signature needs a real box, but only for the few that genuinely touch.
            AABB box = boxes.get(i);

            // The box's span in hull-local space. Conservative for a rotated hull, but every
            // generated point is checked against the box below, so it cannot invent contacts.
            localMin.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
            localMax.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
            for (int c = 0; c < 8; c++) {
                corner.set(
                        (float) ((c & 1) == 0 ? boxMinX : boxMaxX),
                        (float) ((c & 2) == 0 ? boxMinY : boxMaxY),
                        (float) ((c & 4) == 0 ? boxMinZ : boxMaxZ));
                // Into the reused scratch. The allocating overload made sixteen vectors per box.
                obb.worldToLocal(corner, axes, cornerLocal);
                localMin.min(cornerLocal);
                localMax.max(cornerLocal);
            }

            int face = contactFace(obb, box, axes);
            int normalAxis = face >> 1;
            boolean positive = (face & 1) == 0;
            int uAxis = (normalAxis + 1) % 3;
            int vAxis = (normalAxis + 2) % 3;

            float uLimit = extents.get(uAxis) + FACE_GAP;
            float vLimit = extents.get(vAxis) + FACE_GAP;
            float uFrom = Math.max(localMin.get(uAxis), -uLimit);
            float uTo = Math.min(localMax.get(uAxis), uLimit);
            float vFrom = Math.max(localMin.get(vAxis), -vLimit);
            float vTo = Math.min(localMax.get(vAxis), vLimit);
            if (uFrom > uTo || vFrom > vTo) {
                continue;
            }

            float normalCoord = positive
                    ? extents.get(normalAxis) + FACE_OFFSET
                    : -extents.get(normalAxis) - FACE_OFFSET;
            // Sampling the hull's face plane assumes the box lies beyond it, which stops being
            // true once boxes are sub-block: a half-height step the hull has driven into, or the
            // tread of a stair, sits entirely inside the hull and every sample would land just
            // outside it and be thrown away. Slide the plane onto the nearest surface of the box,
            // then a hair further in so the half-open test above still accepts it. Bounded by the
            // box's own span, so this can only ever move a sample onto geometry.
            float nearest = Mth.clamp(normalCoord, localMin.get(normalAxis), localMax.get(normalAxis));
            if (nearest != normalCoord) {
                normalCoord = nearest > normalCoord ? nearest + FACE_OFFSET : nearest - FACE_OFFSET;
            }
            // On a side face, one of the two tangent axes is the hull's own Y, and that is the
            // axis the climb skirt is measured along. Sampling it there is what makes "does this
            // block?" a question about the geometry rather than about where the even spacing
            // happened to put its rows.
            int uSteps = axisSamples(uFrom, uTo, uAxis == 1 ? skirtSample : Float.NaN, uSamples);
            int vSteps = axisSamples(vFrom, vTo, vAxis == 1 ? skirtSample : Float.NaN, vSamples);

            for (int u = 0; u < uSteps; u++) {
                float uPos = uSamples[u];
                for (int v = 0; v < vSteps; v++) {
                    float vPos = vSamples[v];
                    local.set(0, 0, 0);
                    local.setComponent(normalAxis, normalCoord);
                    local.setComponent(uAxis, uPos);
                    local.setComponent(vAxis, vPos);

                    VehicleCubeOBB.CubePoint point = pool == null
                            ? new VehicleCubeOBB.CubePoint(hull, new Vector3f(local), FACES[face])
                            : pool.take(hull, local.x, local.y, local.z, FACES[face]);
                    Vector3f world = point.worldPos(obb, axes);
                    // The footprint was clipped from an axis-aligned bound of a possibly rotated
                    // box, and the sample sits on the hull's face rather than the box's. Testing
                    // the box directly is what makes the contact set a subset of the real
                    // geometry: a box that stops short of the face contributes nothing, where a
                    // cell-granular test would have accepted the whole cell it sits in.
                    // Against the grown bounds, not the true box: a sample sitting in the gap the
                    // sweep leaves above a surface is exactly the contact that has to survive.
                    if (!contains(point, boxMinX, boxMinY, boxMinZ, boxMaxX, boxMaxY, boxMaxZ)
                            || !resolver.resolve(point, world, box)) {
                        if (pool != null) {
                            pool.releaseLast();
                        }
                        continue;
                    }
                    out.add(point);
                }
            }
        }
    }

    /**
     * Containment test against a box the point is expected to be just inside of.
     * Half-open on the far face, like a block cell; a point at exactly the maximum boundary
     * belongs to the adjacent cell, preserving agreement with the cell-based grid query.
     */
    private static boolean contains(VehicleCubeOBB.CubePoint point,
                                    double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        return point.worldX() >= minX && point.worldX() < maxX
                && point.worldY() >= minY && point.worldY() < maxY
                && point.worldZ() >= minZ && point.worldZ() < maxZ;
    }

    /**
     * Indexed by axis * 2 + (negative ? 1 : 0), with faces ordered as LEFT, RIGHT, TOP, BOTTOM, FRONT, BACK.
     */
    private static final VehicleCubeOBB.CubeFace[] FACES = {
            VehicleCubeOBB.CubeFace.LEFT, VehicleCubeOBB.CubeFace.RIGHT,
            VehicleCubeOBB.CubeFace.TOP, VehicleCubeOBB.CubeFace.BOTTOM,
            VehicleCubeOBB.CubeFace.FRONT, VehicleCubeOBB.CubeFace.BACK
    };

    /**
     * Picks the hull face a box is bearing on; the hull axis with the least overlap is the
     * direction of contact. Restricted to the hull's three axes to handle zero-overlap contacts
     * where a vehicle rests on the ground with no penetration.
     * @return axis * 2 + (negative ? 1 : 0)
     */
    private static int contactFace(OBB obb, AABB box, Vector3f[] axes) {
        Vector3f extents = obb.extents();
        Vector3f centre = obb.center();
        double halfX = box.getXsize() * 0.5;
        double halfY = box.getYsize() * 0.5;
        double halfZ = box.getZsize() * 0.5;
        double offsetX = box.getCenter().x - centre.x;
        double offsetY = box.getCenter().y - centre.y;
        double offsetZ = box.getCenter().z - centre.z;

        int bestAxis = 1;
        double bestOverlap = Double.MAX_VALUE;
        double bestDistance = -1;
        for (int axis = 0; axis < 3; axis++) {
            Vector3f direction = axes[axis];
            double boxRadius = Math.abs(direction.x) * halfX
                    + Math.abs(direction.y) * halfY
                    + Math.abs(direction.z) * halfZ;
            double distance = offsetX * direction.x + offsetY * direction.y + offsetZ * direction.z;
            double overlap = boxRadius + extents.get(axis) - Math.abs(distance);
            if (overlap < bestOverlap) {
                bestOverlap = overlap;
                bestAxis = axis;
                bestDistance = distance;
            }
        }
        return bestAxis * 2 + (bestDistance < 0 ? 1 : 0);
    }

    /**
     * Fills out with the footprint's two extremes, any interior rows a wide footprint needs, and
     * one extra at the given position if it lies inside.
     *
     * <p>The extremes are what the hull's support polygon is built from, and evenly spacing a
     * fixed number of samples across the footprint never placed one there: a box merged across
     * sixteen blocks got four rows spread over it, so the polygon came out narrower than the
     * ground the hull was actually standing on. Sampling the edges costs the same and reports the
     * real extent.
     *
     * @return the number of entries written
     */
    private static int axisSamples(float from, float to, float extra, float[] out) {
        float span = to - from;
        int count = 0;
        if (span <= 2 * EDGE_INSET) {
            out[count++] = (from + to) * 0.5f;
        } else {
            float lo = from + EDGE_INSET;
            float hi = to - EDGE_INSET;
            out[count++] = lo;
            int interior = Mth.clamp(Mth.floor(span) - 1, 0, MAX_INTERIOR_SAMPLES);
            for (int i = 1; i <= interior; i++) {
                out[count++] = lo + (hi - lo) * i / (interior + 1);
            }
            out[count++] = hi;
        }
        if (extra >= from && extra <= to) {
            out[count++] = extra;
        }
        return count;
    }

}
