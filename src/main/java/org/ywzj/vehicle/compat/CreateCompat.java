package org.ywzj.vehicle.compat;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.collision.CollisionList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.event.VehicleCollectCollisionEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@EventBusSubscriber
public class CreateCompat {

    private static final String MOD_ID = "create";
    private static boolean IS_LOADED = false;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    @SubscribeEvent
    public static void onVehicleCollectCollision(VehicleCollectCollisionEvent event) {
        if (isLoaded()) {
            CreateCompatImplementation.collideVehicle(event);
        }
    }

}

class CreateCompatImplementation {

    protected static void collideVehicle(VehicleCollectCollisionEvent event) {
        AbstractVehicle vehicle = event.getVehicle();
        Level level = vehicle.level();
        VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
        Vector3f[] axes = mainCubeOBB.obb().getAxes();
        List<VehicleCubeOBB.CubePoint> surfacePoints = mainCubeOBB.cubePoints();
        List<VehicleCubeOBB.CubePoint> touchPoints = new ArrayList<>();
        HashSet<AbstractContraptionEntity> contraptionEntities = new HashSet<>();
        AABB aabb = vehicle.getBoundingBox();
        aabb.inflate(1);
        List<Entity> entities = level.getEntities(vehicle, aabb);
        for (Entity entity : entities) {
            if (entity instanceof AbstractContraptionEntity contraptionEntity) {
                contraptionEntities.add(contraptionEntity);
            }
        }
        for (VehicleCubeOBB.CubePoint point : surfacePoints) {
            Vec3 worldPos = new Vec3(point.worldPos(axes));
            BlockPos blockPos = BlockPos.containing(worldPos);
            contraptionEntities.forEach(contraptionEntity -> {
                CollisionList simplifiedEntityColliders = contraptionEntity.getContraption().getSimplifiedEntityColliders();
                if (simplifiedEntityColliders != null) {
                    if (contains(simplifiedEntityColliders, worldPos)) {
                        point.cubePointContext.setBlockPos(Vec3.atBottomCenterOf(blockPos));
                        point.cubePointContext.setBlockState(null);
                        touchPoints.add(point);
                    }
                }
            });
        }
        event.getTouchPoints().addAll(touchPoints);
    }

    public static boolean contains(CollisionList collisionList, Vec3 pos) {
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
