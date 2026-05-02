package org.ywzj.vehicle.compat.sable;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.entity.misc.RadarMarkerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SubLevelRadarManager {

    private static SubLevelRadarManager INSTANCE;

    private final Map<UUID, RadarMarkerEntity> markers = new HashMap<>();
    private boolean initialized = false;

    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new SubLevelRadarManager();
        }
        INSTANCE.doInit();
    }

    private void doInit() {
        if (initialized) return;
        initialized = true;
        SableEventPlatform.INSTANCE.onSubLevelContainerReady(this::onContainerReady);
    }

    private void onContainerReady(Level level, SubLevelContainer container) {
        for (SubLevel subLevel : container.getAllSubLevels()) {
            addMarker(level, subLevel);
        }
        container.addObserver(new SubLevelObserver() {

            @Override
            public void onSubLevelAdded(SubLevel subLevel) {
                addMarker(level, subLevel);
            }

            @Override
            public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
                removeMarker(subLevel.getUniqueId());
            }

            @Override
            public void tick(SubLevelContainer subLevels) {
                for (SubLevel subLevel : subLevels.getAllSubLevels()) {
                    RadarMarkerEntity marker = markers.get(subLevel.getUniqueId());
                    if (marker != null && !marker.isRemoved()) {
                        BoundingBox3dc box = subLevel.boundingBox();
                        AABB aabb = new AABB(box.minX(), box.minY(), box.minZ(),
                                box.maxX(), box.maxY(), box.maxZ());
                        marker.setPos(aabb.getCenter());
                        Vector3d step = new Vector3d(subLevel.logicalPose().position()).sub(subLevel.lastPose().position());
                        marker.setDeltaMovement(new Vec3(step.x, step.y, step.z));
                    }
                }
            }

        });
    }

    private void addMarker(Level level, SubLevel subLevel) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID uuid = subLevel.getUniqueId();
        if (markers.containsKey(uuid)) {
            return;
        }
        RadarMarkerEntity marker = new RadarMarkerEntity(AllEntities.RADAR_MARKER.get(), level);
        serverLevel.addFreshEntity(marker);
        markers.put(uuid, marker);
    }

    private void removeMarker(UUID uuid) {
        RadarMarkerEntity marker = markers.remove(uuid);
        if (marker != null) {
            marker.discard();
        }
    }

}
