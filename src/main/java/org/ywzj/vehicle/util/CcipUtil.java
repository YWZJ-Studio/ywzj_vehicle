package org.ywzj.vehicle.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.PhysicsEngine;

public class CcipUtil {

    private static final int MAX_TICKS = 1200;

    public static Vec3 computeCcipImpact(Level level, Vec3 startPos, Vec3 startVelocity, float dragCoefficient) {
        double x = startPos.x;
        double y = startPos.y;
        double z = startPos.z;
        double vx = startVelocity.x;
        double vy = startVelocity.y;
        double vz = startVelocity.z;
        double g = PhysicsEngine.G;
        for (int tick = 0; tick < MAX_TICKS; tick++) {
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (speed > 0) {
                double dragFactor = dragCoefficient * speed;
                vx -= dragFactor * vx;
                vy -= dragFactor * vy;
                vz -= dragFactor * vz;
            }
            vy -= g;
            x += vx;
            y += vy;
            z += vz;
            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            if (y <= groundY) {
                return new Vec3(x, groundY, z);
            }
        }
        return null;
    }

}
