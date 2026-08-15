package org.ywzj.vehicle.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.collision.CollisionProvider;
import org.ywzj.vehicle.api.collision.CollisionProviders;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.collision.ChunkCollisionCache;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.ArrayList;
import java.util.List;

public class SableCompat {

    private static final String MOD_ID = "sable";
    private static boolean IS_LOADED = false;

    public static void init() {
        IS_LOADED = ModList.get().isLoaded(MOD_ID);
        if (IS_LOADED) {
            SubLevelRadarManager.init();
            // Only touched when Sable is present, so its classes are never resolved otherwise.
            CollisionProviders.register(new SableCollisionProvider());
        }
    }

    public static boolean isLoaded() {
        return IS_LOADED;
    }

    public static Vec3 transformInverse(Level level, Vec3 checkPos, Vec3 worldPos) {
        return SableImplementation.transformInverse(level, checkPos, worldPos);
    }

    public static Vec3 transform(Level level, Vec3 localPos) {
        return SableImplementation.transform(level, localPos);
    }

}

/**
 * Contributes contacts against Sable sub-level blocks, and carries the vehicle along with the
 * sub-level it is resting on.
 * <p>
 * Was a {@code VehicleCollectCollisionEvent} listener that re-walked every hull sample point and
 * re-transformed it into world space. It now shares the core sampling pass. The tracked sub-level
 * lives in the session rather than the provider, so several vehicles can be sampled at once.
 */
class SableCollisionProvider implements CollisionProvider {

    @Nullable
    @Override
    public Session begin(AbstractVehicle vehicle, AABB hullBounds) {
        Level level = vehicle.level();
        BoundingBox3d bounds = new BoundingBox3d(
                hullBounds.minX, hullBounds.minY, hullBounds.minZ,
                hullBounds.maxX, hullBounds.maxY, hullBounds.maxZ);
        List<SableSession.Region> regions = new ArrayList<>();
        ChunkCollisionCache cache = ChunkCollisionCache.of(level);
        for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, bounds)) {
            // A sub-level's blocks live in the host level at its plot coordinates, so pulling the
            // hull's bound back through the pose gives the region of ordinary blocks to look at —
            // and the ordinary block snapshot cache can then answer for them.
            AABB localBounds = transformBounds(subLevel.logicalPose(), hullBounds, true);
            // begin() runs on the tick thread, which is the only place chunk data may be read.
            // Everything the session does afterwards reads finished snapshots.
            cache.prepare(level, localBounds);
            regions.add(new SableSession.Region(subLevel, localBounds));
        }
        // No sub-level anywhere near the hull: skip every point.
        if (regions.isEmpty()) {
            return null;
        }
        return new SableSession(vehicle, level, regions);
    }

    private static final class SableSession implements Session {

        record Region(SubLevel subLevel, AABB localBounds) {}

        private final AbstractVehicle vehicle;
        private final Level level;
        private final List<Region> regions;
        private SubLevel trackedSubLevel;

        private SableSession(AbstractVehicle vehicle, Level level, List<Region> regions) {
            this.vehicle = vehicle;
            this.level = level;
            this.regions = regions;
        }

        /**
         * Merged block boxes from each sub-level's own region, pushed forward through its pose.
         * <p>
         * A rotated sub-level has no exact axis-aligned form, so each box is widened to its
         * bound — which is what {@link #contactAt} is for. For the flat decks these are usually
         * used as, the transform is a translation and the boxes come out exact.
         */
        @Override
        public boolean collectBoxes(AABB bounds, List<AABB> out) {
            ChunkCollisionCache cache = ChunkCollisionCache.of(level);
            List<AABB> localBoxes = new ArrayList<>();
            for (Region region : regions) {
                localBoxes.clear();
                cache.collectBoxes(region.localBounds(), localBoxes);
                Pose3dc pose = region.subLevel().logicalPose();
                for (int i = 0, size = localBoxes.size(); i < size; i++) {
                    AABB worldBox = transformBounds(pose, localBoxes.get(i), false);
                    if (worldBox.intersects(bounds)) {
                        out.add(worldBox);
                    }
                }
            }
            return true;
        }

        @Nullable
        @Override
        public Contact contactAt(VehicleCubeOBB.CubePoint point, Vector3f pointPos) {
            Vec3 worldPos = new Vec3(pointPos.x, pointPos.y, pointPos.z);
            for (int i = 0, size = regions.size(); i < size; i++) {
                SubLevel subLevel = regions.get(i).subLevel();
                // The pose transform is defined everywhere, so without this a distant sub-level
                // could map an unrelated point into its own blocks and claim a contact.
                if (!subLevel.boundingBox().contains(worldPos.x, worldPos.y, worldPos.z)) {
                    continue;
                }
                Vec3 localPos = subLevel.logicalPose().transformPositionInverse(worldPos);
                BlockPos blockPos = BlockPos.containing(localPos);
                BlockState state = level.getBlockState(blockPos);
                if (!state.isAir() && !state.getCollisionShape(level, blockPos).isEmpty()) {
                    if (trackedSubLevel == null) {
                        trackedSubLevel = subLevel;
                    }
                    return new Contact(worldPos.add(0, -1, 0), state);
                }
            }
            return null;
        }

        @Override
        public void end(List<VehicleCubeOBB.CubePoint> contacts) {
            if (trackedSubLevel != null) {
                SableImplementation.rideSubLevel(vehicle, trackedSubLevel);
            }
        }

    }

    /** Axis-aligned bound of a box's eight corners taken through a pose, either way round. */
    private static AABB transformBounds(Pose3dc pose, AABB box, boolean inverse) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            Vec3 point = new Vec3(
                    (corner & 1) == 0 ? box.minX : box.maxX,
                    (corner & 2) == 0 ? box.minY : box.maxY,
                    (corner & 4) == 0 ? box.minZ : box.maxZ);
            Vec3 mapped = inverse ? pose.transformPositionInverse(point) : pose.transformPosition(point);
            minX = Math.min(minX, mapped.x);
            minY = Math.min(minY, mapped.y);
            minZ = Math.min(minZ, mapped.z);
            maxX = Math.max(maxX, mapped.x);
            maxY = Math.max(maxY, mapped.y);
            maxZ = Math.max(maxZ, mapped.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

}

class SableImplementation {

    protected static void rideSubLevel(AbstractVehicle vehicle, SubLevel trackedSubLevel) {
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
