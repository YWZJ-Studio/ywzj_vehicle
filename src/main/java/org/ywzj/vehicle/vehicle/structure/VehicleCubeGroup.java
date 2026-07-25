package org.ywzj.vehicle.vehicle.structure;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class VehicleCubeGroup {

    public VehicleCubeGroup parent;
    public List<VehicleCubeGroup> children = new ArrayList<>();
    // 组旋转的默认值
    public Quaternionf baseRotation;
    // 组旋转的实际值
    public Quaternionf rotation;
    // 相对于父组枢轴
    public Vec3 pivot;
    // 相对于载具枢轴
    public Vec3 pivotOffset;
    public List<VehicleCubeOBB> cubeOBBs = new ArrayList<>();

    public record GlobalTransform(Vec3 offset, Quaternionf rotation) {}

    public VehicleCubeGroup(VehicleCubeGroup parent, Quaternionf rotation, Vec3 pivot) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
        this.baseRotation = rotation;
        this.rotation = rotation;
        this.pivot = pivot;
        this.pivotOffset = globalTransform().offset;
    }

    public void addChild(VehicleCubeGroup child) {
        this.children.add(child);
    }

    public void addCubeOBB(VehicleCubeOBB cubeOBB) {
        cubeOBBs.add(cubeOBB);
    }

    public VehicleCubeGroup.GlobalTransform globalTransform() {
        return globalTransform(Vec3.ZERO, false);
    }

    public VehicleCubeGroup.GlobalTransform globalTransform(Vec3 offset, boolean withSelfRotation) {
        Quaternionf globalRotation = new Quaternionf(rotation);
        Vector3f globalPivot = pivot.toVector3f()
                .add(withSelfRotation ? rotation.transform(offset.toVector3f()) : offset.toVector3f());
        VehicleCubeGroup parentGroup = parent;
        while (parentGroup != null) {
            parentGroup.rotation.transform(globalPivot);
            globalPivot.add((float) parentGroup.pivot.x, (float) parentGroup.pivot.y, (float) parentGroup.pivot.z);
            globalRotation.premul(parentGroup.rotation);
            parentGroup = parentGroup.parent;
        }
        return new VehicleCubeGroup.GlobalTransform(new Vec3(globalPivot), globalRotation);
    }

}
