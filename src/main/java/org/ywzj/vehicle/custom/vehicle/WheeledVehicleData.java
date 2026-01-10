package org.ywzj.vehicle.custom.vehicle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;

public class WheeledVehicleData extends BaseVehicleData<WheeledVehicle> {

    public float brakeForce;
    public float forwardForce;
    public float backwardForce;
    public float maxSpeedForward;
    public float maxSpeedBackward;
    public float turnStep;
    public float maxTurn;

    @Override
    public WheeledVehicle summon(ResourceLocation customId, Level level, Vec3 position, float xRot, float yRot) {
        WheeledVehicle wheeledVehicle = new WheeledVehicle(AllEntities.TRACKED_VEHICLE.get(), level);
        wheeledVehicle.setCustomId(customId);
        wheeledVehicle.setPos(position);
        wheeledVehicle.setXRot(xRot);
        wheeledVehicle.setYRot(yRot);
        level.addFreshEntity(wheeledVehicle);
        return wheeledVehicle;
    }

    public void build(WheeledVehicleDataPojo pojo) {
        super.build(pojo);
        this.brakeForce = pojo.attributes.brakeForce;
        this.forwardForce = pojo.attributes.forwardForce;
        this.backwardForce = pojo.attributes.backwardForce;
        this.maxSpeedForward = pojo.attributes.maxSpeedForward;
        this.maxSpeedBackward = pojo.attributes.maxSpeedBackward;
        this.turnStep = pojo.attributes.turnStep;
        this.maxTurn = pojo.attributes.maxTurn;
    }

    public void inject(WheeledVehicle vehicle) {
        vehicle.brakeForce = this.brakeForce;
        vehicle.forwardForce = this.forwardForce;
        vehicle.backwardForce = this.backwardForce;
        vehicle.maxSpeedForward = this.maxSpeedForward;
        vehicle.maxSpeedBackward = this.maxSpeedBackward;
        vehicle.turnStep = this.turnStep;
        vehicle.maxTurn = this.maxTurn;
    }

}
