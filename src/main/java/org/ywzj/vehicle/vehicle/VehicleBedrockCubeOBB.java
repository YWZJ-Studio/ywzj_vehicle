package org.ywzj.vehicle.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class VehicleBedrockCubeOBB {

    private final AbstractVehicle vehicle;
    private final OBB obb;
    private final BedrockBone bone;
    private final BedrockCubePerFace cube;
    private final Quaternionf rot;

    public VehicleBedrockCubeOBB(AbstractVehicle vehicle, OBB obb, BedrockBone bone, BedrockCubePerFace cube) {
        this.vehicle = vehicle;
        this.obb = obb;
        this.bone = bone;
        this.cube = cube;
        this.rot = new Quaternionf(bone.rotation);
    }

    public static VehicleBedrockCubeOBB init(AbstractVehicle vehicle, BedrockBone bone, BedrockCubePerFace cube) {
        OBB obb = new OBB(Vec3.ZERO.toVector3f(),
                new Vector3f(cube.getWidth() / 2, cube.getHeight() / 2, cube.getDepth() / 2),
                new Quaternionf(bone.rotation));
        return new VehicleBedrockCubeOBB(vehicle, obb, bone, cube);
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

    public Quaternionf rot() {
        return rot;
    }

}
