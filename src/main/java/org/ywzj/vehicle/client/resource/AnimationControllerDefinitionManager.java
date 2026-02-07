package org.ywzj.vehicle.client.resource;

import org.ywzj.vehicle.client.resource.animation.AnimationControllerDefinition;
import org.ywzj.vehicle.custom.serialize.GsonUtil;

/**
 * Resource reload listener for animation controller definitions.
 * Loads animation controller JSON files from the animation_controllers/ directory.
 */
public class AnimationControllerDefinitionManager extends JsonDataManager<AnimationControllerDefinition> {

    public AnimationControllerDefinitionManager() {
        super(AnimationControllerDefinition.class, GsonUtil.GSON, "animation_controllers", "AnimationController");
    }
}
