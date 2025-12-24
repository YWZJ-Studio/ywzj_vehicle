package org.ywzj.vehicle.vehicle.structure;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCube;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 使用基岩模型描述的载具结构块
 */
public class VehicleBedrockCubeOBB {

    private final OBB obb;
    private final Quaternionf selfRot;
    private final List<CubePoint> cubePoints;
    public HashMap<CubeFace, List<CubePoint>> cubePointsByFace = new HashMap<>();
    private Vec3 offset = Vec3.ZERO;
    public final double boneX;
    public final double boneY;
    public final double boneZ;
    public final double height;
    public final double width;
    public final double depth;
    public double spaceX;
    public double spaceY;
    public double spaceZ;

    public VehicleBedrockCubeOBB(OBB obb) {
        this.obb = obb;
        this.selfRot = new Quaternionf();
        this.cubePoints = new ArrayList<>();
        this.initCubePoints();
        this.offset = Vec3.ZERO;
        this.boneX = 0;
        this.boneY = 0;
        this.boneZ = 0;
        this.height = obb.extents().y * 2;
        this.width = obb.extents().x * 2;
        this.depth = obb.extents().z * 2;
    }

    public VehicleBedrockCubeOBB(OBB obb, BedrockBone bone, BedrockCube cube) {
        this.obb = obb;
        this.selfRot = new Quaternionf(bone.rotation);
        this.cubePoints = new ArrayList<>();
        this.initCubePoints();
        this.offset = new Vec3(bone.x / 16, bone.y / 16, bone.z / 16)
                .add(cube.x() + cube.width() / 2, cube.y() + cube.height() / 2, cube.z() + cube.depth() / 2);
        BedrockBone parent = bone.parent;
        while (parent != null) {
            this.offset = this.offset.add(parent.x / 16, parent.y / 16, parent.z / 16);
            parent = parent.parent;
        }
        this.boneX = bone.x;
        this.boneY = bone.y;
        this.boneZ = bone.z;
        this.height = cube.height();
        this.width = cube.width();
        this.depth = cube.depth();
    }

    public VehicleBedrockCubeOBB(VehicleBedrockCubeOBB origin) {
        this.obb = origin.obb.copy();
        this.selfRot = new Quaternionf(origin.selfRot);
        this.cubePoints = new ArrayList<>();
        this.initCubePoints();
        this.offset = origin.offset;
        this.boneX = origin.boneX;
        this.boneY = origin.boneY;
        this.boneZ = origin.boneZ;
        this.height = origin.height;
        this.width = origin.width;
        this.depth = origin.depth;
    }

    public static VehicleBedrockCubeOBB init(BedrockBone bone, BedrockCube cube) {
        OBB obb = new OBB(Vec3.ZERO.toVector3f(),
                new Vector3f(cube.width() / 2, cube.height() / 2, cube.depth() / 2),
                new Quaternionf(bone.rotation));
        return new VehicleBedrockCubeOBB(obb, bone, cube);
    }

    public static VehicleBedrockCubeOBB defaultCube() {
        return new VehicleBedrockCubeOBB(new OBB(Vec3.ZERO.toVector3f(), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf()));
    }

    public void initCubePoints() {
        float gap = 0.1f;
        float slack = 0.1f;
        float offset = 0.001f;
        float x1 = -obb.extents().x - gap;
        float x2 = obb.extents().x + gap;
        spaceX = (x2 - x1) / Math.ceil(x2 - x1);
        float y1 = -obb.extents().y - gap;
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
        return selfRot;
    }

    public List<CubePoint> cubePoints() {
        return cubePoints;
    }

    public double getBoneX() {
        return boneX;
    }

    public double getBoneY() {
        return boneY;
    }

    public double getBoneZ() {
        return boneZ;
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

    public static class CubePoint {

        private final VehicleBedrockCubeOBB vehicleBedrockCubeOBB;
        private final Vector3f obbLocalPos;
        private Vector3f worldPos;
        private final CubeFace cubeFace;
        public CubePointContext cubePointContext;

        public CubePoint(VehicleBedrockCubeOBB vehicleBedrockCubeOBB, Vector3f obbLocalPos, CubeFace cubeFace) {
            this.vehicleBedrockCubeOBB = vehicleBedrockCubeOBB;
            this.obbLocalPos = obbLocalPos;
            this.cubeFace = cubeFace;
            this.cubePointContext = new CubePointContext();
        }

        public Vector3f obbLocalPos() {
            return obbLocalPos;
        }

        public Vector3f worldPos(Vector3f[] axes) {
            worldPos = vehicleBedrockCubeOBB.obb.localToWorld(obbLocalPos, axes == null ? vehicleBedrockCubeOBB.obb.getAxes() : axes);
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

        private BlockPos blockPos;
        private BlockState blockState;

        public BlockPos blockPos() {
            return blockPos;
        }

        public void setBlockPos(BlockPos blockPos) {
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
