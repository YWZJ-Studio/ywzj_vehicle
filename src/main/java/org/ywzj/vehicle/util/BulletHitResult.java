package org.ywzj.vehicle.util;

import net.minecraft.world.phys.EntityHitResult;

public class BulletHitResult extends EntityHitResult {
    private final boolean headshot;

    public BulletHitResult(EntityUtil.EntityResult result) {
        super(result.getEntity(), result.getHitPos());
        this.headshot = result.isHeadshot();
    }

    public boolean isHeadshot() {
        return this.headshot;
    }
}