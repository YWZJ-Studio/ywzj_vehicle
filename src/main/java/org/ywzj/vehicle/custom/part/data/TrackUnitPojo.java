package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TrackUnitPojo extends SuspensionUnitPojo {

    @SerializedName("track_width")
    public float trackWidth = 0.5f;

    @SerializedName("tracks")
    public List<List<Vec3>> tracks = new ArrayList<>();

    public TrackUnitPojo() {
        this.isSeat = false;
    }

}
