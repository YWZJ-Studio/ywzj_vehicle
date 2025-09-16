package org.ywzj.vehicle.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DebugUtil {

    private static long time;
    private static boolean on;

    public static void timer(String item) {
        if (!on) {
            time = System.nanoTime();
        } else {
            System.out.println(item + ": " + (System.nanoTime() - time));
        }
        on = !on;
    }

    public static void particle(Level level, Vec3 pos) {
        particle(level, pos, null);
    }

    public static void particle(Level level, Vec3 pos, Object color) {
        particle(level, pos, color.hashCode());
    }

    public static void particle(Level level, Vec3 pos, Integer color) {
        if (level.isClientSide) {
            level.addParticle(new DustParticleOptions(color == null ? new Vector3f(0.0F, 1.0F, 0.0F) : intToVec3(color), 1.0F), true, pos.x, pos.y, pos.z, 0, 0, 0);
        } else {
            try {
                level = Minecraft.getInstance().level;
                level.addParticle(new DustParticleOptions(color == null ? new Vector3f(1.0F, 0.0F, 0.0F) : intToVec3(color), 1.0F), true, pos.x, pos.y, pos.z, 0, 0, 0);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Vector3f intToVec3(int color) {
        float r = (float) (((color >> 16) & 0xFF) / 255.0);
        float g = (float) (((color >> 8) & 0xFF) / 255.0);
        float b = (float) ((color & 0xFF) / 255.0);
        return new Vector3f(r, g, b);
    }

}
