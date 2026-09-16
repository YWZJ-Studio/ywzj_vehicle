package org.ywzj.vehicle.custom.vehicle;

import net.minecraft.world.level.Level;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.VesselVehicle;

public class VesselVehicleData extends BaseVehicleData<VesselVehicle> {

    public float brakeForce;
    public float forwardForce;
    public float backwardForce;
    public float maxSpeedForward;
    public float maxSpeedBackward;
    public float turnStep;
    public float maxTurn;

    @Override
    public AbstractVehicle fromCustom(Level level) {
        return new VesselVehicle(AllEntities.VESSEL_VEHICLE.get(), level);
    }

    public void build(VesselVehicleDataPojo pojo) {
        super.build(pojo);
        this.canWade = true;
        this.brakeForce = pojo.attributes.brakeForce;
        this.forwardForce = pojo.attributes.forwardForce;
        this.backwardForce = pojo.attributes.backwardForce;
        this.maxSpeedForward = pojo.attributes.maxSpeedForward;
        this.maxSpeedBackward = pojo.attributes.maxSpeedBackward;
        this.turnStep = pojo.attributes.turnStep;
        this.maxTurn = pojo.attributes.maxTurn;
    }

    @Override
    public void inject(VesselVehicle vehicle) {
        vehicle.brakeForce = this.brakeForce;
        vehicle.forwardForce = this.forwardForce;
        vehicle.backwardForce = this.backwardForce;
        vehicle.maxSpeedForward = this.maxSpeedForward;
        vehicle.maxSpeedBackward = this.maxSpeedBackward;
        vehicle.turnStep = this.turnStep;
        vehicle.maxTurn = this.maxTurn;
        if (vehicle.physicsEngine.physicsInfo.density > 1) {
            vehicle.physicsEngine.physicsInfo.density = 0.3f;
        }
    }

    @Override
    public boolean canWade() {
        return true;
    }

}
