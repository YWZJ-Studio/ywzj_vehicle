package org.ywzj.vehicle.custom.vehicle;

import net.minecraft.world.level.Level;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.RotaryWingVehicle;

public class RotaryWingVehicleData extends BaseVehicleData<RotaryWingVehicle> {

    public float mainRotorForce = 1.4f * 0.7f * 1;
    public float xRotSpeedAcceleration = 1f;
    public float xRotSpeedMax = 4;
    public float yRotSpeedAcceleration = 1;
    public float yRotSpeedMax = 4;
    public float zRotSpeedAcceleration = 1;
    public float zRotSpeedMax = 4;
    public float maxAirSpeed = 1f;

    @Override
    public AbstractVehicle fromCustom(Level level) {
       return new RotaryWingVehicle(AllEntities.ROTARY_WING_VEHICLE.get(), level);
    }

    public void build(RotaryWingVehicleDataPojo pojo) {
        super.build(pojo);
        this.mainRotorForce = pojo.attributes.mainRotorForce;
        this.xRotSpeedAcceleration = pojo.attributes.xRotSpeedAcceleration;
        this.xRotSpeedMax = pojo.attributes.xRotSpeedMax;
        this.yRotSpeedAcceleration = pojo.attributes.yRotSpeedAcceleration;
        this.yRotSpeedMax = pojo.attributes.yRotSpeedMax;
        this.zRotSpeedAcceleration = pojo.attributes.zRotSpeedAcceleration;
        this.zRotSpeedMax = pojo.attributes.zRotSpeedMax;
        this.maxAirSpeed = pojo.attributes.maxAirSpeed;
    }

    public void inject(RotaryWingVehicle vehicle) {
        vehicle.mainRotorForce = this.mainRotorForce;
        vehicle.xRotSpeedAcceleration = this.xRotSpeedAcceleration;
        vehicle.xRotSpeedMax = this.xRotSpeedMax;
        vehicle.yRotSpeedAcceleration = this.yRotSpeedAcceleration;
        vehicle.yRotSpeedMax = this.yRotSpeedMax;
        vehicle.zRotSpeedAcceleration = this.zRotSpeedAcceleration;
        vehicle.zRotSpeedMax = this.zRotSpeedMax;
        vehicle.maxAirSpeed = this.maxAirSpeed;
    }

}
