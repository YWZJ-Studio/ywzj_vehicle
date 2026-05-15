package org.ywzj.vehicle.vehicle.part;

import net.minecraft.world.phys.Vec3;

public class AfterburnerOffset {

    private final Vec3 offset;
    private final float scale;

    public AfterburnerOffset(Vec3 offset, float scale) {
        this.offset = offset;
        this.scale = scale;
    }

    public Vec3 getOffset() {
        return offset;
    }

    public float getScale() {
        return scale;
    }

}
