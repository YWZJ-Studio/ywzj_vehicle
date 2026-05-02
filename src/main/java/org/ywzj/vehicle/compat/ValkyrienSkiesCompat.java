package org.ywzj.vehicle.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.event.VehicleCollectCollisionEvent;
import org.ywzj.vehicle.api.event.VehicleMoveEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ValkyrienSkiesCompat {

    private static final String MOD_ID = "valkyrienskies";
    private static boolean IS_LOADED = false;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    @SubscribeEvent
    public static void onVehicleMoveEvent(VehicleMoveEvent event) {
        if (isLoaded()) {
            ValkyrienSkiesCompatImplementation.dragVehicle(event.getVehicle());
        }
    }

    @SubscribeEvent
    public static void onVehicleCollectCollision(VehicleCollectCollisionEvent event) {
        if (isLoaded()) {
            ValkyrienSkiesCompatImplementation.collideVehicle(event);
        }
    }

}

class ValkyrienSkiesCompatImplementation {

    protected static void dragVehicle(AbstractVehicle vehicle) {
        if (EntityShipCollisionUtils.isCollidingWithUnloadedShips(vehicle)) {
            vehicle.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
            return;
        }
        AABB box = AABB.ofSize(vehicle.position().add(0, 0.49, 0), 1, 1, 1);
        Vec3 movement = EntityShipCollisionUtils.INSTANCE
                .adjustEntityMovementForShipCollisions(vehicle, vehicle.getDeltaMovement(), box, vehicle.level());
        vehicle.setDeltaMovement(movement);
        vehicle.physicsEngine.velocity = movement.toVector3f();
    }

    protected static void collideVehicle(VehicleCollectCollisionEvent event) {
        AbstractVehicle vehicle = event.getVehicle();
        Level level = vehicle.level();
        VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
        Vector3f[] axes = mainCubeOBB.obb().getAxes();
        List<VehicleCubeOBB.CubePoint> surfacePoints = mainCubeOBB.cubePoints();
        List<VehicleCubeOBB.CubePoint> touchPoints = new ArrayList<>();
        for (VehicleCubeOBB.CubePoint point : surfacePoints) {
            Vector3f worldPos = point.worldPos(axes);
            BlockPos blockPos = getPosFromShips(level, new Vector3d(worldPos));
            if (blockPos != null) {
                point.cubePointContext.setBlockPos(Vec3.atBottomCenterOf(blockPos));
                point.cubePointContext.setBlockState(level.getBlockState(blockPos));
                touchPoints.add(point);
            }
        }
        event.getTouchPoints().addAll(touchPoints);
    }

    private static BlockPos getPosFromShips(Level level, final Vector3dc blockPosInGlobal) {
        final double radius = 0.5;
        final AABBdc testAABB = new AABBd(
                blockPosInGlobal.x() - radius, blockPosInGlobal.y() - radius, blockPosInGlobal.z() - radius,
                blockPosInGlobal.x() + radius, blockPosInGlobal.y() + radius, blockPosInGlobal.z() + radius
        );
        final Iterable<Ship> intersectingShips = VSGameUtilsKt.getShipsIntersecting(level, testAABB);
        for (final Ship ship : intersectingShips) {
            final Vector3dc blockPosInLocal =
                    ship.getTransform().getWorldToShip().transformPosition(blockPosInGlobal, new Vector3d());
            final BlockPos blockPos = BlockPos.containing(
                    blockPosInLocal.x(), blockPosInLocal.y(), blockPosInLocal.z()
            );
            final BlockState blockState = level.getBlockState(blockPos);
            if (!blockState.isAir()) {
                return blockPos;
            }
        }
        return null;
    }

}
