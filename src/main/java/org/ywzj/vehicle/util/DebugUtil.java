package org.ywzj.vehicle.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DebugUtil {

    public static void particle(Level level, Vec3 pos) {
        if (level.isClientSide) {
            level.addParticle(new DustParticleOptions(new Vector3f(0.0F, 1.0F, 0.0F), 1.0F), true, pos.x, pos.y, pos.z, 0, 0, 0);
        } else {
            try {
                level = Minecraft.getInstance().level;
                level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F), true, pos.x, pos.y, pos.z, 0, 0, 0);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}
