package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;
import org.ywzj.vehicle.vehicle.pojo.RotInfo;

public class RotatableUnitPojo extends PartUnitPojo {

    @SerializedName("rot_info")
    public RotInfo rotInfo;

}
