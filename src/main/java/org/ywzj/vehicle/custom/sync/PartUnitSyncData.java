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
 * 一个简易的数据同步工具，用于载具上 {@link PartUnit} 的数据同步，由每个 {@link PartUnit} 各自持有<br/>
 * 在载具的 {@link PartUnit} 结构发生后，应重新构造<br/>
 * 需要同步的数据应该是一个基础数据类型，且在客户端是只读的<br/>
 * 你需要确保声明同步数据的顺序在两侧保持严格一致<br/>
 */
public class PartUnitSyncData {
    private final PartUnit<?> partUnit;
    private final int intervalTick;
    private List<SyncDataHolder<?>> dataHolders = new ArrayList<>();
    private boolean isInitialized = false;

    //todo: 支持不同的同步模式
    public enum SyncMode {
        /**
         * 所有追踪此实体的玩家
         */
        TRACKING,
        /**
         * 仅实体的乘客
         */
        PASSENGERS_ONLY
    }

    public PartUnitSyncData(@NotNull PartUnit<?> partUnit, int intervalTick) {
        this.intervalTick = intervalTick;
        this.partUnit = partUnit;
    }

    public PartUnitSyncData(@NotNull PartUnit<?> partUnit) {
        this(partUnit, 1);
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
        int entityId = partUnit.getVehicle().getId();
        if (partUnit.getVehicle().tickCount % intervalTick == 0) {
            var dirtyEntries = packDirty(false);
            if (!dirtyEntries.isEmpty()) {
                var message = new ServerEntityDataUpdate(entityId, partUnit.getIndex(), dirtyEntries);
                Channel.CHANNEL.send(
                        PacketDistributor.TRACKING_ENTITY.with(partUnit::getVehicle),
                        message
                );
            }
        }
    }

    public List<SyncDataEntry<?>> packDirty(boolean force) {
        List<SyncDataEntry<?>> dirtyEntries = new ArrayList<>();
        for (SyncDataHolder<?> holder : dataHolders) {
            holder.refresh();

            if (force || holder.isDirty()) {
                dirtyEntries.add(holder.createEntry());
                holder.setDirty(false);
            }
        }
        return dirtyEntries;
    }
}
