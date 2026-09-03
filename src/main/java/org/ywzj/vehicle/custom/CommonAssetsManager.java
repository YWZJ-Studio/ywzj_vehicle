package org.ywzj.vehicle.custom;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.custom.IStructureModelManager;
import org.ywzj.vehicle.api.custom.IVehicleDataManager;
import org.ywzj.vehicle.api.custom.IVehicleWeaponManager;
import org.ywzj.vehicle.network.SliceReassembler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@EventBusSubscriber
public class CommonAssetsManager {

    public static CommonAssetsManager INSTANCE;
    private final StructureModelManager structureModelManager = new StructureModelManager();
    private final VehicleWeaponManager vehicleWeaponManager = new VehicleWeaponManager();
    private final VehicleDataManager vehicleDataManager = new VehicleDataManager();

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        CommonAssetsManager manager = new CommonAssetsManager();
        event.addListener(manager.structureModelManager);
        event.addListener(manager.vehicleWeaponManager);
        event.addListener(manager.vehicleDataManager);
        event.addListener((barrier, resourceManager, preparationProfiler,
                           reloadProfiler, backgroundExecutor, gameExecutor)
                -> barrier.wait(Void.TYPE).thenRunAsync(() -> INSTANCE = manager, gameExecutor));
        // 首次加载时设置实例，避免重载步骤中无法访问前置的数据
        if (INSTANCE == null) {
            INSTANCE = manager;
        }
    }

    public void reload(ResourceManager resourceManager) {
        structureModelManager.apply(structureModelManager.prepare(resourceManager, null), null, null);
        vehicleWeaponManager.apply(vehicleWeaponManager.prepare(resourceManager, null), null, null);
        vehicleDataManager.apply(vehicleDataManager.prepare(resourceManager, null), null, null);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        INSTANCE = null;
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (INSTANCE != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeMap(INSTANCE.structureModelManager.getCache(),
                    FriendlyByteBuf::writeResourceLocation,
                    (b, s) -> b.writeUtf(s));
            buf.writeMap(INSTANCE.vehicleWeaponManager.getCache(),
                    FriendlyByteBuf::writeResourceLocation,
                    (b, s) -> b.writeUtf(s));
            buf.writeMap(INSTANCE.vehicleDataManager.getCache(),
                    FriendlyByteBuf::writeResourceLocation,
                    (b, s) -> b.writeUtf(s));
            byte[] data = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), data);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                 GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(data);
                gzip.finish();
                buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(output.toByteArray()));
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to gzip common assets", exception);
            }
            var packets = SliceReassembler.sliceData(buf);
            for (var packet : packets) {
                if (event.getPlayer() != null) {
                    PacketDistributor.sendToPlayer(event.getPlayer(), packet);
                } else {
                    PacketDistributor.sendToAllPlayers(packet);
                }
            }
        }
    }

    public static void fromNetwork(FriendlyByteBuf buf) {
        try {
            byte[] compressed = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), compressed);
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                compressed = gzip.readAllBytes();
            }
            FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.wrappedBuffer(compressed));
            var structureModelMap = data.readMap(FriendlyByteBuf::readResourceLocation, byteBuf -> byteBuf.readUtf());
            var vehicleWeaponMap = data.readMap(FriendlyByteBuf::readResourceLocation, byteBuf -> byteBuf.readUtf());
            var vehicleDataMap = data.readMap(FriendlyByteBuf::readResourceLocation, byteBuf -> byteBuf.readUtf());
            StructureModelManager.fromNetwork(structureModelMap);
            VehicleWeaponManager.fromNetwork(vehicleWeaponMap);
            VehicleDataManager.fromNetwork(vehicleDataMap);
        } catch (Exception exception) {
            YwzjVehicle.LOGGER.error("Failed to read common assets from network", exception);
        }
    }

    public static IStructureModelManager structureModelManager() {
        return INSTANCE != null ? INSTANCE.structureModelManager : StructureModelManager.ClientCache.INSTANCE;
    }

    public static IVehicleWeaponManager vehicleWeaponManager() {
        return INSTANCE != null ? INSTANCE.vehicleWeaponManager : VehicleWeaponManager.ClientCache.INSTANCE;
    }

    public static IVehicleDataManager vehicleDataManager() {
        return INSTANCE != null ? INSTANCE.vehicleDataManager : VehicleDataManager.ClientCache.INSTANCE;
    }

}
