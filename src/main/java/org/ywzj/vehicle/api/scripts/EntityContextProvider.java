package org.ywzj.vehicle.api.scripts;


import net.minecraft.world.entity.Entity;
import org.ywzj.vehicle.api.scripts.math.ScriptVec3;

@SuppressWarnings("unused")
public class EntityContextProvider<T extends Entity> {
    protected T entity;

    public EntityContextProvider(T entity) {
        this.entity = entity;
    }

    public int getTickCount() {
        return entity.tickCount;
    }

    public boolean hasPassenger() {
        return !entity.getPassengers().isEmpty();
    }

    public ScriptVec3 position() {
        var pos = entity.position();
        return ScriptVec3.of(pos);
    }

    public ScriptVec3 getLookAngle() {
        var look = entity.getLookAngle();
        return ScriptVec3.of(look);
    }

    public float getHealth() {
        return 0f;
    }
}
