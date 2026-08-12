package org.ywzj.vehicle.api.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface DetachedBodyVehicle {

    boolean isDetachedBodyActive();

    @Nullable
    Vec3 getDetachedBodyAnchor(Entity operator);

    void setDetachedBodyAnchor(Entity operator, @Nullable Vec3 anchor);

    void clearDetachedBodyAnchors();

    Collection<Entity> getDetachedOperators();

    default int getDetachedStreamRadius() {
        return -1;
    }

}
