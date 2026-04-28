package org.ywzj.vehicle.api.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;

import java.util.UUID;

public class HitVehicleEvent extends Event {

    public final UUID shooterUuid;
    public final int entityId;
    public final Vec3 hitRelativePosition;
    public final Vec3 hitRelativeVector;
    public final float damage;
    public final Component message;

    public HitVehicleEvent(UUID shooterUuid, int entityId, Vec3 hitRelativePosition, Vec3 hitRelativeVector, float damage, Component message) {
        this.shooterUuid = shooterUuid;
        this.entityId = entityId;
        this.hitRelativePosition = hitRelativePosition;
        this.hitRelativeVector = hitRelativeVector;
        this.damage = damage;
        this.message = message;
    }

}
