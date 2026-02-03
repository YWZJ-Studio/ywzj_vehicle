package org.ywzj.vehicle.custom.sync;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializer;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerEntityDataUpdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SyncDataManager {
    private final Int2ReferenceMap<SyncDataSerializer<?>> syncedIdMap = new Int2ReferenceOpenHashMap<>();
    private final Reference2IntMap<SyncDataSerializer<?>> syncIdMap = new Reference2IntOpenHashMap<>();
    private final AtomicInteger nextIdTracker = new AtomicInteger();

    private static SyncDataManager INSTANCE;

    public static SyncDataManager get() {
        if (INSTANCE == null) {
            INSTANCE = new SyncDataManager();
        }
        return INSTANCE;
    }

    public synchronized <T> void registerSerializer(SyncDataSerializer<T> serializer) {
        int id = nextIdTracker.getAndIncrement();
        syncedIdMap.put(id, serializer);
        syncIdMap.put(serializer, id);
    }

    public SyncDataSerializer<?> getSerializer(int id) {
        return syncedIdMap.get(id);
    }

    public int getId(SyncDataSerializer<?> serializer) {
        return syncIdMap.getInt(serializer);
    }

    private static final Map<Integer, List<ServerEntityDataUpdate>> PENDING_MESSAGES = new ConcurrentHashMap<>();

    public static void processPendingMessagesForEntity(AbstractVehicle vehicle) {
        try {
            int entityId = vehicle.getId();
            List<ServerEntityDataUpdate> messages = PENDING_MESSAGES.remove(entityId);
            if (messages != null) {
                for (ServerEntityDataUpdate message : messages) {
                    vehicle.getPartUnit(message.partIndex()).ifPresent(
                            partUnit -> partUnit.onUpdateReceived(message.entries())
                    );
                }
            }
        } catch (Exception e) {
            YwzjVehicle.LOGGER.warn("Failed to process entity data update", e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void onMessage(ServerEntityDataUpdate message) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var entity = level.getEntity(message.entityId());
        if (entity == null) {
            // Entity not yet loaded, store the message for later processing
            PENDING_MESSAGES.computeIfAbsent(
                    message.entityId(),
                    id -> Collections.synchronizedList(new ArrayList<>())
            ).add(message);
            return;
        }
        if (entity instanceof AbstractVehicle vehicle) {
            vehicle.getPartUnit(message.partIndex()).ifPresent(
                    partUnit -> partUnit.onUpdateReceived(message.entries())
            );
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof AbstractVehicle vehicle) {
            SyncDataManager.processPendingMessagesForEntity(vehicle);
        }
    }

    @SubscribeEvent
    public static void onTrackingStart(PlayerEvent.StartTracking event) {
        Entity entity = event.getTarget();
        if (entity instanceof AbstractVehicle vehicle && event.getEntity() instanceof ServerPlayer player) {
            vehicle.getPartUnits().forEach(partUnit -> {
                var entries = partUnit.getSyncData().packDirty(true);
                if (entries.isEmpty()) {
                    return;
                }
                Channel.CHANNEL.send(
                        PacketDistributor.PLAYER.with(()-> player),
                        new ServerEntityDataUpdate(vehicle.getId(), partUnit.getIndex(), entries)
                );
            });
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            PENDING_MESSAGES.clear();
        }
    }

    @Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Setup {
        @SubscribeEvent
        public static void onCommonSetupEvent(FMLCommonSetupEvent event) {
            SyncDataManager manager = SyncDataManager.get();
            manager.registerSerializer(SyncDataSerializers.BOOLEAN);
            manager.registerSerializer(SyncDataSerializers.INT);
            manager.registerSerializer(SyncDataSerializers.FLOAT);
            manager.registerSerializer(SyncDataSerializers.DOUBLE);
            manager.registerSerializer(SyncDataSerializers.VEC3);
        }
    }
}
