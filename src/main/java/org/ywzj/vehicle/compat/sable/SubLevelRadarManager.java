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

import java.util.*;

public class SubLevelRadarManager {

    private static SubLevelRadarManager INSTANCE;
    private final Map<Level, ContainerState> containers = new HashMap<>();
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
        ContainerState state = containers.computeIfAbsent(level, ContainerState::new);
        for (SubLevel subLevel : container.getAllSubLevels()) {
            state.queueAdd(subLevel);
        }
        container.addObserver(new SubLevelObserver() {

            @Override
            public void onSubLevelAdded(SubLevel subLevel) {
                state.queueAdd(subLevel);
            }

            @Override
            public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
                state.queueRemove(subLevel.getUniqueId());
            }

            @Override
            public void tick(SubLevelContainer subLevels) {
                state.flush();
                for (SubLevel subLevel : subLevels.getAllSubLevels()) {
                    state.updateMarker(subLevel);
                }
            }

        });
    }

    private static final class ContainerState {

        private final Level level;
        private final Map<UUID, RadarMarkerEntity> markers = new HashMap<>();
        private final List<SubLevel> pendingAdds = new ArrayList<>();
        private final List<UUID> pendingRemoves = new ArrayList<>();

        private ContainerState(Level level) {
            this.level = level;
        }

        private void queueAdd(SubLevel subLevel) {
            if (!(level instanceof ServerLevel)) {
                return;
            }
            if (subLevel.isRemoved() || markers.containsKey(subLevel.getUniqueId())) {
                return;
            }
            for (SubLevel pending : pendingAdds) {
                if (pending.getUniqueId().equals(subLevel.getUniqueId())) {
                    return;
                }
            }
            pendingAdds.add(subLevel);
        }

        private void queueRemove(UUID uuid) {
            pendingAdds.removeIf(subLevel -> subLevel.getUniqueId().equals(uuid));
            if (!(level instanceof ServerLevel)) {
                return;
            }
            if (!pendingRemoves.contains(uuid)) {
                pendingRemoves.add(uuid);
            }
        }

        private void flush() {
            if (!(level instanceof ServerLevel serverLevel)) {
                pendingAdds.clear();
                pendingRemoves.clear();
                return;
            }
            for (UUID uuid : pendingRemoves) {
                RadarMarkerEntity marker = markers.remove(uuid);
                if (marker != null && !marker.isRemoved()) {
                    marker.discard();
                }
            }
            pendingRemoves.clear();
            for (SubLevel subLevel : pendingAdds) {
                if (subLevel.isRemoved() || markers.containsKey(subLevel.getUniqueId())) {
                    continue;
                }
                RadarMarkerEntity marker = new RadarMarkerEntity(AllEntities.RADAR_MARKER.get(), serverLevel);
                serverLevel.addFreshEntity(marker);
                markers.put(subLevel.getUniqueId(), marker);
            }
            pendingAdds.clear();
        }

        private void updateMarker(SubLevel subLevel) {
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

}
