package org.ywzj.vehicle.custom.sync;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializer;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerEntityDataUpdate;
import org.ywzj.vehicle.vehicle.parts.PartUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Simplified data synchronization utility for {@link PartUnit} data on vehicles.
 * Each {@link PartUnit} maintains its own instance.
 * Should be reconstructed when the vehicle's {@link PartUnit} structure changes.
 * Synchronized data should be primitive types and read-only on the client.
 * Declaration order of sync data must be strictly consistent on both sides.
 */
public class PartUnitSyncData {
    private final PartUnit<?> partUnit;
    private final int intervalTick;
    private List<SyncDataHolder<?>> dataHolders = new ArrayList<>();
    private boolean isInitialized = false;
    private SyncMode syncMode = SyncMode.TRACKING; // Default sync mode

    /**
     * Synchronization mode determines which players receive data updates.
     */
    public enum SyncMode {
        /**
         * All players tracking this entity (default).
         * Used for visible data like position, rotation, animations.
         */
        TRACKING,
        
        /**
         * Only passengers of the entity.
         * Used for internal data like ammo count, reload status.
         */
        PASSENGERS_ONLY,
        
        /**
         * Only the operator/controller of this part unit.
         * Used for sensitive data like targeting information, weapon status.
         */
        OPERATOR_ONLY,
        
        /**
         * All players within a specific range.
         * Used for proximity-based data like engine sounds, particle effects.
         */
        NEARBY_PLAYERS,
        
        /**
         * No automatic synchronization.
         * Data must be manually synchronized using custom packets.
         */
        MANUAL
    }

    /**
     * Range for NEARBY_PLAYERS sync mode (in blocks).
     */
    private double nearbyRange = 64.0;

    public PartUnitSyncData(@NotNull PartUnit<?> partUnit, int intervalTick) {
        this.intervalTick = intervalTick;
        this.partUnit = partUnit;
    }

    public PartUnitSyncData(@NotNull PartUnit<?> partUnit) {
        this(partUnit, 1);
    }

    /**
     * Sets the synchronization mode for this data.
     * Must be called before initialization.
     * 
     * @param syncMode The sync mode to use
     * @return This instance for method chaining
     */
    public PartUnitSyncData setSyncMode(SyncMode syncMode) {
        if (isInitialized) {
            throw new IllegalStateException("Cannot change sync mode after initialization");
        }
        this.syncMode = syncMode;
        return this;
    }

    /**
     * Sets the range for NEARBY_PLAYERS sync mode.
     * Must be called before initialization.
     * 
     * @param range Range in blocks
     * @return This instance for method chaining
     */
    public PartUnitSyncData setNearbyRange(double range) {
        if (isInitialized) {
            throw new IllegalStateException("Cannot change nearby range after initialization");
        }
        this.nearbyRange = range;
        return this;
    }

    /**
     * Gets the current synchronization mode.
     */
    public SyncMode getSyncMode() {
        return syncMode;
    }

    /**
     * Gets the nearby range for NEARBY_PLAYERS mode.
     */
    public double getNearbyRange() {
        return nearbyRange;
    }

    public void initialize() {
        this.dataHolders = List.copyOf(dataHolders);
        isInitialized = true;
    }

    public <T> SyncDataHolder<T> define(SyncDataSerializer<T> serializer, Consumer<T> setter,
                                        Supplier<T> getter, T initialValue) {
        if (isInitialized) {
            throw new IllegalStateException("Cannot define new sync data after initialization");
        }
        int index = dataHolders.size();
        var holder = new SyncDataHolder<>(index, serializer, setter, getter, initialValue);
        dataHolders.add(holder);
        return holder;
    }

    @OnlyIn(Dist.CLIENT)
    public void onUpdateReceived(List<SyncDataEntry<?>> entries) {
        for (var entry : entries) {
            var holder = dataHolders.get(entry.index());
            holder.onUpdate(entry);
        }
    }


    public void tick() {
        // Skip sync if mode is MANUAL
        if (syncMode == SyncMode.MANUAL) {
            return;
        }

        int entityId = partUnit.getVehicle().getId();
        if (partUnit.getVehicle().tickCount % intervalTick == 0) {
            var dirtyEntries = packDirty(false);
            if (!dirtyEntries.isEmpty()) {
                var message = new ServerEntityDataUpdate(entityId, partUnit.getIndex(), dirtyEntries);
                sendToTargets(message);
            }
        }
    }

    /**
     * Sends sync data to appropriate targets based on sync mode.
     */
    private void sendToTargets(ServerEntityDataUpdate message) {
        var vehicle = partUnit.getVehicle();
        
        switch (syncMode) {
            case TRACKING -> {
                // Send to all players tracking the entity
                Channel.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> vehicle),
                    message
                );
            }
            
            case PASSENGERS_ONLY -> {
                // Send only to passengers
                vehicle.getPassengers().forEach(passenger -> {
                    if (passenger instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        Channel.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> serverPlayer),
                            message
                        );
                    }
                });
            }
            
            case OPERATOR_ONLY -> {
                // Send only to the operator of this part unit
                var operator = partUnit.getOwner();
                if (operator instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    Channel.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        message
                    );
                }
            }
            
            case NEARBY_PLAYERS -> {
                // Send to players within range
                Channel.CHANNEL.send(
                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        vehicle.getX(),
                        vehicle.getY(),
                        vehicle.getZ(),
                        nearbyRange,
                        vehicle.level().dimension()
                    )),
                    message
                );
            }
            
            case MANUAL -> {
                // No automatic sending - handled externally
            }
        }
    }

    /**
     * Manually sends sync data to specific targets.
     * Useful for MANUAL sync mode or custom synchronization logic.
     * 
     * @param distributor Packet distributor defining targets
     */
    public void manualSync(PacketDistributor.PacketTarget distributor) {
        var dirtyEntries = packDirty(true);
        if (!dirtyEntries.isEmpty()) {
            var message = new ServerEntityDataUpdate(
                partUnit.getVehicle().getId(),
                partUnit.getIndex(),
                dirtyEntries
            );
            Channel.CHANNEL.send(distributor, message);
        }
    }

    public List<SyncDataEntry<?>> packDirty(boolean force) {
        List<SyncDataEntry<?>> dirtyEntries = new ArrayList<>();
        List<SyncDataHolder<?>> holdersToClean = new ArrayList<>();

        for (SyncDataHolder<?> holder : dataHolders) {
            holder.refresh();

            if (force) {
                dirtyEntries.add(holder.createEntry());
            } else if (holder.isDirty()) {
                dirtyEntries.add(holder.createEntry());
                holdersToClean.add(holder);
            }
        }

        for (SyncDataHolder<?> holder : holdersToClean) {
            holder.setDirty(false);
        }

        return dirtyEntries;
    }
}
