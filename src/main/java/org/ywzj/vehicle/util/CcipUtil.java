package org.ywzj.vehicle.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.PhysicsEngine;

public class CcipUtil {

    private static final int MAX_TICKS = 1200;

    public static Vec3 computeCcipImpact(Level level, Vec3 startPos, Vec3 startVelocity, float friction) {
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
                double dragFactor = friction * speed;
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

    public static Vec3 computeCcipImpactRocket(Level level, Vec3 startPos, Vec3 startVelocity, float thrust, float mass, float motorBurnTime, float friction) {
        double x = startPos.x;
        double y = startPos.y;
        double z = startPos.z;
        double vx = startVelocity.x;
        double vy = startVelocity.y;
        double vz = startVelocity.z;
        double g = PhysicsEngine.G;
        for (int tick = 0; tick < MAX_TICKS; tick++) {
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (tick <= motorBurnTime && speed > 0) {
                double acceleration = thrust / mass;
                vx += acceleration * (vx / speed);
                vy += acceleration * (vy / speed);
                vz += acceleration * (vz / speed);
            }
            if (speed > 0) {
                double dragFactor = friction * speed;
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
