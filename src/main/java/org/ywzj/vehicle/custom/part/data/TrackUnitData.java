package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackUnitData extends PartUnitData {

    private List<List<Vec3>> tracks;

    public TrackUnitData(TrackUnitPojo pojo) {
        super(pojo);
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
        if (!tracks.isEmpty() || model == null) {
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
            }
        }
        this.tracks = List.copyOf(parsedTracks);
    }

    public List<List<Vec3>> getTracks() {
        return tracks;
    }

}
