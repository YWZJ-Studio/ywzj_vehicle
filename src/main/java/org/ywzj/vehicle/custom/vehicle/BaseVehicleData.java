package org.ywzj.vehicle.custom.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockCubePerFace;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.part.PartUnitEntry;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.pojo.EnergyInfo;
import org.ywzj.vehicle.vehicle.pojo.PhysicsInfo;
import org.ywzj.vehicle.vehicle.pojo.ViewInfo;
import org.ywzj.vehicle.vehicle.structure.VehicleBedrockCubeOBB;

import java.util.*;

public class BaseVehicleData<T extends AbstractVehicle> {

    protected float maxHealth;
    protected ViewInfo viewInfo;
    protected EnergyInfo energyInfo;
    protected PhysicsInfo physicsInfo;
    protected boolean withWarningReceiver;
    protected boolean protectPassenger;
    protected ResourceLocation structureModel;
    protected List<PartUnitEntry<?, ?>> parts;
    protected VehicleBedrockCubeOBB mainCubeOBB;
    protected final List<VehicleBedrockCubeOBB> vehicleBodyOBBs = new ArrayList<>();

    public BaseVehicleData() {}

    protected static String check(BaseVehicleDataPojo pojo) {
        return "";
    }

    public void build(BaseVehicleDataPojo pojo) {
        String checkResult = check(pojo);
        if (!StringUtils.isBlank(checkResult)) {
            YwzjVehicle.LOGGER.warn(checkResult);
            return;
        }

        this.maxHealth = pojo.maxHealth;
        this.viewInfo = pojo.viewInfo;
        this.energyInfo = pojo.energyInfo;
        this.physicsInfo = pojo.physicsInfo;
        this.withWarningReceiver = pojo.withWarningReceiver;
        this.protectPassenger = pojo.protectPassenger;

        this.structureModel = pojo.structureModel;
        var model = CommonAssetsManager.structureModelManager()
                .getStructureModel(pojo.structureModel).orElseThrow();

        this.parts = pojo.parts;
        for (var entry : this.parts) {
            var partData = entry.data();
            partData.initStructureModel(model);
        }
        this.initOBBs(model);
    }

    public void inject(T vehicle) {}

    public record PartUnitsAndSeats(
            Map<String, PartUnit<?>> partUnitMap,
            List<AbstractVehicle.Seat> seats
    ) {}

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

    public record VehicleStructObbs(List<VehicleBedrockCubeOBB> obbs, VehicleBedrockCubeOBB mainCubeOBB) {}

    /**
     * 基岩模型构造车体OBB
     * @param model
     */
    private void initOBBs(BedrockModel model) {
        buildVehicleBodyOBBs(model.getBoneMap().get("vehicle_body"));
        // 约定取体积最大的块表达车体的物理
        vehicleBodyOBBs.sort(Comparator.comparingDouble(vehicleBodyOBB -> -vehicleBodyOBB.depth * vehicleBodyOBB.width * vehicleBodyOBB.height));
        mainCubeOBB = vehicleBodyOBBs.get(0);
    }

    private void buildVehicleBodyOBBs(BedrockBone bone) {
        bone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).forEach(cube -> vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(bone, cube)));
        for (BedrockBone child : bone.getChildren()) {
            buildVehicleBodyOBBs(child);
        }
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public ViewInfo getViewInfo() {
        return viewInfo;
    }

    public EnergyInfo getEnergyInfo() {
        return energyInfo;
    }

    public PhysicsInfo getPhysicsInfo() {
        return physicsInfo;
    }

    public boolean isWithWarningReceiver() {
        return withWarningReceiver;
    }

    public boolean isProtectPassenger() {
        return protectPassenger;
    }

}
