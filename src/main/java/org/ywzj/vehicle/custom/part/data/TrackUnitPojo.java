package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TrackUnitPojo extends PartUnitPojo {

    @SerializedName("tracks")
    public List<List<Vec3>> tracks = new ArrayList<>();

    @SerializedName("suspensions")
    public Map<String, List<String>> suspensions = new LinkedHashMap<>();

    public TrackUnitPojo() {
        this.isSeat = false;
    }

}
