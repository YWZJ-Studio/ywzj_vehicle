package org.ywzj.vehicle.api.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

public class HitVehicleEvent extends Event {

    public final UUID shooterUuid;
    public final int entityId;
    public final Vec3 hitPosition;
    public final Vec3 hitVector;
    public float caliber;
    public final float damage;
    public final Component message;

    public HitVehicleEvent(UUID shooterUuid, int entityId, Vec3 hitPosition, Vec3 hitVector, float caliber, float damage, Component message) {
        this.shooterUuid = shooterUuid;
        this.entityId = entityId;
        this.hitPosition = hitPosition;
        this.hitVector = hitVector;
        this.caliber = caliber;
        this.damage = damage;
        this.message = message;
    }

}
