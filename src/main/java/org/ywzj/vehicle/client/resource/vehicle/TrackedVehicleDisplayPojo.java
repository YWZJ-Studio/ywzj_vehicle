package org.ywzj.vehicle.client.resource.vehicle;

import com.google.gson.annotations.SerializedName;

public class TrackedVehicleDisplayPojo extends BaseDisplayPojo {

    @SerializedName("track_config")
    public TrackConfig trackConfig;

    public TrackedVehicleDisplayPojo() {}

}
