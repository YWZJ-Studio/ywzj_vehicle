package org.ywzj.vehicle.stream;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.entity.DetachedBodyVehicle;
import org.ywzj.vehicle.mixin.common.ChunkMapAccessor;
import org.ywzj.vehicle.stream.wakeup.VehicleWakeup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public final class DetachedBodyStreamer {

    public static final DetachedBodyStreamer INSTANCE = new DetachedBodyStreamer();

    private static final Comparator<Unit> UNIT_COMPARATOR = (a, b) -> 0;
    private static final TicketType<Unit> STREAM_TICKET = TicketType.create("ywzj_detached_stream", UNIT_COMPARATOR, 40);
    private static final TicketType<Unit> BODY_TICKET = TicketType.create("ywzj_detached_body", UNIT_COMPARATOR, 40);
    private static final LongOpenHashSet EMPTY_CHUNKS = new LongOpenHashSet();
    private static final int PIN_REFRESH_TICKS = 200;

    private final Set<Entity> trackedVehicles = new HashSet<>();
    private final Map<UUID, LongOpenHashSet> subscribed = new HashMap<>();
    private final Map<UUID, LongOpenHashSet> pinnedSent = new HashMap<>();
    private final Map<UUID, LongOpenHashSet> displacedBody = new HashMap<>();
    private final Map<UUID, ServerPlayer> viewing = new HashMap<>();
    private final Map<UUID, Entity> viewVehicle = new HashMap<>();
    private final Map<UUID, ChunkPos> viewChunk = new HashMap<>();
    private final Map<UUID, ServerLevel> viewLevel = new HashMap<>();
    private final Map<UUID, Long> lastCenter = new HashMap<>();
    private final Map<UUID, Long> lastBodyChunk = new HashMap<>();
    private final Set<UUID> workUuids = new HashSet<>();
    private long ticks;

    private DetachedBodyStreamer() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        INSTANCE.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof DetachedBodyVehicle) {
            INSTANCE.trackedVehicles.add(event.getEntity());
            VehicleWakeup.recordPosition(event.getEntity());
            ChunkStreamDebug.log(ChunkStreamDebug.Category.SESSION, "tracking detached-capable vehicle {} @ {}",
                    ChunkStreamDebug.shortId(event.getEntity().getUUID()), event.getEntity().chunkPosition());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof DetachedBodyVehicle) {
            INSTANCE.trackedVehicles.remove(entity);
            VehicleWakeup.recordSleep(entity);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INSTANCE.clearOperator(player, "logout");
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INSTANCE.disengage(player, "death");
        }
    }

    public static void onOperatorDetached(ServerPlayer player, String reason) {
        INSTANCE.disengage(player, reason);
    }

    private void tick(MinecraftServer server) {
        this.ticks++;
        if (this.ticks % 20 == 0) {
            ChunkStreamDebug.refresh();
        }
        if (this.trackedVehicles.isEmpty() && this.subscribed.isEmpty()) {
            return;
        }
        boolean heartbeat = this.ticks % ChunkStreamDebug.heartbeatTicks() == 0;
        int viewDistance = server.getPlayerList().getViewDistance();
        int radius = Math.min(AllConfigs.server.detachedStreamRadius.get(), viewDistance);
        int pinRadius = AllConfigs.server.detachedBodyPinRadius.get();
        int budgetPerTick = AllConfigs.server.detachedMaxChunksPerTick.get();

        this.viewing.clear();
        this.viewVehicle.clear();
        this.viewChunk.clear();
        this.viewLevel.clear();

        collectOperators(radius);

        DetachedBodyStreaming.publishPilots(this.viewing.isEmpty()
                ? Collections.emptySet() : new HashSet<>(this.viewing.keySet()));

        this.workUuids.clear();
        this.workUuids.addAll(this.subscribed.keySet());
        this.workUuids.addAll(this.viewing.keySet());

        Map<UUID, LongOpenHashSet> snapshot = new HashMap<>();
        List<ServerPlayer> needScan = null;
        List<ServerPlayer> restored = null;

        for (UUID id : this.workUuids) {
            ServerPlayer operator = this.viewing.get(id);
            if (operator == null) {
                operator = server.getPlayerList().getPlayer(id);
            }
            if (operator == null) {
                dropState(id);
                continue;
            }

            if (this.viewing.containsKey(id)) {
                if (stream(operator, radius, pinRadius, budgetPerTick, viewDistance, snapshot, heartbeat)) {
                    if (needScan == null) {
                        needScan = new ArrayList<>();
                    }
                    needScan.add(operator);
                }
            } else {
                releaseToBody(operator, "stopped-viewing");
                if (restored == null) {
                    restored = new ArrayList<>();
                }
                restored.add(operator);
            }
        }

        DetachedBodyStreaming.publish(snapshot);

        if (needScan != null) {
            for (ServerPlayer operator : needScan) {
                reevaluateVisibility(this.viewLevel.get(operator.getUUID()), operator);
            }
        }
        if (restored != null) {
            for (ServerPlayer operator : restored) {
                if (operator.level() instanceof ServerLevel level) {
                    reevaluateVisibility(level, operator);
                }
            }
        }

        if (this.ticks % 40 == 0) {
            for (Entity vehicle : this.trackedVehicles) {
                VehicleWakeup.recordPosition(vehicle);
            }
        }
    }

    private void collectOperators(int radius) {
        for (Entity vehicle : this.trackedVehicles) {
            if (!(vehicle.level() instanceof ServerLevel level)) {
                continue;
            }
            DetachedBodyVehicle detached = (DetachedBodyVehicle) vehicle;
            String vehicleTag = "vehicle " + ChunkStreamDebug.shortId(vehicle.getUUID());
            if (!detached.isDetachedBodyActive()) {
                ChunkStreamDebug.state(ChunkStreamDebug.Category.SESSION, vehicleTag, "idle (no detached operator)");
                continue;
            }
            for (Entity passenger : vehicle.getPassengers()) {
                if (!(passenger instanceof ServerPlayer operator)) {
                    continue;
                }
                String tag = vehicleTag + "/" + operator.getScoreboardName();
                Vec3 anchor = detached.getDetachedBodyAnchor(operator);
                if (anchor == null) {
                    ChunkStreamDebug.state(ChunkStreamDebug.Category.SESSION, tag, "riding without a body anchor");
                    continue;
                }
                if (operator.level() != level) {
                    ChunkStreamDebug.state(ChunkStreamDebug.Category.SESSION, tag, "body is in another dimension");
                    continue;
                }
                ChunkStreamDebug.state(ChunkStreamDebug.Category.SESSION, tag,
                        "streaming vehicle chunk " + vehicle.chunkPosition() + " radius " + radius);
                UUID id = operator.getUUID();
                this.viewing.put(id, operator);
                this.viewVehicle.put(id, vehicle);
                this.viewChunk.put(id, vehicle.chunkPosition());
                this.viewLevel.put(id, level);
            }
        }
    }

    private boolean stream(ServerPlayer operator, int radius, int pinRadius, int budgetPerTick,
                           int viewDistance, Map<UUID, LongOpenHashSet> snapshot, boolean heartbeat) {
        UUID id = operator.getUUID();
        ServerLevel level = this.viewLevel.get(id);
        ChunkPos target = this.viewChunk.get(id);
        ChunkPos body = operator.chunkPosition();
        ChunkStreamDebug.Session session = ChunkStreamDebug.session(id, operator.getScoreboardName());
        session.vehicle = ChunkStreamDebug.shortId(this.viewVehicle.get(id).getUUID());
        session.center = target;
        session.radius = radius;

        long targetKey = ChunkPos.asLong(target.x, target.z);
        long bodyKey = ChunkPos.asLong(body.x, body.z);
        Long previousCenter = this.lastCenter.get(id);
        Long previousBody = this.lastBodyChunk.put(id, bodyKey);
        boolean firstView = !this.subscribed.containsKey(id);
        boolean moved = previousCenter == null || previousCenter != targetKey;
        boolean bodyMoved = previousBody == null || previousBody != bodyKey;

        if (moved || this.ticks % 20 == 0) {
            level.getChunkSource().addRegionTicket(STREAM_TICKET, target, radius, Unit.INSTANCE);
            this.lastCenter.put(id, targetKey);
            session.ticketsIssued++;
            ChunkStreamDebug.log(ChunkStreamDebug.Category.TICKET, "{}: region ticket {} radius {}",
                    operator.getScoreboardName(), target, radius);
        }
        if (moved || bodyMoved || firstView || this.ticks % 20 == 0) {
            operator.connection.send(new ClientboundSetChunkCacheCenterPacket(target.x, target.z));
            session.centerUpdates++;
            ChunkStreamDebug.log(ChunkStreamDebug.Category.CENTER, "{}: cache center -> {}",
                    operator.getScoreboardName(), target);
        }
        if (firstView || bodyMoved || this.ticks % 20 == 0) {
            parkBody(operator, level, body, firstView || bodyMoved);
        }
        if (firstView) {
                ChunkStreamDebug.log(ChunkStreamDebug.Category.SESSION,
                    "{}: view handed to vehicle {} (body stays at chunk {})",
                    operator.getScoreboardName(), target, body);
        }

        int storageRadius = clientStorageRadius(viewDistance);
        int viewRange = storageRadius * 2 + 1;
        LongOpenHashSet have = this.subscribed.getOrDefault(id, EMPTY_CHUNKS);
        LongOpenHashSet next = new LongOpenHashSet();
        int budget = budgetPerTick;
        int sent = 0;
        int missing = 0;
        int clobbered = 0;
        int displaced = 0;
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int cx = target.x + dx;
                    int cz = target.z + dz;
                    long pos = ChunkPos.asLong(cx, cz);
                    if (have.contains(pos)) {
                        next.add(pos);
                        continue;
                    }
                    if (budget <= 0) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        missing++;
                        continue;
                    }
                    sendChunk(operator, level, chunk);
                    sent++;
                    budget--;
                    next.add(pos);

                    // Fold this chunk onto the body's storage square to find the body-side chunk sharing
                    // its client cache slot; that chunk has just been evicted client-side, which
                    // renderers observe as an unload with no matching load.
                    int wrapX = Math.floorMod(cx - body.x + storageRadius, viewRange) - storageRadius;
                    int wrapZ = Math.floorMod(cz - body.z + storageRadius, viewRange) - storageRadius;
                    if (body.x + wrapX == cx && body.z + wrapZ == cz) {
                        continue;
                    }
                    displaced++;
                    long lost = ChunkPos.asLong(body.x + wrapX, body.z + wrapZ);
                    this.displacedBody.computeIfAbsent(id, key -> new LongOpenHashSet()).add(lost);
                    if (Math.abs(wrapX) <= pinRadius && Math.abs(wrapZ) <= pinRadius) {
                        LongOpenHashSet held = this.pinnedSent.get(id);
                        if (held != null && held.remove(lost)) {
                            clobbered++;
                        }
                    }
                }
            }
        }
        session.bodyChunksDisplaced += displaced;
        if (clobbered > 0) {
            ChunkStreamDebug.log(ChunkStreamDebug.Category.PIN,
                    "{}: {} body pin(s) displaced in the client cache, re-sending",
                    operator.getScoreboardName(), clobbered);
        }
        if (displaced > 0) {
            ChunkStreamDebug.log(ChunkStreamDebug.Category.CHUNK,
                    "{}: {} vehicle chunk(s) displaced a body-side chunk in the client cache",
                    operator.getScoreboardName(), displaced);
        }
        this.subscribed.put(id, next);
        session.chunksSent += sent;
        session.chunkMisses += missing;
        session.liveChunks = next.size();
        if (sent > 0) {
            ChunkStreamDebug.log(ChunkStreamDebug.Category.CHUNK, "{}: sent {} chunk(s) around {}, {} not loaded yet, {} live",
                    operator.getScoreboardName(), sent, target, missing, next.size());
        }
        if (missing > 0 && budget > 0) {
            ChunkStreamDebug.state(ChunkStreamDebug.Category.CHUNK, "gap " + operator.getScoreboardName(),
                    missing + " chunk(s) around " + target + " still generating");
        }

        LongOpenHashSet reveal = new LongOpenHashSet(next);
        pinBodyChunks(operator, level, body, target, pinRadius, viewDistance, reveal, session);
        if (!reveal.isEmpty()) {
            snapshot.put(id, reveal);
        }

        if (heartbeat) {
            session.heartbeat();
        }
        return moved || bodyMoved || firstView || sent > 0;
    }

    /**
     * The client keeps its chunk cache in a ring buffer indexed modulo the storage diameter around
     * the cache center. With the center parked on the vehicle, chunks around the operator's body land
     * on the same slots as vehicle chunks and are evicted, so body chunks outside that window are sent
     * separately and held in the client's side map (see ClientChunkCacheMixin). A pin is re-sent
     * whenever its slot was taken over, and on a slow cadence as a backstop, because an eviction the
     * server does not notice would otherwise leave a permanent hole next to the operator.
     */
    private void pinBodyChunks(ServerPlayer operator, ServerLevel level, ChunkPos body, ChunkPos target,
                               int pinRadius, int viewDistance, LongOpenHashSet reveal,
                               ChunkStreamDebug.Session session) {
        UUID id = operator.getUUID();
        int clientRadius = clientStorageRadius(viewDistance);
        boolean refresh = this.ticks % PIN_REFRESH_TICKS == 0;
        LongOpenHashSet previous = this.pinnedSent.getOrDefault(id, EMPTY_CHUNKS);
        LongOpenHashSet fresh = new LongOpenHashSet();
        for (int dx = -pinRadius; dx <= pinRadius; dx++) {
            for (int dz = -pinRadius; dz <= pinRadius; dz++) {
                int cx = body.x + dx;
                int cz = body.z + dz;
                long pos = ChunkPos.asLong(cx, cz);
                reveal.add(pos);
                if (Math.abs(cx - target.x) <= clientRadius && Math.abs(cz - target.z) <= clientRadius) {
                    continue;
                }
                fresh.add(pos);
                if (previous.contains(pos) && !refresh) {
                    continue;
                }
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    fresh.remove(pos);
                    ChunkStreamDebug.log(ChunkStreamDebug.Category.PIN, "{}: body chunk {} {} not loaded, retrying",
                            operator.getScoreboardName(), cx, cz);
                    continue;
                }
                sendChunk(operator, level, chunk);
                session.pinsSent++;
                ChunkStreamDebug.log(ChunkStreamDebug.Category.PIN, "{}: pinned body chunk {} {}",
                        operator.getScoreboardName(), cx, cz);
            }
        }
        int dropped = 0;
        for (long pos : previous) {
            if (!fresh.contains(pos)) {
                dropped++;
            }
        }
        if (dropped > 0) {
            session.pinsDropped += dropped;
            ChunkStreamDebug.log(ChunkStreamDebug.Category.PIN, "{}: released {} stale body pin(s)",
                    operator.getScoreboardName(), dropped);
        }
        if (fresh.isEmpty()) {
            this.pinnedSent.remove(id);
        } else {
            this.pinnedSent.put(id, fresh);
        }
    }

    /**
     * Streaming sends chunks straight down the connection, so ChunkMap's record of what the operator
     * has is never touched and is wrong in both directions by the time the view comes back: it still
     * lists body chunks the client evicted from its ring buffer, and knows nothing of the vehicle
     * chunks. Anything it wrongly believes was delivered is never sent again, which leaves holes that
     * survive walking away and coming back. Rather than guessing which chunks were lost, the record
     * is emptied and vanilla is asked to re-track from scratch exactly as it does on join; that also
     * routes every chunk back through the packet path renderers hook, which a direct re-send does not.
     */
    private void releaseToBody(ServerPlayer operator, String reason) {
        UUID id = operator.getUUID();
        DetachedBodyStreaming.clearPilot(id);
        LongOpenHashSet streamed = this.subscribed.remove(id);
        this.lastCenter.remove(id);
        this.pinnedSent.remove(id);
        this.lastBodyChunk.remove(id);
        this.displacedBody.remove(id);
        ChunkStreamDebug.Session session = ChunkStreamDebug.session(id, operator.getScoreboardName());
        if (streamed != null) {
            for (long pos : streamed) {
                forget(operator, ChunkPos.getX(pos), ChunkPos.getZ(pos));
            }
            session.chunksForgotten += streamed.size();
        }
        if (!(operator.level() instanceof ServerLevel level)) {
            ChunkStreamDebug.endSession(id, reason);
            return;
        }
        ChunkPos body = operator.chunkPosition();
        ChunkMapAccessor chunkMap = (ChunkMapAccessor) (Object) level.getChunkSource().chunkMap;
        level.getChunkSource().move(operator);
        operator.setChunkTrackingView(ChunkTrackingView.EMPTY);
        chunkMap.ywzj$updateChunkTracking(operator);
        session.centerUpdates++;
        ChunkStreamDebug.log(ChunkStreamDebug.Category.SESSION,
                "{}: view handed back to body at chunk {} ({}), forgot {} vehicle chunk(s), re-tracking radius {}",
                operator.getScoreboardName(), body, reason,
                streamed == null ? 0 : streamed.size(), chunkMap.ywzj$playerViewDistance(operator));
        ChunkStreamDebug.endSession(id, reason);
    }


    private void parkBody(ServerPlayer operator, ServerLevel level, ChunkPos body, boolean apply) {
        int ticketRadius = AllConfigs.server.detachedBodyTicketRadius.get();
        if (ticketRadius < 0) {
            return;
        }
        level.getChunkSource().addRegionTicket(BODY_TICKET, body, ticketRadius, Unit.INSTANCE);
        if (!apply) {
            return;
        }
        level.getChunkSource().move(operator);
        ChunkStreamDebug.state(ChunkStreamDebug.Category.TICKET, "body " + operator.getScoreboardName(),
                "parked at " + body + " on a radius " + ticketRadius + " ticket");
    }

    private static int clientStorageRadius(int viewDistance) {
        return Math.max(2, viewDistance) + 3;
    }

    private static void forget(ServerPlayer operator, int chunkX, int chunkZ) {
        operator.connection.send(new ClientboundForgetLevelChunkPacket(new ChunkPos(chunkX, chunkZ)));
    }

    private void disengage(ServerPlayer operator, String reason) {
        if (!this.subscribed.containsKey(operator.getUUID())) {
            clearOperator(operator, reason);
            return;
        }
        releaseToBody(operator, reason);
        if (operator.level() instanceof ServerLevel level) {
            reevaluateVisibility(level, operator);
        }
    }

    private void clearOperator(ServerPlayer operator, String reason) {
        dropState(operator.getUUID());
        ChunkStreamDebug.endSession(operator.getUUID(), reason);
    }

    private void dropState(UUID id) {
        this.subscribed.remove(id);
        this.lastCenter.remove(id);
        this.pinnedSent.remove(id);
        this.lastBodyChunk.remove(id);
        this.displacedBody.remove(id);
    }

    /**
     * Entity visibility is normally only recomputed when an entity or player moves; neither happens
     * for a parked body, so every tracker is asked to re-test this operator after the streamed chunk
     * set changes.
     */
    private void reevaluateVisibility(ServerLevel level, ServerPlayer operator) {
        if (level == null) {
            return;
        }
        ChunkMapAccessor accessor = (ChunkMapAccessor) (Object) level.getChunkSource().chunkMap;
        for (Object tracked : accessor.ywzj$entityMap().values()) {
            ((DetachedTracked) tracked).ywzj$updatePlayer(operator);
        }
        ChunkStreamDebug.Session session = ChunkStreamDebug.peek(operator.getUUID());
        if (session != null) {
            session.visibilityScans++;
        }
    }

    private static void sendChunk(ServerPlayer operator, ServerLevel level, LevelChunk chunk) {
        operator.connection.send(chunk.getAuxLightManager(chunk.getPos()).sendLightDataTo(
                new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null)));
    }

}
