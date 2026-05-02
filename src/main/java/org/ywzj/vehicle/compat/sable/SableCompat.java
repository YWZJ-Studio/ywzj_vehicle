package org.ywzj.vehicle.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.event.VehicleCollectCollisionEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber
public class SableCompat {

    private static final String MOD_ID = "sable";
    private static boolean IS_LOADED = false;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID);
        if (IS_LOADED) {
            SubLevelRadarManager.init();
        }
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    @SubscribeEvent
    public static void onVehicleCollectCollision(VehicleCollectCollisionEvent event) {
        if (isLoaded()) {
            SableImplementation.collideVehicle(event);
        }
    }

    public static Vec3 transformInverse(Level level, Vec3 checkPos, Vec3 worldPos) {
        return SableImplementation.transformInverse(level, checkPos, worldPos);
    }

    public static Vec3 transform(Level level, Vec3 localPos) {
        return SableImplementation.transform(level, localPos);
    }

}

class SableImplementation {

    protected static void collideVehicle(VehicleCollectCollisionEvent event) {
        AbstractVehicle vehicle = event.getVehicle();
        Level level = vehicle.level();
        VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
        Vector3f[] axes = mainCubeOBB.obb().getAxes();
        List<VehicleCubeOBB.CubePoint> surfacePoints = mainCubeOBB.cubePoints();
        List<VehicleCubeOBB.CubePoint> touchPoints = new ArrayList<>();
        SubLevel trackedSubLevel = null;
        for (VehicleCubeOBB.CubePoint point : surfacePoints) {
            Vec3 worldPos = new Vec3(point.worldPos(axes));
            BoundingBox3d queryBounds = new BoundingBox3d(
                    worldPos.x - 0.001, worldPos.y - 0.001, worldPos.z - 0.001,
                    worldPos.x + 0.001, worldPos.y + 0.001, worldPos.z + 0.001
            );
            for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, queryBounds)) {
                Vec3 localPos = subLevel.logicalPose().transformPositionInverse(worldPos);
                BlockPos blockPos = BlockPos.containing(localPos);
                BlockState state = level.getBlockState(blockPos);
                if (!state.isAir() && !state.getCollisionShape(level, blockPos).isEmpty()) {
                    point.cubePointContext.setBlockPos(worldPos.add(0, -1, 0));
                    point.cubePointContext.setBlockState(state);
                    touchPoints.add(point);
                    if (trackedSubLevel == null) {
                        trackedSubLevel = subLevel;
                    }
                }
            }
        }
        if (trackedSubLevel != null) {
            // 伴随旋转
            Quaterniondc lastOrientation = trackedSubLevel.lastPose().orientation();
            Quaterniondc currentOrientation = trackedSubLevel.logicalPose().orientation();
            Quaterniond relativeOrientation = new Quaterniond();
            currentOrientation.div(lastOrientation, relativeOrientation);
            final double angleDiff = 2 * relativeOrientation.y / relativeOrientation.w;
            final float delta = (float) Math.toDegrees(angleDiff);
            vehicle.setYRot(vehicle.getYRot() - delta);
            // 伴随移动
            LivingEntityMovementExtension livingExt = (LivingEntityMovementExtension) vehicle;
            Vec3 velocity = JOMLConversion.toMojang(livingExt.sable$getInheritedVelocity());
            EntityMovementExtension entityMovementExtension = (EntityMovementExtension) vehicle;
            SubLevelEntityCollision.CollisionInfo collisionInfo = SubLevelEntityCollision.collide(vehicle, Vec3.ZERO, velocity, ((LevelExtension) vehicle.level()).sable$getJOMLSink());
            collisionInfo.preTrackingSubLevel = entityMovementExtension.sable$getTrackingSubLevel();
            collisionInfo.preDeltaMovement = vehicle.getDeltaMovement();
            if (collisionInfo.trackingSubLevel != null) {
                if (collisionInfo.verticalCollisionBelow) {
                    entityMovementExtension.sable$setTrackingSubLevel(collisionInfo.trackingSubLevel);
                }
            } else {
                entityMovementExtension.sable$setTrackingSubLevel(null);
            }

            if (collisionInfo.inheritedMotion != null && collisionInfo.inheritedMotion.lengthSqr() > 1.0E-12) {
                vehicle.setPos(vehicle.position().add(collisionInfo.inheritedMotion));
                livingExt.sable$getInheritedVelocity()
                        .set(collisionInfo.inheritedMotion.x, collisionInfo.inheritedMotion.y, collisionInfo.inheritedMotion.z);
            }
            // 发包强制刷新客户端侧载具的位置与旋转
            Vec3 vec31 = vehicle.trackingPosition();
            VecDeltaCodec positionCodec = new VecDeltaCodec();
            positionCodec.setBase(vehicle.trackingPosition());
            ClientboundMoveEntityPacket.PosRot packet = new ClientboundMoveEntityPacket.PosRot(vehicle.getId(),
                    (short) ((int) positionCodec.encodeX(vec31)),
                    (short) ((int) positionCodec.encodeY(vec31)),
                    (short) ((int) positionCodec.encodeZ(vec31)),
                    (byte) 0, (byte) 0, vehicle.onGround());
            ((ServerLevel) vehicle.level()).getChunkSource().broadcast(vehicle, packet);
        }
        event.getTouchPoints().addAll(touchPoints);
    }

    protected static Vec3 transformInverse(Level level, Vec3 checkPos, Vec3 worldPos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, checkPos);
        if (subLevel != null) {
            return subLevel.logicalPose().transformPositionInverse(worldPos);
        }
        BoundingBox3d queryBounds = new BoundingBox3d(
                checkPos.x - 0.001, checkPos.y - 0.001, checkPos.z - 0.001,
                checkPos.x + 0.001, checkPos.y + 0.001, checkPos.z + 0.001
        );
        for (SubLevel querySubLevel : Sable.HELPER.getAllIntersecting(level, queryBounds)) {
            return querySubLevel.logicalPose().transformPositionInverse(worldPos);
        }
        return worldPos;
    }

    protected static Vec3 transform(Level level, Vec3 localPos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, localPos);
        if (subLevel != null) {
            return subLevel.logicalPose().transformPosition(localPos);
        }
        return localPos;
    }

}
