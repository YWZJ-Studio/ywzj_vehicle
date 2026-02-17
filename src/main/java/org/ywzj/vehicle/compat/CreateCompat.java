package org.ywzj.vehicle.compat;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.event.VehicleCollectCollisionEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
                Optional<List<AABB>> collidableBBsOptional = contraptionEntity.getContraption().getSimplifiedEntityColliders();
                if (collidableBBsOptional.isPresent()) {
                    List<AABB> collidableBBs = collidableBBsOptional.get();
                    if (collidableBBs.stream().anyMatch(bb -> bb.move(contraptionEntity.position()).contains(worldPos))) {
                        point.cubePointContext.setBlockPos(blockPos);
                        point.cubePointContext.setBlockState(null);
                        touchPoints.add(point);
                    }
                }
            });
        }
        event.getTouchPoints().addAll(touchPoints);
    }

}
