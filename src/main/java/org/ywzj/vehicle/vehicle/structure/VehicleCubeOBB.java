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

    private final OBB obb;
    public VehicleCubeGroup group;
    private final List<CubePoint> cubePoints;
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
        this.initCubePoints();
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
        this.initCubePoints();
    }

    public VehicleCubeOBB(VehicleCubeOBB origin) {
        this.obb = origin.obb.copy();
        this.group = origin.group;
        this.cubePoints = new ArrayList<>();
        this.initCubePoints();
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
        initCubePoints();
    }

    public void update(AbstractVehicle vehicle) {
        positionO = position;
        rotationO = rotation;
        VehicleCubeGroup.GlobalTransform globalTransform = group.globalTransform();
        Quaternionf globalRotation = globalTransform.rotation();
        Vector3f centerOffset = new Vector3f((float) (x + width / 2), (float) (y + height / 2), (float) (z + depth / 2));
        globalRotation.transform(centerOffset);
        Quaternionf vehicleRotation = vehicle.rotYXZ();
        Vector3f center = vehicleRotation.transform(globalTransform.offset()
                .add(centerOffset.x, centerOffset.y, centerOffset.z)
                .subtract(vehicle.centerOffset.x, vehicle.centerOffset.y, vehicle.centerOffset.z)
                .toVector3f());
        position = vehicle.position()
                .add(vehicle.centerOffset)
                .add(center.x, center.y, center.z);
        obb.setCenter(position.toVector3f());
        rotation = vehicleRotation.mul(globalRotation);
        obb.setRotation(rotation);
    }

    public static VehicleCubeOBB defaultCube() {
        return new VehicleCubeOBB(new OBB(Vec3.ZERO.toVector3f(), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf()));
    }

    public void initCubePoints() {
        float gap = 0.1f;
        float slack = 0.1f;
        float offset = 0.001f;
        float x1 = -obb.extents().x - gap;
        float x2 = obb.extents().x + gap;
        spaceX = (x2 - x1) / Math.ceil(x2 - x1);
        float y1 = -obb.extents().y - offset;
        float y2 = obb.extents().y + gap;
        spaceY = (y2 - y1) / Math.ceil(y2 - y1);
        float z1 = -obb.extents().z - gap;
        float z2 = obb.extents().z + gap;
        spaceZ = (z2 - z1) / Math.ceil(z2 - z1);
        cubePointsByFace.put(CubeFace.FRONT, new ArrayList<>());
        cubePointsByFace.put(CubeFace.BACK, new ArrayList<>());
        cubePointsByFace.put(CubeFace.LEFT, new ArrayList<>());
        cubePointsByFace.put(CubeFace.RIGHT, new ArrayList<>());
        cubePointsByFace.put(CubeFace.TOP, new ArrayList<>());
        cubePointsByFace.put(CubeFace.BOTTOM, new ArrayList<>());
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
        List<CubePoint> bottomPoints = cubePointsByFace.get(CubeFace.BOTTOM);
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
        return cubePoints;
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
            worldPos = vehicleCubeOBB.obb.localToWorld(obbLocalPos, axes == null ? vehicleCubeOBB.obb.getAxes() : axes);
            return worldPos;
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

    }

    public enum CubeFace {
        FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
    }

}
