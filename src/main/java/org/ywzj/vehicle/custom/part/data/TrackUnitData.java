package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackUnitData extends SuspensionUnitData {

    private static final float COLLISION_THICKNESS = 1.0f / 16;
    private final float trackWidth;
    private List<List<Vec3>> tracks;
    private final List<Vec3> supportOffsets = new ArrayList<>();

    public TrackUnitData(TrackUnitPojo pojo) {
        super(pojo);
        this.trackWidth = pojo.trackWidth;
        this.tracks = copyTracks(pojo.tracks);
    }

    private static List<List<Vec3>> copyTracks(List<List<Vec3>> tracks) {
        if (tracks == null) {
            return List.of();
        }
        List<List<Vec3>> result = new ArrayList<>();
        for (List<Vec3> track : tracks) {
            if (track == null) {
                continue;
            }
            List<Vec3> points = track.stream().filter(point -> point != null).toList();
            if (!points.isEmpty()) {
                result.add(points);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public void initStructureModel(BedrockModel model, Map<BedrockBone, VehicleCubeGroup> vehiclePartGroups) {
        super.initStructureModel(model, vehiclePartGroups);
        partCubeOBBs = new ArrayList<>();
        if (!tracks.isEmpty()) {
            for (List<Vec3> points : tracks) {
                buildCollisionStructure(points, new Vec3(1, 0, 0));
            }
            return;
        }
        BedrockBone structureBone = model.getBoneMap().get(this.structureBone);
        if (structureBone == null) {
            return;
        }
        List<List<Vec3>> parsedTracks = new ArrayList<>();
        for (BedrockBone trackBone : structureBone.getChildren()) {
            VehicleCubeGroup trackGroup = vehiclePartGroups.get(trackBone);
            if (trackGroup == null) {
                continue;
            }
            List<Vec3> points = new ArrayList<>();
            for (var cube : trackBone.cubes) {
                Vec3 cubePivot = new Vec3(
                        cube.x() + cube.width() / 2,
                        cube.y() + cube.height() / 2,
                        cube.z() + cube.depth() / 2
                );
                points.add(trackGroup.globalTransform(cubePivot, true).offset());
            }
            if (!points.isEmpty()) {
                parsedTracks.add(List.copyOf(points));
                Vec3 widthAxis = new Vec3(trackGroup.globalTransform().rotation().transform(new Vector3f(1, 0, 0)));
                buildCollisionStructure(points, widthAxis);
            }
        }
        this.tracks = List.copyOf(parsedTracks);
    }

    private void buildCollisionStructure(List<Vec3> points, Vec3 widthAxis) {
        var transform = structureGroup.globalTransform();
        Quaternionf inverseRotation = new Quaternionf(transform.rotation()).invert();
        Vec3 localWidthAxis = new Vec3(inverseRotation.transform(widthAxis.toVector3f()));
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (Vec3 point : points) {
            Vector3f localPoint = inverseRotation.transform(point.subtract(transform.offset()).toVector3f());
            min.min(localPoint);
            max.max(localPoint);
        }
        // 每条履带只生成一个外包围块，横向范围包含履带宽度。
        Vector3f center = new Vector3f(min).add(max).mul(0.5f);
        Vector3f extents = new Vector3f(max).sub(min).mul(0.5f).add(
                (float) Math.abs(localWidthAxis.x) * trackWidth / 2,
                (float) Math.abs(localWidthAxis.y) * trackWidth / 2 + COLLISION_THICKNESS / 2,
                (float) Math.abs(localWidthAxis.z) * trackWidth / 2);
        extents.max(new Vector3f(COLLISION_THICKNESS / 2));
        VehicleCubeOBB cube = new VehicleCubeOBB(new OBB(new Vector3f(), extents,
                new Quaternionf(structureGroup.rotation)));
        cube.group = structureGroup;
        cube.x = center.x - extents.x;
        cube.y = center.y - extents.y;
        cube.z = center.z - extents.z;
        cube.rebuild();
        partCubeOBBs.add(cube);
        addSupportOffsets(center, extents);
    }

    private void addSupportOffsets(Vector3f localCenter, Vector3f extents) {
        Quaternionf parentRotation = structureGroup.parent == null ? new Quaternionf()
                : structureGroup.parent.globalTransform().rotation();
        Quaternionf inverseParent = new Quaternionf(parentRotation).invert();
        var transform = structureGroup.globalTransform(new Vec3(localCenter), true);
        Quaternionf rotation = new Quaternionf(inverseParent).mul(transform.rotation());
        Vector3f down = new Quaternionf(rotation).invert().transform(new Vector3f(0, -1, 0));
        int normal = Math.abs(down.x) >= Math.abs(down.y) && Math.abs(down.x) >= Math.abs(down.z) ? 0
                : Math.abs(down.y) >= Math.abs(down.z) ? 1 : 2;
        int u = (normal + 1) % 3;
        int v = (normal + 2) % 3;
        // 在最朝下的面上按最多半格间距采样，包含边缘及面中心。
        int stepsU = Math.max(2, (int) Math.ceil(extents.get(u) * 2) * 2);
        int stepsV = Math.max(2, (int) Math.ceil(extents.get(v) * 2) * 2);
        Vec3 center = new Vec3(inverseParent.transform(transform.offset().subtract(pivotOffset).toVector3f()));
        for (int i = 0; i <= stepsU; i++) {
            for (int j = 0; j <= stepsV; j++) {
                Vector3f point = new Vector3f();
                point.setComponent(normal, Math.copySign(extents.get(normal), down.get(normal)));
                point.setComponent(u, extents.get(u) * (2.0f * i / stepsU - 1));
                point.setComponent(v, extents.get(v) * (2.0f * j / stepsV - 1));
                supportOffsets.add(center.add(new Vec3(rotation.transform(point))));
            }
        }
    }

    public List<Vec3> getSupportOffsets() {
        return List.copyOf(supportOffsets);
    }

    public float getTrackWidth() {
        return trackWidth;
    }

    public List<List<Vec3>> getTracks() {
        return tracks;
    }

}
