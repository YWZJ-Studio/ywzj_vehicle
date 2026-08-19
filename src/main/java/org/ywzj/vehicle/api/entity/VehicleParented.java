package org.ywzj.vehicle.api.entity;

import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.vehicle.parenting.DeckAttachment;

/**
 * Storage slot for an entity's parent frame, implemented on all MC entities by a mixin.
 */
public interface VehicleParented {

    @Nullable
    DeckAttachment ywzj_vehicle$deckAttachment();

    void ywzj_vehicle$setDeckAttachment(@Nullable DeckAttachment attachment);

}
