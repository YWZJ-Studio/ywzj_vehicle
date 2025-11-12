package org.ywzj.vehicle.custom.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.PartUnitEntry;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.*;

public class BaseVehicleData {

    private ResourceLocation structureModel;
    private List<PartUnitEntry<?, ?>> parts;
    private VehicleBedrockCubeOBB mainCubeOBB;
    private final List<VehicleBedrockCubeOBB> vehicleBodyOBBs = new ArrayList<>();

    protected BaseVehicleData() {}

    protected static String check(BaseVehicleDataPojo pojo) {
        return "";
    }

    @Nullable
    public static BaseVehicleData of(BaseVehicleDataPojo pojo) {
        String checkResult = check(pojo);
        if (!StringUtils.isBlank(checkResult)) {
            YwzjVehicle.LOGGER.warn(checkResult);
            return null;
        }

        var data = new BaseVehicleData();

        data.structureModel = pojo.structureModel;
        var model = CommonAssetsManager.structureModelManager()
                .getStructureModel(pojo.structureModel).orElseThrow();

        data.parts = pojo.parts;
        for (var entry : data.parts) {
            var partData = entry.data();
            partData.initStructureModel(model);
        }
        data.initOBBs(model);
        return data;
    }

    public record PartUnitsAndSeats(
            Map<String, PartUnit<?>> partUnitMap,
            List<AbstractVehicle.Seat> seats
    ) {
    }

    public PartUnitsAndSeats createPartUnits(AbstractVehicle vehicle) {
        Map<String, PartUnit<?>> partUnitMap = new LinkedHashMap<>();
        List<AbstractVehicle.Seat> seats = new ArrayList<>();
        int i = 0;
        // 从data创建
        for (var partData : parts) {
            var partUnit = partData.create(i, vehicle);
            partUnitMap.put(partData.data().getId(), partUnit);
            if (partData.data().isSeat()) {
                int seatIndex = seats.size();
                seats.add(new AbstractVehicle.Seat(seatIndex, partUnit));
            }
            i++;
        }
        // 额外操作
        var view = Collections.unmodifiableMap(partUnitMap);
        for (var partUnit : partUnitMap.values()) {
            partUnit.combineAndInit(view, vehicle);
        }

        return new PartUnitsAndSeats(partUnitMap, seats);
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
        // 约定取体积最大的块表达车体的物理
        List<BedrockCubePerFace> cubes = new ArrayList<>(bone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
        cubes.sort(Comparator.comparingDouble(cube1 -> cube1.depth() * cube1.width() * cube1.height()));
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
    }

}
