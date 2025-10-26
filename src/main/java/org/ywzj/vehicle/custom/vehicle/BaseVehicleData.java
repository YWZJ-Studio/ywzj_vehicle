package org.ywzj.vehicle.custom.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.ywzj.vehicle.bedrock.model.BedrockModelLoader;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.*;

public class BaseVehicleData {
    private ResourceLocation structureModel;
    private List<WeaponUnitData> weaponUnitData;
    private VehicleBedrockCubeOBB mainCubeOBB;
    private List<VehicleBedrockCubeOBB> vehicleBodyOBBs = new ArrayList<>();
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

        data.weaponUnitData = pojo.weaponUnitData.stream().map(
                unitPojo -> WeaponUnitData.of(unitPojo, model)
        ).toList();

        data.initOBBs(model);
        return data;
    }

    public Map<String, WeaponUnit> createPartUnits(AbstractVehicle vehicle) {
        Map<String, WeaponUnit> partUnitMap = new LinkedHashMap<>();
        int i = 0;
        // 从data创建
        for (var partData : weaponUnitData) {
            var partUnit = new WeaponUnit(partData, i, vehicle);
            partUnitMap.put(partData.getId(), partUnit);
            i++;
        }
        // 依照父级进行链接
        for (var partUnit : weaponUnitData) {
            if (partUnit.getParent() != null) {
                var parent = partUnitMap.get(partUnit.getParent());
                if (parent != null) {
                    partUnitMap.get(partUnit.getId()).setBaseWeaponUnit(parent);
                }
            }
        }
        return partUnitMap;
    }

    public List<WeaponUnitData> getWeaponUnitData() {
        return weaponUnitData;
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
        cubes.sort(Comparator.comparingDouble(cube1 -> cube1.getDepth() * cube1.getWidth() * cube1.getHeight()));
        this.width = cubes.get(0).getWidth();
        this.length = cubes.get(0).getDepth();
        this.height = cubes.get(0).getHeight();
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
        for (var data : weaponUnitData) {
            for (var cubeOBB : data.getUnitBedrockCubeOBBs()) {
                Vec3 offset = cubeOBB.offset();
                this.width = (float) Math.max((Math.abs(offset.x) + cubeOBB.getWidth() / 2) * 2, this.width);
                this.length = (float) Math.max((Math.abs(offset.z) + cubeOBB.getDepth() / 2) * 2, this.length);
                this.height = (float) Math.max(Math.abs(offset.y) + cubeOBB.getHeight() / 2, this.height);
            }
        }
    }
}
