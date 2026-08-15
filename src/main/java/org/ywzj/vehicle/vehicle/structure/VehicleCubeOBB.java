package org.ywzj.vehicle.vehicle.structure;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * 使用基岩模型描述的载具结构块
 */
public class VehicleCubeOBB {

    private static final int MAX_SAMPLES_PER_AXIS = 24;

    /** How far outside its face a sample point sits, so a resting hull still registers. */
    public static final float POINT_OFFSET = 0.001f;

    private final OBB obb;
    public VehicleCubeGroup group;
    private final List<CubePoint> cubePoints;
    private final List<CubePoint> attachedPoints = new ArrayList<>();
    private boolean pointsInitialized;
    private final Vector3f localCenter = new Vector3f();
    private final Quaternionf localRotation = new Quaternionf();
    public HashMap<CubeFace, List<CubePoint>> cubePointsByFace = new HashMap<>();
    public CubePoint bottomPoint;
    private Vec3 offset = Vec3.ZERO;
    public double x;
    public double y;
    public double z;
    public double height;
    public double width;
    public double depth;
    public double spaceX;
    public double spaceY;
    public double spaceZ;
    public Vec3 position;
    public Vec3 positionO;
    public Quaternionf rotation;
    public Quaternionf rotationO;

    public VehicleCubeOBB(OBB obb) {
        this.obb = obb;
        this.cubePoints = new ArrayList<>();
        this.initSpacing();
        this.offset = Vec3.ZERO;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.height = obb.extents().y * 2;
        this.width = obb.extents().x * 2;
        this.depth = obb.extents().z * 2;
    }

    public VehicleCubeOBB(OBB obb, VehicleCubeGroup group, BedrockCube cube) {
        this.obb = obb;
        this.group = group;
        this.cubePoints = new ArrayList<>();
        this.offset = group
                .globalTransform(new Vec3(cube.x() + cube.width() / 2, cube.y() + cube.height() / 2, cube.z() + cube.depth() / 2), false)
                .offset();
        this.x = cube.x();
        this.y = cube.y();
        this.z = cube.z();
        this.height = cube.height();
        this.width = cube.width();
        this.depth = cube.depth();
        group.addCubeOBB(this);
        this.initSpacing();
    }

    public VehicleCubeOBB(VehicleCubeOBB origin) {
        this.obb = origin.obb.copy();
        this.group = origin.group;
        this.cubePoints = new ArrayList<>();
        this.initSpacing();
        this.offset = origin.offset;
        this.x = origin.x;
        this.y = origin.y;
        this.z = origin.z;
        this.height = origin.height;
        this.width = origin.width;
        this.depth = origin.depth;
    }

    public static VehicleCubeOBB init(VehicleCubeGroup group, BedrockCube cube) {
        OBB obb = new OBB(Vec3.ZERO.toVector3f(),
                new Vector3f(cube.width() / 2, cube.height() / 2, cube.depth() / 2),
                new Quaternionf(group.rotation));
        return new VehicleCubeOBB(obb, group, cube);
    }

    public void rebuild() {
        offset = group
                .globalTransform(new Vec3(x + width / 2, y + height / 2, z + depth / 2), false)
                .offset();
        obb.setExtents(new Vector3f((float) (width / 2), (float) (height / 2), (float) (depth / 2)));
        cubePoints.clear();
        cubePointsByFace.clear();
        attachedPoints.clear();
        pointsInitialized = false;
        initSpacing();
    }

    /**
     * Standalone refresh: walks the group's parent chain itself. Use the overload below
     * when updating many cubes at once, so the chain walk and vehicle rotation are shared.
     */
    public void update(AbstractVehicle vehicle) {
        VehicleCubeGroup.GlobalTransform globalTransform = group.globalTransform();
        update(vehicle, vehicle.rotYXZ(), globalTransform.offset().toVector3f(), globalTransform.rotation());
    }

    /**
     * Refreshes this cube's local and world pose.
     * <p>
     * {@code groupOffset} and {@code groupRotation} are the owning group's transform relative
     * to the vehicle pivot, and {@code vehicleRotation} is {@link AbstractVehicle#rotYXZ()};
     * the caller computes each of them once for the whole model rather than per cube. Both are
     * only read, so the caller may reuse its own buffers across cubes.
     */
    public void update(AbstractVehicle vehicle, Quaternionf vehicleRotation, Vector3f groupOffset, Quaternionf groupRotation) {
        positionO = position;
        rotationO = rotation;

        localCenter.set((float) (x + width / 2), (float) (y + height / 2), (float) (z + depth / 2));
        groupRotation.transform(localCenter);
        localCenter.add(groupOffset);
        localCenter.sub((float) vehicle.centerOffset.x, (float) vehicle.centerOffset.y, (float) vehicle.centerOffset.z);
        localRotation.set(groupRotation);


        Vector3f center = obb.center();
        center.set(localCenter);
        vehicleRotation.transform(center);
        position = new Vec3(
                vehicle.getX() + vehicle.centerOffset.x + center.x,
                vehicle.getY() + vehicle.centerOffset.y + center.y,
                vehicle.getZ() + vehicle.centerOffset.z + center.z);
        center.set((float) position.x, (float) position.y, (float) position.z);

        rotation = vehicleRotation.mul(groupRotation, new Quaternionf());
        obb.setRotation(rotation);
    }

    /**
     * Centre of this cube in vehicle-local space. Read-only; owned by {@link #update}.
     */
    public Vector3f localCenter() {
        return localCenter;
    }

    /**
     * Orientation of this cube in vehicle-local space. Read-only; owned by {@link #update}.
     */
    public Quaternionf localRotation() {
        return localRotation;
    }

    public static VehicleCubeOBB defaultCube() {
        return new VehicleCubeOBB(new OBB(Vec3.ZERO.toVector3f(), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf()));
    }

    /**
     * Initialises sample spacing and the per-face table.
     */
    private void initSpacing() {
        float gap = 0.1f;
        float offset = POINT_OFFSET;
        float x1 = -obb.extents().x - gap;
        float x2 = obb.extents().x + gap;
        float y1 = -obb.extents().y - offset;
        float y2 = obb.extents().y + gap;
        float z1 = -obb.extents().z - gap;
        float z2 = obb.extents().z + gap;
        spaceX = spacing(x2 - x1);
        spaceY = spacing(y2 - y1);
        spaceZ = spacing(z2 - z1);
        for (CubeFace face : CubeFace.values()) {
            cubePointsByFace.put(face, new ArrayList<>());
        }
    }

    /**
     * Divides span into equal segments, at most MAX_SAMPLES_PER_AXIS of them.
     * At a fixed 1-block spacing the sample count grows with surface area
     * an 8x3x10 tank needs ~460 points, but scales poory beyond that
     * When span is at most MAX_SAMPLES_PER_AXIS this reduces to span / ceil(span), identical
     * to the original, so existing small and mid-sized vehicles keep exactly
     * the same sample point layout.
     * For the time being should allow for much cheaper larger vehices.
     * TODO: logarithmically scaled sample based on surface?
     */
    private static double spacing(double span) {
        double step = Math.max(1.0, span / MAX_SAMPLES_PER_AXIS);
        return span / Math.max(1, Math.ceil(span / step));
    }


    /**
     * Hull-local Y below which a contact on a side face is something to ride over rather than
     * something to be stopped by — the vehicle's suspension, in effect. Above it, a side contact
     * cancels the velocity pressing into it.
     * <p>
     * <b>Why two rows and not one.</b> {@code PhysicsEngine} used to test this inline as
     * {@code -extents.y + spaceY}, but that was never the value it enforced. The sample grid puts
     * its lowest row at {@code -extents.y - POINT_OFFSET} and steps by {@code spaceY}, so rows 0
     * and 1 both fall below that threshold — row 1 by exactly {@code POINT_OFFSET} — and row 2 is
     * the first that can block. No sample existed in between, so the difference never showed.
     * <p>
     * Contacts built from real geometry land wherever the geometry is, and they promptly found
     * that empty band: a one-block step touches the hull about a block up, which is above the
     * written threshold and below the real one, so vehicles stopped dead against steps they had
     * always driven over. This is the band they actually had.
     * <p>
     * It scales with the hull because the grid did, which reads as bigger vehicles riding over
     * bigger bumps. Note it exceeds {@link AbstractVehicle#maxUpStep()}: an obstacle between the
     * two is neither blocked nor climbed in one go, and gets ploughed into. That gap predates the
     * contact work.
     */
    public double climbSkirt() {
        return -obb.extents().y - POINT_OFFSET + 2 * spaceY;
    }

    private void ensurePoints() {
        if (pointsInitialized) {
            return;
        }
        pointsInitialized = true;
        initCubePoints();
    }

    public void initCubePoints() {
        float gap = 0.1f;
        float slack = 0.1f;
        float offset = POINT_OFFSET;
        float x1 = -obb.extents().x - gap;
        float x2 = obb.extents().x + gap;
        float y1 = -obb.extents().y - offset;
        float y2 = obb.extents().y + gap;
        float z1 = -obb.extents().z - gap;
        float z2 = obb.extents().z + gap;
        CubePoint cubePoint;
        // 前后
        for (float x = x1; x <= x2 + slack; x += spaceX) {
            for (float y = y1; y <= y2 + slack; y += spaceY) {
                cubePoint = new CubePoint(this, new Vector3f(x, y, obb.extents().z + offset), CubeFace.FRONT);
                cubePointsByFace.get(CubeFace.FRONT).add(cubePoint);
                cubePoints.add(cubePoint);
                cubePoint = new CubePoint(this, new Vector3f(x, y, -obb.extents().z - offset), CubeFace.BACK);
                cubePointsByFace.get(CubeFace.BACK).add(cubePoint);
                cubePoints.add(cubePoint);
            }
        }
        // 左右
        for (float y = y1; y <= y2 + slack; y += spaceY) {
            for (float z = z1; z <= z2 + slack; z += spaceZ) {
                cubePoint = new CubePoint(this, new Vector3f(obb.extents().x + offset, y, z), CubeFace.LEFT);
                cubePointsByFace.get(CubeFace.LEFT).add(cubePoint);
                cubePoints.add(cubePoint);
                cubePoint = new CubePoint(this, new Vector3f(-obb.extents().x - offset, y, z), CubeFace.RIGHT);
                cubePointsByFace.get(CubeFace.RIGHT).add(cubePoint);
                cubePoints.add(cubePoint);
            }
        }
        // 上下
        for (float x = x1; x <= x2 + slack; x += spaceX) {
            for (float z = z1; z <= z2 + slack; z += spaceZ) {
                cubePoint = new CubePoint(this, new Vector3f(x, obb.extents().y + offset, z), CubeFace.TOP);
                cubePointsByFace.get(CubeFace.TOP).add(cubePoint);
                cubePoints.add(cubePoint);
                cubePoint = new CubePoint(this, new Vector3f(x, -obb.extents().y - offset, z), CubeFace.BOTTOM);
                cubePointsByFace.get(CubeFace.BOTTOM).add(cubePoint);
                cubePoints.add(cubePoint);
            }
        }
        initBottomPoint();
    }

    public void initBottomPoint() {
        ensurePoints();
        List<CubePoint> bottomPoints = cubePointsByFace.get(CubeFace.BOTTOM);
        if (bottomPoints == null || bottomPoints.isEmpty()) {
            return;
        }
        bottomPoints.sort(Comparator.comparingDouble(p -> p.obbLocalPos().y));
        bottomPoint = bottomPoints.get(0);
    }

    public Vec3 offset() {
        return offset;
    }

    public Vec3 center(AbstractVehicle vehicle) {
        return vehicle.position().add(offset());
    }

    public OBB obb() {
        return obb;
    }

    public Quaternionf selfRot() {
        return group.rotation;
    }

    public List<CubePoint> cubePoints() {
        ensurePoints();
        return cubePoints;
    }

    /**
     * Adds a sample point a part owns, such as a landing gear leg reaching below the hull.
     */
    public void attachPoint(CubePoint point) {
        ensurePoints();
        cubePoints.add(point);
        cubePointsByFace.get(point.cubeFace()).add(point);
        attachedPoints.add(point);
    }

    /**
     * Sample points a part attached, as opposed to the ones {@link #initCubePoints} lays over the
     * cube's own surface.
     * <p>
     * The distinction only matters to the inverted collision query, which asks which world boxes
     * overlap the OBB rather than what is under each sample point. That answers for the surface
     * and nothing else — a landing gear leg reaches a couple of blocks <em>below</em> the OBB, so
     * no box pass can ever produce it. These are probed as points under either query.
     */
    public List<CubePoint> attachedPoints() {
        return attachedPoints;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public double getDepth() {
        return depth;
    }

    public double volume() {
        return width * height * depth;
    }

    public static class CubePoint {

        private final VehicleCubeOBB vehicleCubeOBB;
        private final Vector3f obbLocalPos;
        private Vector3f worldPos;
        private final CubeFace cubeFace;
        public CubePointContext cubePointContext;

        public CubePoint(VehicleCubeOBB vehicleCubeOBB, Vector3f obbLocalPos, CubeFace cubeFace) {
            this.vehicleCubeOBB = vehicleCubeOBB;
            this.obbLocalPos = obbLocalPos;
            this.cubeFace = cubeFace;
            this.cubePointContext = new CubePointContext();
        }

        public Vector3f obbLocalPos() {
            return obbLocalPos;
        }

        public Vector3f worldPos(Vector3f[] axes) {
            if (worldPos == null) {
                worldPos = new Vector3f();
            }
            return vehicleCubeOBB.obb.localToWorld(
                    obbLocalPos, axes == null ? vehicleCubeOBB.obb.getAxes() : axes, worldPos);
        }

        public Vector3f cachedWorldPos() {
            return worldPos;
        }

        public CubeFace cubeFace() {
            return cubeFace;
        }

    }

    public static class CubePointContext {

        private Vec3 blockPos;
        private BlockState blockState;
        private double surfaceY = Double.NaN;

        public Vec3 blockPos() {
            return blockPos;
        }

        public void setBlockPos(Vec3 blockPos) {
            this.blockPos = blockPos;
        }

        public BlockState blockState() {
            return blockState;
        }

        public void setBlockState(BlockState blockState) {
            this.blockState = blockState;
        }

        /**
         * World height of the top of the geometry this point is touching, or {@code NaN} when the
         * source could not say.
         * <p>
         * This is the number "am I standing on a slab or a block?" actually wants. It used to be
         * guessed downstream from the block state — {@code HALF} property present, so assume half
         * a block — which is right for a bottom slab, wrong for a top slab, and wrong for stairs.
         * A collision snapshot knows the real answer, so it reports it. Providers do not have
         * geometry to report, so they leave it {@code NaN} and the old estimate still applies.
         */
        public double surfaceY() {
            return surfaceY;
        }

        public void setSurfaceY(double surfaceY) {
            this.surfaceY = surfaceY;
        }

    }

    public enum CubeFace {
        FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
    }

}
