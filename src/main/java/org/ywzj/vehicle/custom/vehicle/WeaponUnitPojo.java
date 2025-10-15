package org.ywzj.vehicle.custom.vehicle;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WeaponUnitPojo {
    public enum PartType {
        @SerializedName("weapon")
        WEAPON,
        @SerializedName("spotter")
        SPOTTER
    }

    public static class RotInfo {
        @SerializedName("x_rot_speed")
        private float xRotSpeed = 1.0f;
        @SerializedName("y_rot_speed")
        private float yRotSpeed = 1.0f;
        @SerializedName("x_rot_max")
        private float xRotMax = 18;
        @SerializedName("x_rot_min")
        private float xRotMin = -18;

        public float getXRotSpeed() {
            return xRotSpeed;
        }
        public float getYRotSpeed() {
            return yRotSpeed;
        }

        public float getXRotMax() {
            return xRotMax;
        }

        public float getXRotMin() {
            return xRotMin;
        }
    }

    @SerializedName("rot_info")
    public RotInfo rotInfo = new RotInfo();

    @SerializedName("id")
    public String id;

    @SerializedName("type")
    public PartType partType = PartType.WEAPON;

    @SerializedName("name")
    public String name = "part.ywzj_vehicle.default_name";

    @SerializedName("structure_bone")
    public String structureBone = null;

    @SerializedName("parent")
    public String parent = null;

    @SerializedName("optical_sight_offset")
    public Vec3 opticalSightOffset = Vec3.ZERO;

    @SerializedName("operator_offset")
    public Vec3 operatorOffset = Vec3.ZERO;

    @SerializedName("seat_offset")
    public Vec3 seatOffset = Vec3.ZERO;

    @SerializedName("weapons")
    public List<ResourceLocation> weapons = List.of();
}
