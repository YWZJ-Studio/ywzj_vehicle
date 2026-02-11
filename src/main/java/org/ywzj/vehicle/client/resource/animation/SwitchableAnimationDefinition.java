package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;

/**
 * Definition for switchable animation (doors, landing gear, etc.)
 */
public class SwitchableAnimationDefinition {
    /**
     * Part unit ID that controls this switchable animation
     */
    @SerializedName("part_id")
    private String partId;

    /**
     * Animation name to play
     */
    @SerializedName("animation")
    private String animation;

    public String getPartId() {
        return partId;
    }

    public String getAnimation() {
        return animation;
    }
}
