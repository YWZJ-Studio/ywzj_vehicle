package org.ywzj.vehicle.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.vehicle.PhysicsEngine;

public class CcipUtil {

    private static final int MAX_TICKS = 1200;

    public static Vec3 computeCcip(Level level, Vec3 startPos, Vec3 startVelocity, float friction) {
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

    public static Vec3 computeCcipRocket(Level level, Vec3 startPos, Vec3 startVelocity, float thrust, float mass, float motorBurnTime, float friction) {
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

    public static Vec3 computeCcipCannon(Level level, Entity entity, Vec3 startPos, Vec3 startVelocity, float friction, int life) {
        Vec3 position = startPos;
        Vec3 velocity = startVelocity;
        double g = PhysicsEngine.G;
        int maxTicks = Math.min(MAX_TICKS, life + 1);
        for (int tick = 0; tick < maxTicks; tick++) {
            Vec3 nextPosition = position.add(velocity);
            HitResult result = level.clip(new ClipContext(position, nextPosition,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (result.getType() != HitResult.Type.MISS) {
                return result.getLocation();
            }
            position = nextPosition;
            velocity = velocity.scale(1 - friction).add(0, -g, 0);
        }
        return null;
    }

}
