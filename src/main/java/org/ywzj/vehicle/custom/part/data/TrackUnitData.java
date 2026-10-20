package org.ywzj.vehicle.custom.part.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeGroup;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TrackUnitData extends PartUnitData {

    private final List<List<Vec3>> tracks;
    private final Map<String, List<String>> suspensions;
    private List<TrackDefinition> trackDefinitions = List.of();

    public record TrackDefinition(String name, List<Vec3> points, List<String> suspensionUnitIds) {}

    public TrackUnitData(TrackUnitPojo pojo) {
        super(pojo);
        this.tracks = copyTracks(pojo.tracks);
        this.suspensions = pojo.suspensions;
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
        partCubeOBBs = List.of();
        BedrockBone structureBone = model == null ? null : model.getBoneMap().get(this.structureBone);
        List<BedrockBone> trackBones = structureBone == null ? List.of() : structureBone.getChildren();
        int trackCount = structureBone == null ? tracks.size() : trackBones.size();
        if (trackCount != suspensions.size()) {
            throw new IllegalArgumentException("Track and suspension counts must match");
        }
        if (structureBone == null) {
            List<String> trackNames = new ArrayList<>(suspensions.keySet());
            List<TrackDefinition> definitions = new ArrayList<>(tracks.size());
            for (int index = 0; index < tracks.size(); index++) {
                String trackName = trackNames.get(index);
                definitions.add(new TrackDefinition(trackName, tracks.get(index), suspensions.get(trackName)));
            }
            this.trackDefinitions = List.copyOf(definitions);
            return;
        }
        Map<BedrockBone, String> trackNames = new IdentityHashMap<>();
        model.getBoneMap().forEach((name, bone) -> trackNames.put(bone, name));
        List<TrackDefinition> definitions = new ArrayList<>(trackBones.size());
        for (BedrockBone trackBone : trackBones) {
            String trackName = trackNames.get(trackBone);
            VehicleCubeGroup trackGroup = vehiclePartGroups.get(trackBone);
            List<Vec3> points = new ArrayList<>();
            if (trackGroup != null) {
                for (var cube : trackBone.cubes) {
                    Vec3 cubePivot = new Vec3(
                            cube.x() + cube.width() / 2,
                            cube.y() + cube.height() / 2,
                            cube.z() + cube.depth() / 2
                    );
                    points.add(trackGroup.globalTransform(cubePivot, true).offset());
                }
            }
            definitions.add(new TrackDefinition(trackName, List.copyOf(points), suspensions.get(trackName)));
        }
        this.trackDefinitions = List.copyOf(definitions);
    }

    public List<TrackDefinition> getTrackDefinitions() {
        return trackDefinitions;
    }

}
