package org.ywzj.vehicle.custom;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.custom.IStructureModelManager;
import org.ywzj.vehicle.api.custom.IVehicleDataManager;
import org.ywzj.vehicle.api.custom.IVehicleWeaponManager;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.SliceReassembler;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
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
                           reloadProfiler, backgroundExecutor, gameExecutor) -> {
            return barrier.wait(Void.TYPE).thenRunAsync(() -> {
                        INSTANCE = manager;
                    }, gameExecutor);
        });
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
                    FriendlyByteBuf::writeUtf);
            buf.writeMap(INSTANCE.vehicleWeaponManager.getCache(),
                    FriendlyByteBuf::writeResourceLocation,
                    FriendlyByteBuf::writeUtf);
            buf.writeMap(INSTANCE.vehicleDataManager.getCache(),
                    FriendlyByteBuf::writeResourceLocation,
                    FriendlyByteBuf::writeUtf);

            // 切片发送
            var packets = SliceReassembler.sliceData(buf);

            for (var packet : packets) {
                if (event.getPlayer() != null) {
                    Channel.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), packet);
                } else {
                    Channel.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
                }
            }
        }
    }

    // 收到所有数据包后组装数据并重载
    public static void fromNetwork(FriendlyByteBuf buf) {
        try {
            var structureModelMap = buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readUtf);
            var vehicleWeaponMap = buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readUtf);
            var vehicleDataMap = buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readUtf);

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
