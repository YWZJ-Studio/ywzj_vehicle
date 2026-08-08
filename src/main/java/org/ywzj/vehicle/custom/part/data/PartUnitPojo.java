package org.ywzj.vehicle.custom.part.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.pojo.DefenseStats;
import org.ywzj.vehicle.vehicle.pojo.PassengerPose;

import java.util.ArrayList;
import java.util.List;

/**
 * 辅助反序列化载具部件单元的Pojo类
 */
public class PartUnitPojo {

    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name = "part.ywzj_vehicle.default_name";

    @SerializedName("max_health")
    public float maxHealth = -1;

    @SerializedName("defense_stats")
    public DefenseStats defenseStats = new DefenseStats();

    @SerializedName("render_bone")
    public String renderBone = null;

    @SerializedName("structure_bone")
    public String structureBone = null;

    @SerializedName("detachable")
    public boolean detachable = true;

    @SerializedName("is_seat")
    public boolean isSeat = true;

    @SerializedName("seat_rot")
    public float seatRot;

    @SerializedName("seat_offset")
    public Vec3 seatOffset = Vec3.ZERO;

    @SerializedName("passenger_pose")
    public PassengerPose passengerPose = null;

    @SerializedName("passenger_can_use_item")
    public boolean passengerCanUseItem = false;

    @SerializedName("owner_view_offset")
    public Vec3 ownerViewOffset = null;

    @SerializedName("render_model")
    public boolean renderModel = false;

    @SerializedName("display_id")
    public ResourceLocation displayId;

    @SerializedName("display_offset")
    public Vec3 displayOffset;

    @SerializedName("sub_part_unit_ids")
    public List<String> subPartUnitIds = new ArrayList<>();

}
