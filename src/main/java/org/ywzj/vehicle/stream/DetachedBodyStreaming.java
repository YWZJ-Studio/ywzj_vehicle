package org.ywzj.vehicle.stream;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.ywzj.vehicle.api.entity.DetachedBodyVehicle;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DetachedBodyStreaming {

    private static volatile Map<UUID, LongOpenHashSet> snapshot = Collections.emptyMap();
    private static volatile Set<UUID> piloting = Collections.emptySet();

    private DetachedBodyStreaming() {
    }

    public static void publish(Map<UUID, LongOpenHashSet> next) {
        snapshot = next;
    }

    public static void publishPilots(Set<UUID> next) {
        piloting = next;
    }

    public static void reset() {
        snapshot = Collections.emptyMap();
        piloting = Collections.emptySet();
    }

    public static boolean isPiloting(UUID playerId) {
        return playerId != null && piloting.contains(playerId);
    }

    public static boolean isChunkStreamedTo(UUID playerId, int chunkX, int chunkZ) {
        if (playerId == null) {
            return false;
        }
        LongOpenHashSet set = snapshot.get(playerId);
        return set != null && set.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static boolean isDetached(Entity entity) {
        return entity instanceof DetachedBodyVehicle vehicle && vehicle.isDetachedBodyActive();
    }

    public static boolean mustStayPaired(Entity entity, Player player) {
        if (entity == null || entity == player) {
            return true;
        }
        if (entity.getVehicle() == player) {
            return true;
        }
        Entity vehicle = player.getVehicle();
        return vehicle != null && entity.getRootVehicle() == vehicle.getRootVehicle();
    }

    public static boolean shouldReveal(Entity entity, Player player) {
        if (entity == null || player == null) {
            return false;
        }
        Entity ridden = player.getVehicle();
        if (ridden != null && entity.getRootVehicle() == ridden.getRootVehicle()) {
            return true;
        }
        Map<UUID, LongOpenHashSet> snap = snapshot;
        if (snap.isEmpty()) {
            return false;
        }
        LongOpenHashSet set = snap.get(player.getUUID());
        if (set == null) {
            return false;
        }
        ChunkPos pos = entity.chunkPosition();
        if (!set.contains(ChunkPos.asLong(pos.x, pos.z))) {
            return false;
        }
        if (ChunkStreamDebug.on(ChunkStreamDebug.Category.ENTITY)) {
            ChunkStreamDebug.Session session = ChunkStreamDebug.peek(player.getUUID());
            if (session != null) {
                session.entityReveals++;
            }
        }
        return true;
    }

}
