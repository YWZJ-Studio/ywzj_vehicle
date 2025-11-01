package org.ywzj.vehicle.api.event;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

public class HitVehicleEvent extends Event {

    public final UUID shooterUuid;
    public final int entityId;
    public final Vec3 hitRelativePosition;
    public final Vec3 hitRelativeVector;

    public HitVehicleEvent(UUID shooterUuid, int entityId, Vec3 hitRelativePosition, Vec3 hitRelativeVector) {
        this.shooterUuid = shooterUuid;
        this.entityId = entityId;
        this.hitRelativePosition = hitRelativePosition;
        this.hitRelativeVector = hitRelativeVector;
    }

}
