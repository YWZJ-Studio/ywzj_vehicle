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

import java.util.List;

/**
 * Builds contact points from real geometry instead of a fixed hull sampling grid.
 * <p>
 * The grid approach asks "is there something under each of my N surface points?", so it costs
 * O(hull surface area) whether or not anything is nearby. This asks the opposite question —
 * "which boxes overlap my hull?" — and only then generates contact points, over the area that
 * actually touches. Cost becomes O(contact area).
 * <p>
 * Contacts are still {@link VehicleCubeOBB.CubePoint}s carrying a face and a hull-local position,
 * because {@code PhysicsEngine} reasons in those terms: it cancels velocity per face, and builds
 * its support polygon from local positions. The change is where the points come from, not what
 * they are.
 * <p>
 * <b>Why a small grid per contact rather than one point per box.</b> The support polygon in
 * {@code rotAndFallByGravity} is the convex hull of the contact positions. A hull resting on one
 * large merged ground box would collapse to a single contact and tip over instantly. Sampling the
 * overlap footprint preserves its extent, which is the property the polygon actually needs.
 * <p>
 * <b>Where the boxes come from is not this class's business.</b> World blocks arrive from
 * {@link ChunkCollisionCache#collectBoxes}; a {@link CollisionProvider} contributes its own the
 * same way. Both go through the same footprint sampling and the same verification, so a
 * contraption deck and a stone floor produce contacts that {@code PhysicsEngine} cannot tell
 * apart — which is the point.
 */
public final class ContactSynthesis {

    /**
     * Samples per tangent axis across one contact footprint, so a box contributes at most 16
     * contacts. Enough to carry the footprint's extent into the support polygon; small enough
     * that a hull flat on the ground produces tens of contacts instead of hundreds.
     */
    private static final int MAX_SAMPLES_PER_AXIS = 4;

    /** Matches the outward offset {@code VehicleCubeOBB.initCubePoints} places its points at. */
    private static final float FACE_OFFSET = 0.001f;

    /** Matches the tangential overshoot the sampling grid used, so edge coverage is unchanged. */
    private static final float FACE_GAP = 0.1f;

    /**
     * How far below a contact to look for the cell that owns it. A fence or wall reaches half a
     * block above its own cell, so a hull resting on one lands in the empty cell above it.
     */
    private static final int OWNER_SEARCH_DEPTH = 3;

    private ContactSynthesis() {}

    /**
     * Fills in what a candidate contact touched, or reports that it touched nothing.
     * <p>
     * Called once per generated point, after the point has been proven to lie inside the box it
     * came from. A resolver may still reject: a provider's boxes are allowed to be conservative,
     * and this is where that slack is taken back out.
     */
    @FunctionalInterface
    public interface ContactResolver {

        /**
         * @param worldPos the candidate position. Owned by the caller and reused — read it, do
         *                 not retain it.
         * @return true to keep the point, having filled in its {@code cubePointContext}
         */
        boolean resolve(VehicleCubeOBB.CubePoint point, Vector3f worldPos, AABB box);

    }

    /**
     * Resolves contacts against world blocks, reading the snapshot the boxes themselves came from.
     */
    public static ContactResolver blocks(ChunkCollisionCache.Cursor cursor) {
        return (point, worldPos, box) -> {
            int blockX = Mth.floor(Mth.clamp(worldPos.x, box.minX, box.maxX - 1.0e-6));
            int blockZ = Mth.floor(Mth.clamp(worldPos.z, box.minZ, box.maxZ - 1.0e-6));
            int from = Mth.floor(Mth.clamp(worldPos.y, box.minY, box.maxY - 1.0e-6));
            int to = Math.max(Mth.floor(box.minY), from - OWNER_SEARCH_DEPTH);
            // A box can stand taller than the cell that owns it, so the cell the contact lands in
            // is not always the one holding the block. Walk down to find it.
            for (int blockY = from; blockY >= to; blockY--) {
                BlockState state = cursor.collisionAt(blockX, blockY, blockZ);
                if (state == null) {
                    continue;
                }
                // Same value as Vec3.atBottomCenterOf(BlockPos.containing(worldPos))
                point.cubePointContext.setBlockPos(new Vec3(blockX + 0.5, blockY, blockZ + 0.5));
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
                                        VehicleCubeOBB.CubePointContext context, Vector3f worldPos) {
        int blockX = Mth.floor(worldPos.x);
        int blockZ = Mth.floor(worldPos.z);
        int blockY = Mth.floor(worldPos.y);
        BlockState state = cursor.collisionAt(blockX, blockY, blockZ);
        if (state == null) {
            blockY--;
            state = cursor.collisionAt(blockX, blockY, blockZ);
            if (state == null || cursor.collisionTop(blockX, blockY, blockZ) <= worldPos.y) {
                return false;
            }
        }
        // Same value as Vec3.atBottomCenterOf(BlockPos.containing(worldPos))
        context.setBlockPos(new Vec3(blockX + 0.5, blockY, blockZ + 0.5));
        context.setBlockState(state);
        context.setSurfaceY(cursor.collisionTop(blockX, blockY, blockZ));
        return true;
    }

    /**
     * Resolves contacts against one provider session's own geometry.
     * <p>
     * The provider's boxes drove the broad phase; its {@code contactAt} is still what decides,
     * so a session whose boxes are a loose bound over a rotated sub-level stays exact.
     */
    public static ContactResolver provider(CollisionProvider.Session session) {
        return (point, worldPos, box) -> {
            CollisionProvider.Contact contact = session.contactAt(point, worldPos);
            if (contact == null) {
                return false;
            }
            point.cubePointContext.setBlockPos(contact.blockPos());
            point.cubePointContext.setBlockState(contact.state());
            // Providers report a position and a state but no geometry, so downstream falls back
            // to estimating the surface from the state.
            point.cubePointContext.setSurfaceY(Double.NaN);
            return true;
        };
    }

    /**
     * Appends contacts between the hull and each candidate box to {@code out}.
     *
     * @param boxes    world-space collision boxes overlapping the hull's bound
     * @param resolver decides what, if anything, each generated point touched
     */
    public static void collect(VehicleCubeOBB hull, Vector3f[] axes, List<AABB> boxes,
                               ContactResolver resolver, List<VehicleCubeOBB.CubePoint> out) {
        BoxBuffer buffer = new BoxBuffer(boxes.size());
        buffer.addAll(boxes);
        collect(hull, axes, buffer, resolver, out);
    }

    /**
     * As {@link #collect(VehicleCubeOBB, Vector3f[], List, ContactResolver, List)}, reading boxes
     * as primitives. The block path uses this; the list form remains for {@code CollisionProvider},
     * whose public API hands back real {@link AABB}s.
     */
    public static void collect(VehicleCubeOBB hull, Vector3f[] axes, BoxBuffer boxes,
                               ContactResolver resolver, List<VehicleCubeOBB.CubePoint> out) {
        OBB obb = hull.obb();
        Vector3f extents = obb.extents();
        Vector3f localMin = new Vector3f();
        Vector3f localMax = new Vector3f();
        Vector3f corner = new Vector3f();
        Vector3f local = new Vector3f();
        float[] uSamples = new float[MAX_SAMPLES_PER_AXIS + 1];
        float[] vSamples = new float[MAX_SAMPLES_PER_AXIS + 1];
        // A hair above the plane rather than on it: PhysicsEngine compares against a double and
        // these are floats, so a sample sitting exactly on the boundary could round to the
        // ignored side and quietly undo the point of placing it.
        float skirtSample = (float) (hull.climbSkirt() + FACE_OFFSET);

        // Hoisted out of the loop: identical for every box, and building it per box was three
        // quaternion transforms and four objects each time.
        Matrix3f basis = obb.rotation().get(new Matrix3f());
        Vector3f centre = obb.center();
        for (int i = 0, size = boxes.size(); i < size; i++) {
            double boxMinX = boxes.minX(i);
            double boxMinY = boxes.minY(i);
            double boxMinZ = boxes.minZ(i);
            double boxMaxX = boxes.maxX(i);
            double boxMaxY = boxes.maxY(i);
            double boxMaxZ = boxes.maxZ(i);
            if (!OBB.intersectsBox(basis, centre.x, centre.y, centre.z, extents,
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
                Vector3f localCorner = obb.worldToLocal(corner, axes);
                localMin.min(localCorner);
                localMax.max(localCorner);
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

                    Vector3f world = obb.localToWorld(local, axes, corner);
                    // The footprint was clipped from an axis-aligned bound of a possibly rotated
                    // box, and the sample sits on the hull's face rather than the box's. Testing
                    // the box directly is what makes the contact set a subset of the real
                    // geometry: a box that stops short of the face contributes nothing, where a
                    // cell-granular test would have accepted the whole cell it sits in.
                    if (!contains(box, world)) {
                        continue;
                    }
                    VehicleCubeOBB.CubePoint point =
                            new VehicleCubeOBB.CubePoint(hull, new Vector3f(local), FACES[face]);
                    point.worldPos(axes);
                    if (resolver.resolve(point, world, box)) {
                        out.add(point);
                    }
                }
            }
        }
    }

    /**
     * Containment against a box the point is expected to be just inside of. The point sits
     * {@link #FACE_OFFSET} beyond the hull's face, so a hull merely resting on a box — touching
     * it with no penetration at all, which is what every stationary vehicle does — still lands
     * inside.
     * <p>
     * <b>Half-open on the far face, like a block cell.</b> A block at y=64 occupies [64, 65), and
     * a point at exactly 65.0 belongs to the air above it. Closing the interval instead put a
     * contact on the top edge of every one-block step — and that edge sits above the skirt
     * {@code motionByImpact} uses to decide what to drive over, so the step cancelled the
     * vehicle's forward velocity and could never be climbed. The old cell test got this right for
     * free by flooring the point; matching its convention is what keeps the two queries agreeing.
     */
    private static boolean contains(AABB box, Vector3f point) {
        return point.x >= box.minX && point.x < box.maxX
                && point.y >= box.minY && point.y < box.maxY
                && point.z >= box.minZ && point.z < box.maxZ;
    }

    /**
     * Indexed by {@code axis * 2 + (negative ? 1 : 0)}, matching the face conventions
     * {@code initCubePoints} uses: +X is LEFT, +Y is TOP, +Z is FRONT.
     */
    private static final VehicleCubeOBB.CubeFace[] FACES = {
            VehicleCubeOBB.CubeFace.LEFT, VehicleCubeOBB.CubeFace.RIGHT,
            VehicleCubeOBB.CubeFace.TOP, VehicleCubeOBB.CubeFace.BOTTOM,
            VehicleCubeOBB.CubeFace.FRONT, VehicleCubeOBB.CubeFace.BACK
    };

    /**
     * Picks the hull face a box is bearing on: the hull axis along which the two overlap least,
     * since that is the direction the box is pressing from.
     * <p>
     * Deliberately not {@code OBB.calculateMTV}. That searches all fifteen separating axes, so it
     * can return a face diagonal rather than one of the hull's own, and — more importantly — it
     * treats zero overlap as separation and returns a zero vector. A vehicle resting on the
     * ground penetrates it by nothing at all, which is precisely the case that has to work: an
     * earlier revision fell back to "whichever way the box lies" there and picked FRONT for a
     * pillar under the hull's corner, generating contacts on the wrong face and finding none.
     * Restricting the search to the hull's three axes and keeping zero-overlap contacts fixes it.
     *
     * @return {@code axis * 2 + (negative ? 1 : 0)}
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

    private static int sampleCount(float span) {
        if (span <= 1.0e-4f) {
            return 1;
        }
        return Mth.clamp(Mth.ceil(span) + 1, 1, MAX_SAMPLES_PER_AXIS);
    }

    /**
     * Fills {@code out} with evenly spaced coordinates across the footprint, plus one extra at
     * {@code extra} when that lies inside it. Pass {@link Float#NaN} for no extra.
     *
     * @return how many entries were written
     */
    private static int axisSamples(float from, float to, float extra, float[] out) {
        int steps = sampleCount(to - from);
        for (int i = 0; i < steps; i++) {
            out[i] = steps == 1 ? (from + to) * 0.5f : from + (to - from) * i / (steps - 1);
        }
        if (extra >= from && extra <= to) {
            out[steps++] = extra;
        }
        return steps;
    }

}
