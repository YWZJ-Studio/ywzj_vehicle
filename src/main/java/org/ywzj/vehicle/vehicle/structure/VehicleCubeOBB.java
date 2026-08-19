package org.ywzj.vehicle.vehicle.structure;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

/**
 * 使用基岩模型描述的载具结构块
 */
public class VehicleCubeOBB {

    private static final int MAX_SAMPLES_PER_AXIS = 24;

    // How far outside its face a sample point sits for contact detection.
    public static final float POINT_OFFSET = 0.001f;
    // Headroom above the step height, in blocks. Must stay above the speculative contact margin,
    // or a contact generated before overlap reads as a wall instead of a step.
    private static final double RIDE_STEP_MARGIN = 0.06;

    private final OBB obb;
    public VehicleCubeGroup group;
    private boolean deck;
    private final List<CubePoint> cubePoints;
    private final List<CubePoint> attachedPoints = new ArrayList<>();
    private boolean pointsInitialized;
    private final Vector3f localCenter = new Vector3f();
    private final Quaternionf localRotation = new Quaternionf();
    public EnumMap<CubeFace, List<CubePoint>> cubePointsByFace = new EnumMap<>(CubeFace.class);
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
        this.deck = origin.deck;
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
        return init(group, cube, false);
    }

    public static VehicleCubeOBB init(VehicleCubeGroup group, BedrockCube cube, boolean deck) {
        OBB obb = new OBB(Vec3.ZERO.toVector3f(),
                new Vector3f(cube.width() / 2, cube.height() / 2, cube.depth() / 2),
                new Quaternionf(group.rotation));
        VehicleCubeOBB cubeOBB = new VehicleCubeOBB(obb, group, cube);
        cubeOBB.deck = deck;
        return cubeOBB;
    }

    // Whether this cube is declared landing surface for other vehicles.
    public boolean isDeck() {
        return deck;
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
     * Refreshes this cube's pose by walking the parent chain.
     */
    public void update(AbstractVehicle vehicle) {
        VehicleCubeGroup.GlobalTransform globalTransform = group.globalTransform();
        update(vehicle, vehicle.rotYXZ(), globalTransform.offset().toVector3f(), globalTransform.rotation());
    }

    /**
     * Refreshes this cube's local and world pose.
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

    public void translate(float fx, float fy, float fz, double dx, double dy, double dz) {
        positionO = position;
        rotationO = rotation;
        if (position != null) {
            position = position.add(dx, dy, dz);
        }
        obb.center().add(fx, fy, fz);
    }

    /**
     * Centre of this cube in vehicle-local space. Read-only
     */
    public Vector3f localCenter() {
        return localCenter;
    }

    /**
     * Orientation of this cube in vehicle-local space. Read-only
     */
    public Quaternionf localRotation() {
        return localRotation;
    }

    public static VehicleCubeOBB defaultCube() {
        return new VehicleCubeOBB(new OBB(Vec3.ZERO.toVector3f(), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf()));
    }

    /**
     * Initializes sample spacing and the per-face table.
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
     * Divides span into at most MAX_SAMPLES_PER_AXIS equal segments.
     */
    private static double spacing(double span) {
        double step = Math.max(1.0, span / MAX_SAMPLES_PER_AXIS);
        return span / Math.max(1, Math.ceil(span / step));
    }



    /**
     * Height above the hull underside, in local coordinates, below which a contact is treated as
     * something to ride over rather than something to stop against. Scales with the hull, so
     * bigger vehicles ride over bigger bumps.
     */
    public double climbSkirt() {
        return -obb.extents().y - POINT_OFFSET + 2 * spaceY;
    }

    /**
     * The climb skirt clamped to what this vehicle can actually step up. Anchored to the hull
     * underside, never to a datum an obstacle can raise.
     */
    public double rideSkirt(double maxUpStep) {
        return Math.min(climbSkirt(), -obb.extents().y + maxUpStep + RIDE_STEP_MARGIN);
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
     * Sample points attached by parts, distinct from those initialized on the cube surface.
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


        public Vector3f worldPos(OBB pose, Vector3f[] axes) {
            if (worldPos == null) {
                worldPos = new Vector3f();
            }
            return pose.localToWorld(obbLocalPos, axes, worldPos);
        }

        public Vector3f cachedWorldPos() {
            return worldPos;
        }

        public CubeFace cubeFace() {
            return cubeFace;
        }

    }

    public static class CubePointContext {

        public static final long NO_CELL = Long.MIN_VALUE;
        private long cellPos = NO_CELL;
        private boolean worldCell;
        private BlockState blockState;
        private double surfaceY = Double.NaN;

        /** The contacted cell packed as a long, or NO_CELL if none. */
        public long cellPos() {
            return cellPos;
        }

        /** True when a cell is set and it names a real block in this level. */
        public boolean hasWorldCell() {
            return worldCell && cellPos != NO_CELL;
        }

        public boolean hasCell() {
            return cellPos != NO_CELL;
        }

        public int cellY() {
            return BlockPos.getY(cellPos);
        }

        /** Records a contact against a real block in this level. */
        public void setWorldCell(int x, int y, int z) {
            this.cellPos = BlockPos.asLong(x, y, z);
            this.worldCell = true;
        }

        /** Records a contact against provider geometry, which must never be written back to. */
        public void setProviderCell(Vec3 blockPos) {
            if (blockPos == null) {
                clearCell();
                return;
            }
            this.cellPos = BlockPos.asLong(
                    Mth.floor(blockPos.x), Mth.floor(blockPos.y), Mth.floor(blockPos.z));
            this.worldCell = false;
        }

        public void clearCell() {
            this.cellPos = NO_CELL;
            this.worldCell = false;
        }


        @Nullable
        public Vec3 blockPos() {
            if (cellPos == NO_CELL) {
                return null;
            }
            return new Vec3(BlockPos.getX(cellPos) + 0.5, BlockPos.getY(cellPos),
                    BlockPos.getZ(cellPos) + 0.5);
        }

        public BlockState blockState() {
            return blockState;
        }

        public void setBlockState(BlockState blockState) {
            this.blockState = blockState;
        }

        /**
         * World height of the top of the geometry this point is touching, or NaN if unknown.
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
