package org.ywzj.vehicle.custom.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;

public class TrackedVehicleData extends BaseVehicleData<TrackedVehicle> {

    public float brakeAcceleration;
    public float forwardAcceleration;
    public float backwardAcceleration;
    public float maxSpeedForward;
    public float maxSpeedBackward;
    public float turnAcceleration;
    public float maxTurn;

    @Override
    public AbstractVehicle summon(ResourceLocation customId, Level level, Vec3 position, float xRot, float yRot) {
        TrackedVehicle trackedVehicle = new TrackedVehicle(AllEntities.TRACKED_VEHICLE.get(), level);
        trackedVehicle.setCustomId(customId);
        trackedVehicle.setPos(position);
        trackedVehicle.setXRot(xRot);
        trackedVehicle.setYRot(yRot);
        level.addFreshEntity(trackedVehicle);
        return trackedVehicle;
    }

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

    public void inject(TrackedVehicle vehicle) {
        vehicle.brakeAcceleration = this.brakeAcceleration;
        vehicle.forwardAcceleration = this.forwardAcceleration;
        vehicle.backwardAcceleration = this.backwardAcceleration;
        vehicle.maxSpeedForward = this.maxSpeedForward;
        vehicle.maxSpeedBackward = this.maxSpeedBackward;
        vehicle.turnAcceleration = this.turnAcceleration;
        vehicle.maxTurn = this.maxTurn;
    }

}
