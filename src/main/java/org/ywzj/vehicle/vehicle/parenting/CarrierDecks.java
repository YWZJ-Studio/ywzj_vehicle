package org.ywzj.vehicle.vehicle.parenting;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of carrier vehicles per level for quick lookup.
 * Uses copy-on-write lists updated from the tick thread only; off-thread solves read frozen snapshots.
 */
public final class CarrierDecks {

    private static final Map<Level, List<AbstractVehicle>> BY_LEVEL = new ConcurrentHashMap<>();

    private CarrierDecks() {}

    /** Register or unregister a vehicle as a carrier; idempotent list membership test. */
    public static void setRegistered(AbstractVehicle vehicle, boolean carrier) {
        Level level = vehicle.level();
        List<AbstractVehicle> carriers = BY_LEVEL.get(level);
        if (!carrier) {
            if (carriers != null && carriers.contains(vehicle)) {
                remove(level, vehicle);
            }
            return;
        }
        if (carriers != null && carriers.contains(vehicle)) {
            return;
        }
        add(level, vehicle);
    }

    /** Unregister a vehicle when it leaves the world. */
    public static void unregister(AbstractVehicle vehicle) {
        remove(vehicle.level(), vehicle);
    }

    /** Remove all carriers from a level when it unloads. */
    public static void forget(Level level) {
        BY_LEVEL.remove(level);
    }

    private static synchronized void add(Level level, AbstractVehicle vehicle) {
        List<AbstractVehicle> current = BY_LEVEL.get(level);
        List<AbstractVehicle> next = current == null ? new ArrayList<>(2) : new ArrayList<>(current);
        if (next.contains(vehicle)) {
            return;
        }
        next.add(vehicle);
        BY_LEVEL.put(level, List.copyOf(next));
    }

    private static synchronized void remove(Level level, AbstractVehicle vehicle) {
        List<AbstractVehicle> current = BY_LEVEL.get(level);
        if (current == null || !current.contains(vehicle)) {
            return;
        }
        List<AbstractVehicle> next = new ArrayList<>(current);
        next.remove(vehicle);
        if (next.isEmpty()) {
            BY_LEVEL.remove(level);
        } else {
            BY_LEVEL.put(level, List.copyOf(next));
        }
    }

    /** Get carriers in a level; returns empty list if none exist. */
    public static List<AbstractVehicle> in(Level level) {
        List<AbstractVehicle> carriers = BY_LEVEL.get(level);
        return carriers == null ? Collections.emptyList() : carriers;
    }

    /** Check if a level contains any carriers. */
    public static boolean any(Level level) {
        return BY_LEVEL.containsKey(level);
    }

    /** Find the carrier with maximum overlap to a region; null if none found or overlapping two. */
    @Nullable
    public static AbstractVehicle nearest(AbstractVehicle vehicle, AABB region) {
        List<AbstractVehicle> carriers = in(vehicle.level());
        AbstractVehicle best = null;
        double bestOverlap = 0;
        for (int i = 0, size = carriers.size(); i < size; i++) {
            AbstractVehicle carrier = carriers.get(i);
            if (carrier == vehicle || carrier.isRemoved() || !carrier.collision
                    || carrier.level() != vehicle.level()) {
                continue;
            }
            // Prevent cycles: a parked carrier must not also become its parent's floor.
            if (VehicleHarness.carrierOf(carrier) == vehicle) {
                continue;
            }
            AABB bounds = carrier.getBoundingBox();
            double overlap = overlapVolume(bounds, region);
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                best = carrier;
            }
        }
        return best;
    }

    private static double overlapVolume(AABB a, AABB b) {
        double x = Math.min(a.maxX, b.maxX) - Math.max(a.minX, b.minX);
        if (x <= 0) {
            return 0;
        }
        double y = Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY);
        if (y <= 0) {
            return 0;
        }
        double z = Math.min(a.maxZ, b.maxZ) - Math.max(a.minZ, b.minZ);
        return z <= 0 ? 0 : x * y * z;
    }

}
