package org.ywzj.vehicle.client.render.animation.graph;

import java.util.Set;

/**
 * Bone mask for selective pose blending.
 * Masks must be pre-registered in the engine.
 * JSON can only reference masks by name.
 */
public class BoneMask {
    private final String name;
    private final Set<String> boneNames;

    public BoneMask(String name, Set<String> boneNames) {
        this.name = name;
        this.boneNames = Set.copyOf(boneNames);
    }

    /**
     * Check if a bone is included in this mask
     */
    public boolean contains(String boneName) {
        return boneNames.contains(boneName);
    }

    public String getName() {
        return name;
    }

    public Set<String> getBoneNames() {
        return boneNames;
    }
}
