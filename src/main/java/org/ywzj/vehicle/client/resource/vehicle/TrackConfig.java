package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.YwzjVehicle;

public class TrackConfig {

    @SerializedName("left_track")
    public String leftTrack;

    @SerializedName("right_track")
    public String rightTrack;

    @SerializedName("model")
    public ResourceLocation model = YwzjVehicle.resourceLocation("ywzj_vehicle:effect/track_link");

    @SerializedName("texture")
    public ResourceLocation texture = YwzjVehicle.resourceLocation("ywzj_vehicle:textures/effect/track_link.png");

    @SerializedName("module_length")
    public float moduleLength = 6f / 16.0f;

    @SerializedName("track_width")
    public float trackWidth = 1.0f;

    public boolean isValid() {
        return leftTrack != null && !leftTrack.isEmpty()
            && rightTrack != null && !rightTrack.isEmpty()
            && moduleLength > 0
            && trackWidth > 0;
    }

}
