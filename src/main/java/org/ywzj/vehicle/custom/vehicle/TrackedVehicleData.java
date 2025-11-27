package org.ywzj.vehicle.custom.vehicle;

public class TrackedVehicleData extends BaseVehicleData {

    public float brakeAcceleration;
    public float forwardAcceleration;
    public float backwardAcceleration;
    public float maxSpeedForward;
    public float maxSpeedBackward;
    public float turnAcceleration;
    public float maxTurn;

    public void build(TrackedVehicleDataPojo pojo) {
        super.build(pojo);
        this.brakeAcceleration = pojo.attributes.brakeAcceleration;
        this.forwardAcceleration = pojo.attributes.forwardAcceleration;
        this.backwardAcceleration = pojo.attributes.backwardAcceleration;
        this.maxSpeedForward = pojo.attributes.maxSpeedForward;
        this.maxSpeedBackward = pojo.attributes.maxSpeedBackward;
        this.turnAcceleration = pojo.attributes.turnAcceleration;
        this.maxTurn = pojo.attributes.maxTurn;
    }

}
