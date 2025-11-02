package org.ywzj.vehicle.custom.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.ywzj.vehicle.custom.part.PartUnitEntry;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.resource.BedrockModelLoader;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.*;

public class BaseVehicleData {
    private ResourceLocation structureModel;
    private List<PartUnitEntry<?, ?>> parts;
    private VehicleBedrockCubeOBB mainCubeOBB;
    private final List<VehicleBedrockCubeOBB> vehicleBodyOBBs = new ArrayList<>();
    private float width = 1.0f;
    private float length = 1.0f;
    private float height = 1.0f;

    protected BaseVehicleData() {
    }

    @Nullable
    public static BaseVehicleData of(BaseVehicleDataPojo pojo) {
        if (pojo.structureModel == null) return null;
        var data = new BaseVehicleData();

        data.structureModel = pojo.structureModel;
        var model = BedrockModelLoader.getModel(pojo.structureModel);

        data.parts = pojo.parts;
        for (var entry : data.parts) {
            var partData = entry.data();
            partData.initStructureModel(model);
        }
        data.initOBBs(model);
        return data;
    }

    public Map<String, PartUnit<?>> createPartUnits(AbstractVehicle vehicle) {
        Map<String, PartUnit<?>> partUnitMap = new LinkedHashMap<>();
        int i = 0;
        // 从data创建
        for (var partData : parts) {
            partUnitMap.put(partData.data().getId(), partData.create(i, vehicle));
            i++;
        }
        // 额外操作
        var view = Collections.unmodifiableMap(partUnitMap);
        for (var partUnit : partUnitMap.values()) {
            partUnit.combineAndInit(view, vehicle);
        }

        return partUnitMap;
    }

    public ResourceLocation getStructureModel() {
        return structureModel;
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public float getLength() {
        return length;
    }

    public VehicleStructObbs getVehicleStructObbs() {
        var obbs = vehicleBodyOBBs.stream().map(VehicleBedrockCubeOBB::new).toList();
        return new VehicleStructObbs(obbs, obbs.get(0));
    }

    public record VehicleStructObbs(List<VehicleBedrockCubeOBB> obbs, VehicleBedrockCubeOBB mainCubeOBB) {
    }

    // 缓存
    private void initOBBs(BedrockModel model) {
        BedrockBone bone = model.getBoneMap().get("vehicle_body");
        // 约定取体积最大的块表达车体的长宽高
        List<BedrockCubePerFace> cubes = new ArrayList<>(bone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
        cubes.sort(Comparator.comparingDouble(cube1 -> cube1.depth() * cube1.width() * cube1.height()));
        this.width = cubes.get(0).width();
        this.length = cubes.get(0).depth();
        this.height = cubes.get(0).height();
        mainCubeOBB = VehicleBedrockCubeOBB.init(bone, cubes.remove(0));
        vehicleBodyOBBs.add(mainCubeOBB);
        for (BedrockCubePerFace cube : cubes) {
            vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(bone, cube));
        }
        for (BedrockBone child : bone.getChildren()) {
            List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
            for (BedrockCubePerFace cube : childCubes) {
                vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(child, cube));
            }
        }
        // 由部件结构拓展车体长宽
        for (var entry : this.parts) {
            var data = entry.data();
            for (var cubeOBB : data.getUnitBedrockCubeOBBs()) {
                Vec3 offset = cubeOBB.offset();
                this.width = (float) Math.max((Math.abs(offset.x) + cubeOBB.getWidth() / 2) * 2, this.width);
                this.length = (float) Math.max((Math.abs(offset.z) + cubeOBB.getDepth() / 2) * 2, this.length);
                this.height = (float) Math.max(Math.abs(offset.y) + cubeOBB.getHeight() / 2, this.height);
            }
        }
    }
}
