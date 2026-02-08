package org.ywzj.vehicle.api.animation;

import net.minecraft.world.entity.Entity;
import org.ywzj.vehicle.client.render.animation.context.EntityContext;

/**
 * Interface for entities that support animation system.
 * 
 * @param <E> Entity type
 * @param <CTX> Animation context type
 */
public interface IAnimationEntity<E extends Entity, CTX extends EntityContext<E>> {
    /**
     * Get the current animation instance.
     */
    IAnimationInstance<CTX> getAnimationInstance();
}
