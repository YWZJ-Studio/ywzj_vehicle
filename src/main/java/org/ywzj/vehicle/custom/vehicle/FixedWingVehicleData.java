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
        this.landingGearDragK = pojo.attributes.landingGearDragK;
        this.turnRateBySpeed = pojo.attributes.turnRateBySpeed;
        this.xTurnRate = pojo.attributes.xTurnRate;
        this.yTurnRate = pojo.attributes.yTurnRate;
        this.zTurnRate = pojo.attributes.zTurnRate;
        this.vortexOffsets = pojo.attributes.vortexOffsets;
        this.landingGearPartId = pojo.landingGearPartId;
    }

    public void inject(FixedWingVehicle vehicle) {
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
        vehicle.landingGearDragK = this.landingGearDragK;
        vehicle.turnRateBySpeed = this.turnRateBySpeed;
        vehicle.xTurnRate = this.xTurnRate;
        vehicle.yTurnRate = this.yTurnRate;
        vehicle.zTurnRate = this.zTurnRate;
        vehicle.vortexOffsets = this.vortexOffsets;
        vehicle.landingGearPartId = this.landingGearPartId;
    }

}
