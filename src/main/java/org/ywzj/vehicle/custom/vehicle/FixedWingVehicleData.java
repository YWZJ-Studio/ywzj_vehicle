package org.ywzj.vehicle.custom.vehicle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;
import org.ywzj.vehicle.vehicle.part.*;
import org.ywzj.vehicle.vehicle.pojo.AfterburnerOffset;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FixedWingVehicleData extends BaseVehicleData<FixedWingVehicle> {

    public VehicleCubeOBB aerodynamicCubeOBB;
    public float thrust = 0.02f;
    public float thrustK = 1.5f;
    public float ceiling = 512;
    public float xRotInputStep = 0.2f;
    public float yRotInputStep = 0.5f;
    public float zRotInputStep = 0.2f;
    public float airDragKMin = 1f / 500;
    public float airDragKMax = 4f / 500;
    public float liftToDragK = 6;
    public float angleOfAttackMin = -10f;
    public float angleOfAttackMax = 25f;
    public float xRotInputDragK = 1f;
    public float yRotInputDragK = 1f / 4;
    public float zRotInputDragK = 1f / 8;
    public float turnRateBySpeed = 1f / 2.5f;
    public float xTurnRate = 2;
    public float yTurnRate = 3;
    public float zTurnRate = 8;
    public List<Vec3> vortexOffsets;
    public List<Vec3> aerobaticSmokeOffsets;
    public List<AfterburnerOffset> afterburnerOffsets;
    public String landingGearPartId;
    public String airbrakePartId;
    public String thrustPartId;

    @Override
    public AbstractVehicle fromCustom(Level level) {
        return new FixedWingVehicle(AllEntities.FIXED_WING_VEHICLE.get(), level);
    }

    @Override
    public PartUnitsAndSeats createPartUnits(AbstractVehicle vehicle) {
        if (vehicle instanceof FixedWingVehicle fixedWingVehicle) {
            PartUnitsAndSeats result = super.createPartUnits(vehicle);
            PartUnit<?> landingGearUnit = result.partUnitMap().get(this.landingGearPartId);
            if (landingGearUnit instanceof LandingGearUnit switchableUnit) {
                fixedWingVehicle.landingGear = switchableUnit;
            }
            PartUnit<?> airbrakePartUnit = result.partUnitMap().get(this.airbrakePartId);
            if (airbrakePartUnit instanceof AirbrakeUnit airbrakeUnit) {
                fixedWingVehicle.airbrakeUnit = airbrakeUnit;
            }
            PartUnit<?> thrustPartUnit = result.partUnitMap().get(this.thrustPartId);
            if (thrustPartUnit instanceof ThrustUnit thrustUnit) {
                fixedWingVehicle.thrustUnit = thrustUnit;
            }
            if (afterburnerOffsets == null || afterburnerOffsets.isEmpty()) {
                return result;
            }
            fixedWingVehicle.afterburnerUnits = new ArrayList<>();
            Map<String, PartUnit<?>> partUnitMap = new LinkedHashMap<>(result.partUnitMap());
            int index = partUnitMap.size();
            for (AfterburnerOffset offset : afterburnerOffsets) {
                AfterburnerUnit afterburnerUnit = new AfterburnerUnit(index, vehicle, offset.offset(), offset.scale());
                partUnitMap.put(afterburnerUnit.getId(), afterburnerUnit);
                fixedWingVehicle.afterburnerUnits.add(afterburnerUnit);
                index++;
            }
            return new PartUnitsAndSeats(partUnitMap, result.seats());
        }
        return null;
    }

    public void build(FixedWingVehicleDataPojo pojo) {
        super.build(pojo);
        var model = CommonAssetsManager.structureModelManager()
                .getStructureModel(pojo.structureModel).orElseThrow();
        BedrockBone bone = model.getBoneMap().get("aerodynamic_structure");
        if (bone != null) {
            this.aerodynamicCubeOBB = largestVehicleCubeOBB(bone);
        } else {
            this.aerodynamicCubeOBB = this.mainCubeOBB;
        }
        this.thrust = pojo.attributes.thrust;
        this.thrustK = pojo.attributes.thrustK;
        this.ceiling = pojo.attributes.ceiling;
        this.xRotInputStep = pojo.attributes.xRotInputStep;
        this.yRotInputStep = pojo.attributes.yRotInputStep;
        this.zRotInputStep = pojo.attributes.zRotInputStep;
        this.airDragKMin = pojo.attributes.airDragKMin;
        this.airDragKMax = pojo.attributes.airDragKMax;
        this.liftToDragK = pojo.attributes.liftToDragK;
        this.angleOfAttackMin = pojo.attributes.angleOfAttackMin;
        this.angleOfAttackMax = pojo.attributes.angleOfAttackMax;
        this.xRotInputDragK = pojo.attributes.xRotInputDragK;
        this.yRotInputDragK = pojo.attributes.yRotInputDragK;
        this.zRotInputDragK = pojo.attributes.zRotInputDragK;
        this.turnRateBySpeed = pojo.attributes.turnRateBySpeed;
        this.xTurnRate = pojo.attributes.xTurnRate;
        this.yTurnRate = pojo.attributes.yTurnRate;
        this.zTurnRate = pojo.attributes.zTurnRate;
        this.vortexOffsets = pojo.attributes.vortexOffsets;
        this.afterburnerOffsets = pojo.attributes.afterburnerOffsets;
        this.aerobaticSmokeOffsets = pojo.attributes.aerobaticSmokeOffsets.isEmpty() ? pojo.attributes.vortexOffsets : pojo.attributes.aerobaticSmokeOffsets;
        this.landingGearPartId = pojo.landingGearPartId;
        this.airbrakePartId = pojo.airbrakePartId;
        this.thrustPartId = pojo.thrustPartId;
    }

    public void inject(FixedWingVehicle vehicle) {
        vehicle.aerodynamicCubeOBB = new VehicleCubeOBB(this.aerodynamicCubeOBB);
        vehicle.thrust = this.thrust;
        vehicle.thrustK = this.thrustK;
        vehicle.ceiling = this.ceiling;
        vehicle.xRotInputStep = this.xRotInputStep;
        vehicle.yRotInputStep = this.yRotInputStep;
        vehicle.zRotInputStep = this.zRotInputStep;
        vehicle.airDragKMin = this.airDragKMin;
        vehicle.airDragKMax = this.airDragKMax;
        vehicle.liftToDragK = this.liftToDragK;
        vehicle.angleOfAttackMin = this.angleOfAttackMin;
        vehicle.angleOfAttackMax = this.angleOfAttackMax;
        vehicle.xRotInputDragK = this.xRotInputDragK;
        vehicle.yRotInputDragK = this.yRotInputDragK;
        vehicle.zRotInputDragK = this.zRotInputDragK;
        vehicle.turnRateBySpeed = this.turnRateBySpeed;
        vehicle.xTurnRate = this.xTurnRate;
        vehicle.yTurnRate = this.yTurnRate;
        vehicle.zTurnRate = this.zTurnRate;
        vehicle.vortexOffsets = this.vortexOffsets;
        vehicle.aerobaticSmokeOffsets = this.aerobaticSmokeOffsets;
    }

}
