package org.ywzj.vehicle.stream.wakeup;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.entity.DetachedBodyVehicle;
import org.ywzj.vehicle.stream.ChunkStreamDebug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber
public final class VehicleWakeup {
    /* TODO: Perhaps keep the vehicle chunkloading if it still has velocity to prevent
     * it freezing in air, and only unload once it is settled.
     */


    public enum Result {
        LOADED,
        WAKING,
        UNKNOWN,
        OTHER_DIMENSION
    }

    @FunctionalInterface
    public interface WakeCallback {

        void onWake(ServerPlayer requester, Entity vehicle);

        default void onTimeout(ServerPlayer requester) {
        }

    }

    private static final Comparator<Unit> UNIT_COMPARATOR = (a, b) -> 0;
    private static final TicketType<Unit> WAKE_TICKET = TicketType.create("ywzj_vehicle_wakeup", UNIT_COMPARATOR, 200);
    private static final List<Pending> PENDING = new ArrayList<>();

    private VehicleWakeup() {
    }

    public static void recordPosition(Entity vehicle) {
        if (!(vehicle instanceof DetachedBodyVehicle) || !(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        VehicleWakeupData.get(level.getServer()).put(vehicle.getUUID(), level.dimension(),
                vehicle.position(), level.getGameTime());
    }

    public static void recordSleep(Entity vehicle) {
        if (!(vehicle instanceof DetachedBodyVehicle) || !(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        Entity.RemovalReason reason = vehicle.getRemovalReason();
        if (reason != null && !reason.shouldSave()) {
            VehicleWakeupData.get(level.getServer()).remove(vehicle.getUUID());
            ChunkStreamDebug.log(ChunkStreamDebug.Category.WAKEUP, "vehicle {} removed ({}), forgotten",
                    ChunkStreamDebug.shortId(vehicle.getUUID()), reason);
            return;
        }
        recordPosition(vehicle);
        ChunkStreamDebug.log(ChunkStreamDebug.Category.WAKEUP, "vehicle {} sleeping at {} in {}",
                ChunkStreamDebug.shortId(vehicle.getUUID()), vehicle.chunkPosition(), level.dimension().location());
    }

    @Nullable
    public static VehicleWakeupData.Entry lookup(MinecraftServer server, UUID vehicleId) {
        return VehicleWakeupData.get(server).get(vehicleId);
    }

    public static Result request(ServerPlayer requester, UUID vehicleId, WakeCallback callback) {
        MinecraftServer server = requester.server;
        VehicleWakeupData.Entry entry = lookup(server, vehicleId);
        if (entry == null) {
            ChunkStreamDebug.log(ChunkStreamDebug.Category.WAKEUP, "{} requested unknown vehicle {}",
                    requester.getScoreboardName(), ChunkStreamDebug.shortId(vehicleId));
            return Result.UNKNOWN;
        }
        if (entry.dimension() != requester.level().dimension()) {
            ChunkStreamDebug.log(ChunkStreamDebug.Category.WAKEUP, "{} requested vehicle {} in {}, but stands in {}",
                    requester.getScoreboardName(), ChunkStreamDebug.shortId(vehicleId),
                    entry.dimension().location(), requester.level().dimension().location());
            return Result.OTHER_DIMENSION;
        }
        ServerLevel level = server.getLevel(entry.dimension());
        if (level == null) {
            return Result.UNKNOWN;
        }
        Entity present = level.getEntity(vehicleId);
        if (present != null && isReady(present)) {
            callback.onWake(requester, present);
            return Result.LOADED;
        }
        for (Pending pending : PENDING) {
            if (pending.vehicleId.equals(vehicleId) && pending.requesterId.equals(requester.getUUID())) {
                return Result.WAKING;
            }
        }
        Pending pending = new Pending(vehicleId, requester.getUUID(), entry, callback,
                AllConfigs.server.vehicleWakeupTimeout.get());
        PENDING.add(pending);
        ticket(level, entry.chunk());
        ChunkStreamDebug.log(ChunkStreamDebug.Category.WAKEUP,
                "{} waking vehicle {} at chunk {} in {} (timeout {}t)",
                requester.getScoreboardName(), ChunkStreamDebug.shortId(vehicleId), entry.chunk(),
                entry.dimension().location(), pending.ticksLeft);
        return Result.WAKING;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            ServerPlayer requester = server.getPlayerList().getPlayer(pending.requesterId);
            ServerLevel level = server.getLevel(pending.entry.dimension());
            if (requester == null || level == null) {
                iterator.remove();
                continue;
            }
            Entity vehicle = level.getEntity(pending.vehicleId);
            if (vehicle != null && isReady(vehicle)) {
                iterator.remove();
                ChunkStreamDebug.log(ChunkStreamDebug.Category.WAKEUP, "vehicle {} woke at {} after {}t",
                        ChunkStreamDebug.shortId(pending.vehicleId), vehicle.chunkPosition(),
                        pending.waitedTicks());
                pending.callback.onWake(requester, vehicle);
                continue;
            }
            pending.ticksLeft--;
            if (pending.ticksLeft <= 0) {
                iterator.remove();
                pending.onTimeout(requester);
                continue;
            }
            if (pending.ticksLeft % 20 == 0) {
                ticket(level, pending.entry.chunk());
            }
        }
    }

    /**
     * A vehicle ejects everything it deserialised as a passenger on its first tick, so connecting
     * during that tick would mount the operator and immediately throw them back out.
     */
    private static boolean isReady(Entity vehicle) {
        return vehicle.tickCount > 1;
    }

    private static void ticket(ServerLevel level, ChunkPos pos) {
        level.getChunkSource().addRegionTicket(WAKE_TICKET, pos,
                AllConfigs.server.vehicleWakeupTicketRadius.get(), Unit.INSTANCE);
        ChunkStreamDebug.log(ChunkStreamDebug.Category.TICKET, "wakeup ticket at {} in {}",
                pos, level.dimension().location());
    }

    private static final class Pending {

        private final UUID vehicleId;
        private final UUID requesterId;
        private final VehicleWakeupData.Entry entry;
        private final WakeCallback callback;
        private final int timeout;
        private int ticksLeft;

        private Pending(UUID vehicleId, UUID requesterId, VehicleWakeupData.Entry entry,
                        WakeCallback callback, int timeout) {
            this.vehicleId = vehicleId;
            this.requesterId = requesterId;
            this.entry = entry;
            this.callback = callback;
            this.timeout = timeout;
            this.ticksLeft = timeout;
        }

        private int waitedTicks() {
            return this.timeout - this.ticksLeft;
        }

        private void onTimeout(ServerPlayer requester) {
            ChunkStreamDebug.warn(ChunkStreamDebug.Category.WAKEUP,
                    "vehicle {} did not load within {}t at chunk {} in {} (requested by {})",
                    ChunkStreamDebug.shortId(this.vehicleId), this.timeout, this.entry.chunk(),
                    this.entry.dimension().location(), requester.getScoreboardName());
            this.callback.onTimeout(requester);
        }

    }

}
