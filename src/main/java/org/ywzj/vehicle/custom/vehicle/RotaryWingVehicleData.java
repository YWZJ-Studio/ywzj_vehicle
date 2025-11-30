package org.ywzj.vehicle.custom.vehicle;

public class RotaryWingVehicleData extends BaseVehicleData {

    public float mainRotorForce = 1.4f * 0.7f * 1;
    public float xRotSpeedAcceleration = 1f;
    public float xRotSpeedMax = 4;
    public float yRotSpeedAcceleration = 1;
    public float yRotSpeedMax = 4;
    public float zRotSpeedAcceleration = 1;
    public float zRotSpeedMax = 4;
    public float maxAirSpeed = 1f;

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

}
