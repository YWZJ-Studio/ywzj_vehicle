package org.ywzj.vehicle.custom.vehicle;

import net.minecraft.world.level.Level;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.custom.part.data.RopeUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;
import org.ywzj.vehicle.vehicle.part.LandingGearUnit;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RopeUnit;

import java.util.LinkedHashMap;
import java.util.Map;

public class RotaryWingVehicleData extends BaseVehicleData<RotaryWingVehicle> {

    public float mainRotorForce = 1.4f * 0.7f * 1;
    public float ceiling = 256;
    public float xRotSpeedAcceleration = 1f;
    public float xRotSpeedMax = 4;
    public float yRotSpeedAcceleration = 1;
    public float yRotSpeedMax = 4;
    public float zRotSpeedAcceleration = 1;
    public float zRotSpeedMax = 4;
    public float maxAirSpeed = 1f;
    public boolean fastRoping;
    public String fastRopingDoorId;
    public String landingGearPartId;

    @Override
    public AbstractVehicle fromCustom(Level level) {
       return new RotaryWingVehicle(AllEntities.ROTARY_WING_VEHICLE.get(), level);
    }

    @Override
    public PartUnitsAndSeats createPartUnits(AbstractVehicle vehicle) {
        if (vehicle instanceof RotaryWingVehicle rotaryWingVehicle) {
            PartUnitsAndSeats result = super.createPartUnits(vehicle);
            PartUnit<?> landingGearUnit = result.partUnitMap().get(this.landingGearPartId);
            if (landingGearUnit instanceof LandingGearUnit switchableUnit) {
                rotaryWingVehicle.landingGear = switchableUnit;
            }
            Map<String, PartUnit<?>> partUnitMap = new LinkedHashMap<>(result.partUnitMap());
            if (fastRoping) {
                RopeUnit ropeUnit = new RopeUnit(partUnitMap.size(), vehicle, RopeUnitData.create("fast_roping_rope", 32));
                partUnitMap.put(ropeUnit.getId(), ropeUnit);
                ropeUnit.retract();
                rotaryWingVehicle.fastRopingRope = ropeUnit;
            }
            RopeUnit cargoRope = new RopeUnit(partUnitMap.size(), vehicle, RopeUnitData.create("cargo_rope", 8));
            partUnitMap.put(cargoRope.getId(), cargoRope);
            cargoRope.retract();
            rotaryWingVehicle.cargoRope = cargoRope;
            return new PartUnitsAndSeats(partUnitMap, result.seats());
        }
        return null;
    }

    public void build(RotaryWingVehicleDataPojo pojo) {
        super.build(pojo);
        this.mainRotorForce = pojo.attributes.mainRotorForce;
        this.ceiling = pojo.attributes.ceiling;
        this.xRotSpeedAcceleration = pojo.attributes.xRotSpeedAcceleration;
        this.xRotSpeedMax = pojo.attributes.xRotSpeedMax;
        this.yRotSpeedAcceleration = pojo.attributes.yRotSpeedAcceleration;
        this.yRotSpeedMax = pojo.attributes.yRotSpeedMax;
        this.zRotSpeedAcceleration = pojo.attributes.zRotSpeedAcceleration;
        this.zRotSpeedMax = pojo.attributes.zRotSpeedMax;
        this.maxAirSpeed = pojo.attributes.maxAirSpeed;
        this.fastRoping = pojo.attributes.fastRoping;
        this.fastRopingDoorId = pojo.attributes.fastRopingDoorId;
        this.landingGearPartId = pojo.landingGearPartId;
    }

    public void inject(RotaryWingVehicle vehicle) {
        vehicle.mainRotorForce = this.mainRotorForce;
        vehicle.ceiling = this.ceiling;
        vehicle.xRotSpeedAcceleration = this.xRotSpeedAcceleration;
        vehicle.xRotSpeedMax = this.xRotSpeedMax;
        vehicle.yRotSpeedAcceleration = this.yRotSpeedAcceleration;
        vehicle.yRotSpeedMax = this.yRotSpeedMax;
        vehicle.zRotSpeedAcceleration = this.zRotSpeedAcceleration;
        vehicle.zRotSpeedMax = this.zRotSpeedMax;
        vehicle.maxAirSpeed = this.maxAirSpeed;
        vehicle.fastRoping = this.fastRoping;
        vehicle.fastRopingDoorId = this.fastRopingDoorId;
        vehicle.landingGearPartId = this.landingGearPartId;
    }

}
