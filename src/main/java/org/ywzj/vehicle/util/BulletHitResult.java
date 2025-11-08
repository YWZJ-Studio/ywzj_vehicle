package org.ywzj.vehicle.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class BulletHitResult extends EntityHitResult {

    private final boolean headshot;

    public BulletHitResult(Entity entity, Vec3 position, boolean headshot) {
        super(entity, position);
        this.headshot = headshot;
    }

    public boolean isHeadshot() {
        return this.headshot;
    }

}