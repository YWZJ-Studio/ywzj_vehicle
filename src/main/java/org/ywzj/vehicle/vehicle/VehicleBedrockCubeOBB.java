package org.ywzj.vehicle.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用基岩模型描述的载具结构块
 */
public class VehicleBedrockCubeOBB {

    private final AbstractVehicle vehicle;
    private final OBB obb;
    private final BedrockBone bone;
    private final BedrockCubePerFace cube;
    private final Quaternionf selfRot;
    private final List<CubePoint> cubePoints;

    public VehicleBedrockCubeOBB(AbstractVehicle vehicle, OBB obb, BedrockBone bone, BedrockCubePerFace cube) {
        this.vehicle = vehicle;
        this.obb = obb;
        this.bone = bone;
        this.cube = cube;
        this.selfRot = new Quaternionf(bone.rotation);
        this.cubePoints = new ArrayList<>();
        this.initCubePoints();
    }

    public static VehicleBedrockCubeOBB init(AbstractVehicle vehicle, BedrockBone bone, BedrockCubePerFace cube) {
        OBB obb = new OBB(Vec3.ZERO.toVector3f(),
                new Vector3f(cube.getWidth() / 2, cube.getHeight() / 2, cube.getDepth() / 2),
                new Quaternionf(bone.rotation));
        return new VehicleBedrockCubeOBB(vehicle, obb, bone, cube);
    }

    public void initCubePoints() {
        float spacing = 1f;
        float gap = 0.1f;
        float offset = 0.001f;
        float halfX = Math.round(obb.extents().x - gap);
        float halfY = Math.round(obb.extents().y - gap);
        float halfZ = Math.round(obb.extents().z - gap);
        // 前后
        for (float x = -halfX; x <= halfX; x += spacing) {
            for (float y = -halfY; y <= halfY; y += spacing) {
                cubePoints.add(new CubePoint(this, new Vector3f(x, y, obb.extents().z + offset), CubeFace.FRONT));
                cubePoints.add(new CubePoint(this, new Vector3f(x, y, -obb.extents().z - offset), CubeFace.BACK));
            }
        }
        // 左右
        for (float y = -halfY; y <= halfY; y += spacing) {
            for (float z = -halfZ; z <= halfZ; z += spacing) {
                cubePoints.add(new CubePoint(this, new Vector3f(obb.extents().x + offset, y, z), CubeFace.LEFT));
                cubePoints.add(new CubePoint(this, new Vector3f(-obb.extents().x - offset, y, z), CubeFace.RIGHT));
            }
        }
        // 上下
        for (float x = -halfX; x <= halfX; x += spacing) {
            for (float z = -halfZ; z <= halfZ; z += spacing) {
                cubePoints.add(new CubePoint(this, new Vector3f(x, obb.extents().y + offset, z), CubeFace.TOP));
                cubePoints.add(new CubePoint(this, new Vector3f(x, -obb.extents().y - offset, z), CubeFace.BOTTOM));
            }
        }
    }

    public Vec3 offset() {
        return new Vec3(bone.x / 16, bone.y / 16, bone.z / 16)
                .add(cube.getX() + cube.getWidth() / 2, cube.getY() + cube.getHeight() / 2, cube.getZ() + cube.getDepth() / 2);
    }

    public Vec3 center() {
        return vehicle.position().add(offset());
    }

    public AbstractVehicle vehicle() {
        return vehicle;
    }

    public BedrockCubePerFace cube() {
        return cube;
    }

    public OBB obb() {
        return obb;
    }

    public BedrockBone bone() {
        return bone;
    }

    public Quaternionf selfRot() {
        return selfRot;
    }

    public List<CubePoint> cubePoints() {
        return cubePoints;
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
