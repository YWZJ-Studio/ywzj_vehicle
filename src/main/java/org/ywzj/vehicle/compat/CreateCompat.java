package org.ywzj.vehicle.compat;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.collision.CollisionList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.collision.CollisionProvider;
import org.ywzj.vehicle.api.collision.CollisionProviders;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;

public class CreateCompat {

    private static final String MOD_ID = "create";
    private static boolean IS_LOADED = false;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID);
        if (IS_LOADED) {
            // Only register when Create is present to avoid classloading.
            CollisionProviders.register(new CreateCollisionProvider());
        }
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

}

/**
 * Contributes contacts against Create contraption colliders.
 */
class CreateCollisionProvider implements CollisionProvider {

    @Nullable
    @Override
    public Session begin(AbstractVehicle vehicle, AABB hullBounds) {
        Level level = vehicle.level();
        List<Contraption> contraptions = new ArrayList<>();
        for (Entity entity : level.getEntities(vehicle, hullBounds)) {
            if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                CollisionList simplifiedEntityColliders =
                        contraptionEntity.getContraption().getSimplifiedEntityColliders();
                if (simplifiedEntityColliders != null) {
                    contraptions.add(new Contraption(contraptionEntity, simplifiedEntityColliders));
                }
            }
        }
        // No contraption in range: skip every point rather than testing an empty set per point.
        return contraptions.isEmpty() ? null : new CreateSession(contraptions);
    }

    /**
     * One contraption's colliders with the entity that positions them.
     */
    record Contraption(AbstractContraptionEntity entity, CollisionList colliders) {}

    record CreateSession(List<Contraption> contraptions) implements Session {

        @Override
        public boolean collectBoxes(AABB bounds, List<AABB> out) {
            Vec3 zero = new Vec3(0, 0, 0);
            for (Contraption contraption : contraptions) {
                AbstractContraptionEntity entity = contraption.entity();
                // Affine property: extract rotation from origin difference; four calls instead of eight.
                Vec3 origin = entity.toGlobalVector(zero, 1.0F);
                Vec3 axisX = entity.toGlobalVector(new Vec3(1, 0, 0), 1.0F).subtract(origin);
                Vec3 axisY = entity.toGlobalVector(new Vec3(0, 1, 0), 1.0F).subtract(origin);
                Vec3 axisZ = entity.toGlobalVector(new Vec3(0, 0, 1), 1.0F).subtract(origin);

                CollisionList colliders = contraption.colliders();
                for (int i = 0; i < colliders.size; i++) {
                    double cx = colliders.centerX[i];
                    double cy = colliders.centerY[i];
                    double cz = colliders.centerZ[i];
                    double ex = colliders.extentsX[i];
                    double ey = colliders.extentsY[i];
                    double ez = colliders.extentsZ[i];
                    double centreX = origin.x + axisX.x * cx + axisY.x * cy + axisZ.x * cz;
                    double centreY = origin.y + axisX.y * cx + axisY.y * cy + axisZ.y * cz;
                    double centreZ = origin.z + axisX.z * cx + axisY.z * cy + axisZ.z * cz;
                    // Axis-aligned bound of the rotated box. Loose while a contraption is turning,
                    // which is allowed: contactAt still tests the real box.
                    double spanX = Math.abs(axisX.x) * ex + Math.abs(axisY.x) * ey + Math.abs(axisZ.x) * ez;
                    double spanY = Math.abs(axisX.y) * ex + Math.abs(axisY.y) * ey + Math.abs(axisZ.y) * ez;
                    double spanZ = Math.abs(axisX.z) * ex + Math.abs(axisY.z) * ey + Math.abs(axisZ.z) * ez;
                    if (centreX + spanX <= bounds.minX || centreX - spanX >= bounds.maxX
                            || centreY + spanY <= bounds.minY || centreY - spanY >= bounds.maxY
                            || centreZ + spanZ <= bounds.minZ || centreZ - spanZ >= bounds.maxZ) {
                        continue;
                    }
                    out.add(new AABB(
                            centreX - spanX, centreY - spanY, centreZ - spanZ,
                            centreX + spanX, centreY + spanY, centreZ + spanZ));
                }
            }
            return true;
        }

        @Nullable
        @Override
        public Contact contactAt(VehicleCubeOBB.CubePoint point, Vector3f worldPos) {
            Vec3 world = new Vec3(worldPos.x, worldPos.y, worldPos.z);
            for (int i = 0, size = contraptions.size(); i < size; i++) {
                Contraption contraption = contraptions.get(i);
                Vec3 local = contraption.entity().toLocalVector(world, 1.0F);
                if (contains(contraption.colliders(), local)) {
                    // Create colliders are not blocks, so there is no state to report.
                    return new Contact(new Vec3(
                            Mth.floor(world.x) + 0.5, Mth.floor(world.y), Mth.floor(world.z) + 0.5), null);
                }
            }
            return null;
        }

    }

    static boolean contains(CollisionList collisionList, Vec3 pos) {
        double px = pos.x;
        double py = pos.y;
        double pz = pos.z;
        for (int i = 0; i < collisionList.size; i++) {
            if (Math.abs(px - collisionList.centerX[i]) > collisionList.extentsX[i]) continue;
            if (Math.abs(py - collisionList.centerY[i]) > collisionList.extentsY[i]) continue;
            if (Math.abs(pz - collisionList.centerZ[i]) > collisionList.extentsZ[i]) continue;
            return true;
        }
        return false;
    }

}
