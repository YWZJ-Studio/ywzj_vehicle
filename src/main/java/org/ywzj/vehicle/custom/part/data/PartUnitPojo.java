package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

/**
 * 辅助反序列化载具部件单元的Pojo类
 */
public class PartUnitPojo {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name = "part.ywzj_vehicle.default_name";

    @SerializedName("structure_bone")
    public String structureBone = null;

    @SerializedName("seat_offset")
    public Vec3 seatOffset = Vec3.ZERO;

    @SerializedName("owner_view_offset")
    public Vec3 ownerViewOffset = null;

    @SerializedName("is_seat")
    public boolean isSeat = true;

}
