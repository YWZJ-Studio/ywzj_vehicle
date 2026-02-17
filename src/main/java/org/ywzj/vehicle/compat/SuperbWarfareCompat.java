package org.ywzj.vehicle.compat;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3d;
import org.ywzj.vehicle.util.BulletHitResult;

import java.util.Optional;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class SuperbWarfareCompat {

    private static final String MOD_ID = "superbwarfare";
    private static boolean IS_LOADED = false;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    public static BulletHitResult getHitResult(Entity bulletEntity, Entity entity, Vec3 startVec, Vec3 endVec) {
        if (entity instanceof OBBEntity obbEntity && !obbEntity.enableAABB()) {
            var obbList = obbEntity.getOBBs();
            for (var obb : obbList) {
                Optional<Vector3d> optional = obb.clip(OBB.vec3ToVector3d(startVec), OBB.vec3ToVector3d(endVec));
                if (optional.isPresent()) {
                    if (bulletEntity.level() instanceof ServerLevel serverLevel && bulletEntity.getDeltaMovement().lengthSqr() > 0.01) {
                        Vec3 hitPos = OBB.vector3dToVec3(optional.get());
                        bulletEntity.level().playSound(null, BlockPos.containing(hitPos), ModSounds.HIT.get(), SoundSource.PLAYERS, 1, 1);
                        sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.2, false);
                        sendParticle(serverLevel, ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.01, false);
                    }
                    return new BulletHitResult(entity, OBB.vector3dToVec3(optional.get()), false);
                }
            }
        }
        return null;
    }

}
