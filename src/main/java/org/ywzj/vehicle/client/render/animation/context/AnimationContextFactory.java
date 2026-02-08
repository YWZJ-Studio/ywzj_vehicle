package org.ywzj.vehicle.client.render.animation.context;

import net.minecraft.world.entity.Entity;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.TrackedVehicle;

/**
 * Factory for creating animation contexts for specific entity types.
 * This allows VehicleDisplay to create the correct context type at runtime.
 *
 * @param <E> Entity type
 * @param <CTX> Context type
 */
@FunctionalInterface
public interface AnimationContextFactory<E extends Entity, CTX extends EntityContext<E>> {
    
    /**
     * Create a new animation context for the given entity
     */
    CTX create(E entity);
    
    /**
     * Factory for vehicle contexts
     */
    static <V extends AbstractVehicle> AnimationContextFactory<V, VehicleContext<V>> vehicle() {
        return VehicleContext::new;
    }

    static AnimationContextFactory<TrackedVehicle, TrackedVehicleContext> trackedVehicle() {
        return TrackedVehicleContext::new;
    }
    
    /**
     * Factory for generic entity contexts
     */
    static <E extends Entity> AnimationContextFactory<E, EntityContext<E>> entity() {
        return EntityContext::new;
    }
}
