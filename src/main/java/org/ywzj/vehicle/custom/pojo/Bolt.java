package org.ywzj.vehicle.custom.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

/**
 * @param offset       炮闩偏移，为武器枢轴相对于武器站枢轴的偏移
 * @param barrelLength 炮管长度，为发射物生成位置与武器枢轴位置的距离
 */
public record Bolt(
        @SerializedName("offset")
        Vec3 offset,
        @SerializedName("barrel_length")
        float barrelLength
) {
}
