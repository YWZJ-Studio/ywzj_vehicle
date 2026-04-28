package org.ywzj.vehicle.api.event;

import net.minecraft.world.damagesource.DamageSource;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class VehicleAttackEvent extends VehicleEvent {

    private final DamageSource source;
    private final float amount;

    public VehicleAttackEvent(AbstractVehicle vehicle, DamageSource source, float amount) {
        super(vehicle);
        this.source = source;
        this.amount = amount;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }

}
