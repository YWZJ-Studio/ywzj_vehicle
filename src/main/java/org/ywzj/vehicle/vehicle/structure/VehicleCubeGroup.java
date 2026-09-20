package org.ywzj.vehicle.vehicle.structure;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class VehicleCubeGroup {

    public VehicleCubeGroup parent;
    public List<VehicleCubeGroup> children = new ArrayList<>();
    // 组旋转的默认值
    public Quaternionf baseRotation;
    // 组旋转的实际值
    public Quaternionf rotation;
    public Quaternionf rotationO;
    // 相对于父组的枢轴
    public Vec3 pivot;
    // 相对于载具枢轴的偏移
    public Vec3 pivotOffset;
    // 相对于父组的枢轴的偏移
    public Vec3 offset = Vec3.ZERO;
    public Vec3 offsetO = Vec3.ZERO;
    public List<VehicleCubeOBB> cubeOBBs = new ArrayList<>();

    public record GlobalTransform(Vec3 offset, Quaternionf rotation) {}

    public VehicleCubeGroup(VehicleCubeGroup parent, Quaternionf rotation, Vec3 pivot) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
        this.baseRotation = rotation;
        this.rotation = rotation;
        this.rotationO = new Quaternionf(rotation);
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
        return globalTransform(offset, withSelfRotation, group -> group.rotation, group -> group.offset);
    }

    public VehicleCubeGroup.GlobalTransform globalTransform(Vec3 offset, boolean withSelfRotation,
                                                            Function<VehicleCubeGroup, Quaternionf> rotationProvider,
                                                            Function<VehicleCubeGroup, Vec3> offsetProvider) {
        Quaternionf selfRotation = rotationProvider.apply(this);
        Quaternionf globalRotation = new Quaternionf(selfRotation);
        Vector3f globalPivot = pivot.add(offsetProvider.apply(this)).toVector3f()
                .add(withSelfRotation ? selfRotation.transform(offset.toVector3f()) : offset.toVector3f());
        VehicleCubeGroup parentGroup = parent;
        while (parentGroup != null) {
            Quaternionf parentRotation = rotationProvider.apply(parentGroup);
            parentRotation.transform(globalPivot);
            Vec3 parentPivot = parentGroup.pivot.add(offsetProvider.apply(parentGroup));
            globalPivot.add((float) parentPivot.x, (float) parentPivot.y, (float) parentPivot.z);
            globalRotation.premul(parentRotation);
            parentGroup = parentGroup.parent;
        }
        return new VehicleCubeGroup.GlobalTransform(new Vec3(globalPivot), globalRotation);
    }

}
