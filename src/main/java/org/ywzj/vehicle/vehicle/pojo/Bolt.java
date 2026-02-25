package org.ywzj.vehicle.vehicle.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class Bolt {

    // 炮闩偏移，为武器枢轴相对于武器站枢轴的偏移
    @SerializedName("offset")
    public Vec3 offset;

    // 炮管长度，为发射物生成位置与炮管枢轴位置的距离
    @SerializedName("barrel_length")
    public float barrelLength;

    // 炮管相对于武器站正方向的离轴x旋转
    @SerializedName("x_rot")
    public float xRot;

    // 炮管相对于武器站正方向的离轴y旋转
    @SerializedName("y_rot")
    public float yRot;

    public Bolt(Vec3 offset, float barrelLength, float xRot, float yRot) {
        this.offset = offset;
        this.barrelLength = barrelLength;
        this.xRot = xRot;
        this.yRot = yRot;
    }

}
