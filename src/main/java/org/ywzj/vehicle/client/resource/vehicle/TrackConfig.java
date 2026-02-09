package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.annotations.SerializedName;

/**
 * Configuration for track animations in tracked vehicles.
 * Defines the properties of left and right track animations.
 */
public class TrackConfig {
    
    /**
     * Name of the left track animation (e.g., "tread_l_move")
     */
    @SerializedName("left_track")
    public String leftTrack;
    
    /**
     * Name of the right track animation (e.g., "tread_r_move")
     */
    @SerializedName("right_track")
    public String rightTrack;
    
    /**
     * Length of one track module in meters.
     * This determines how much the animation progresses per meter of movement.
     */
    @SerializedName("module_length")
    public float moduleLength = 0.5f;
    
    /**
     * Width between left and right tracks in meters.
     * Used to calculate speed difference during turns.
     */
    @SerializedName("track_width")
    public float trackWidth = 1.0f;
    
    /**
     * Validates the track configuration.
     * @return true if the configuration is valid
     */
    public boolean isValid() {
        return leftTrack != null && !leftTrack.isEmpty()
            && rightTrack != null && !rightTrack.isEmpty()
            && moduleLength > 0
            && trackWidth > 0;
    }
}
