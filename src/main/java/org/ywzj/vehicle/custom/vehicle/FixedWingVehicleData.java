package org.ywzj.vehicle.custom.vehicle;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.FixedWingVehicle;

import java.util.List;

public class FixedWingVehicleData extends BaseVehicleData<FixedWingVehicle> {

    public float thrust = 0.02f;
    public float thrustK = 1.5f;
    public float xRotInputStep = 0.2f;
    public float yRotInputStep = 0.5f;
    public float zRotInputStep = 0.2f;
    public float airDragKMin = 1f / 500;
    public float airDragKMax = 4f / 500;
    public float liftToDragK = 6;
    public float xRotInputDragK = 1f;
    public float yRotInputDragK = 1f / 4;
    public float zRotInputDragK = 1f / 8;
    public float landingGearDragK = 1f / 2;
    public float turnRateBySpeed = 1f / 2.5f;
    public float xTurnRate = 2;
    public float yTurnRate = 3;
    public float zTurnRate = 8;
    public List<Vec3> vortexOffsets;
    public String landingGearPartId;

    @Override
    public AbstractVehicle fromCustom(Level level) {
       return new FixedWingVehicle(AllEntities.FIXED_WING_VEHICLE.get(), level);
    }

    public void build(FixedWingVehicleDataPojo pojo) {
        super.build(pojo);
        this.thrust = pojo.attributes.thrust;
        this.thrustK = pojo.attributes.thrustK;
        this.xRotInputStep = pojo.attributes.xRotInputStep;
        this.yRotInputStep = pojo.attributes.yRotInputStep;
        this.zRotInputStep = pojo.attributes.zRotInputStep;
        this.airDragKMin = pojo.attributes.airDragKMin;
        this.airDragKMax = pojo.attributes.airDragKMax;
        this.liftToDragK = pojo.attributes.liftToDragK;
        this.xRotInputDragK = pojo.attributes.xRotInputDragK;
        this.yRotInputDragK = pojo.attributes.yRotInputDragK;
        this.zRotInputDragK = pojo.attributes.zRotInputDragK;
        this.landingGearDragK = pojo.attributes.landingGearDragK;
        this.turnRateBySpeed = pojo.attributes.turnRateBySpeed;
        this.xTurnRate = pojo.attributes.xTurnRate;
        this.yTurnRate = pojo.attributes.yTurnRate;
        this.zTurnRate = pojo.attributes.zTurnRate;
        this.vortexOffsets = pojo.attributes.vortexOffsets;
        this.landingGearPartId = pojo.landingGearPartId;
    }

    public void inject(FixedWingVehicle vehicle) {
        vehicle.thrust = thrust;
        vehicle.thrustK = thrustK;
        vehicle.xRotInputStep = xRotInputStep;
        vehicle.yRotInputStep = yRotInputStep;
        vehicle.zRotInputStep = zRotInputStep;
        vehicle.airDragKMin = airDragKMin;
        vehicle.airDragKMax = airDragKMax;
        vehicle.liftToDragK = liftToDragK;
        vehicle.xRotInputDragK = xRotInputDragK;
        vehicle.yRotInputDragK = yRotInputDragK;
        vehicle.zRotInputDragK = zRotInputDragK;
        vehicle.landingGearDragK = landingGearDragK;
        vehicle.turnRateBySpeed = turnRateBySpeed;
        vehicle.xTurnRate = xTurnRate;
        vehicle.yTurnRate = yTurnRate;
        vehicle.zTurnRate = zTurnRate;
        vehicle.vortexOffsets = vortexOffsets;
        vehicle.landingGearPartId = landingGearPartId;
    }

}
