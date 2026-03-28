package org.ywzj.vehicle.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.RadarUnit;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ServerBroadcastEntities {

    public List<BroadcastEntity> entities;

    public record BroadcastEntity(int entityId, ResourceLocation entityType, Vec3 entityPosition, Vec3 entityVelocity, CompoundTag data) {}

    public ServerBroadcastEntities() {}

    public static void encode(ServerBroadcastEntities message, FriendlyByteBuf buf) {
        int size = message.entities.size();
        buf.writeInt(size);
        for (int index = 0; index < size; index++) {
            buf.writeInt(message.entities.get(index).entityId);
            buf.writeResourceLocation(message.entities.get(index).entityType);
            buf.writeDouble(message.entities.get(index).entityPosition.x);
            buf.writeDouble(message.entities.get(index).entityPosition.y);
            buf.writeDouble(message.entities.get(index).entityPosition.z);
            buf.writeDouble(message.entities.get(index).entityVelocity.x);
            buf.writeDouble(message.entities.get(index).entityVelocity.y);
            buf.writeDouble(message.entities.get(index).entityVelocity.z);
            buf.writeNbt(message.entities.get(index).data);
        }
    }

    public static ServerBroadcastEntities decode(FriendlyByteBuf buf) {
        ServerBroadcastEntities message = new ServerBroadcastEntities();
        message.entities = new ArrayList<>();
        int size = buf.readInt();
        for (int index = 0; index < size; index++) {
            BroadcastEntity broadcastEntity = new BroadcastEntity(buf.readInt(),
                    buf.readResourceLocation(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readNbt());
            message.entities.add(broadcastEntity);
        }
        return message;
    }

    public static void onServerMessageReceived(ServerBroadcastEntities message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handle(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handle(ServerBroadcastEntities message) {
        if (!LocalVehiclePlayer.instance.onVehicle()) {
            return;
        }
        WeaponUnit weaponUnit = LocalVehiclePlayer.instance.getWeaponUnit();
        if (weaponUnit == null) {
            return;
        }
        RadarUnit radarUnit = weaponUnit.getRadarUnit();
        if (radarUnit == null) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        for (BroadcastEntity broadcastEntity : message.entities) {
            if (level.getEntity(broadcastEntity.entityId) != null) {
                continue;
            }
            ConcurrentHashMap<Integer, LocalVehiclePlayer.ServerEntity> serverEntities = LocalVehiclePlayer.instance.serverEntities;
            LocalVehiclePlayer.ServerEntity serverEntity = serverEntities.get(broadcastEntity.entityId);
            if (serverEntity == null) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(broadcastEntity.entityType);
                Entity entity = type.create(level);
                if (entity != null) {
                    entity.load(broadcastEntity.data);
                    entity.setId(broadcastEntity.entityId);
                    entity.xo = broadcastEntity.entityPosition.x;
                    entity.yo = broadcastEntity.entityPosition.y;
                    entity.zo = broadcastEntity.entityPosition.z;
                    entity.setPos(broadcastEntity.entityPosition);
                    entity.setDeltaMovement(broadcastEntity.entityVelocity);
                    serverEntity = new LocalVehiclePlayer.ServerEntity();
                    serverEntity.entity = entity;
                    serverEntity.updateTick = LocalVehiclePlayer.instance.getPlayer().tickCount;
                    if (entity instanceof AbstractVehicle vehicle) {
                        vehicle.remote = true;
                    }
                    if (entity instanceof RemoteTickEntity remoteTickEntity) {
                        remoteTickEntity.readData(broadcastEntity.data);
                    }
                    serverEntities.put(broadcastEntity.entityId, serverEntity);
                }
            } else {
                Entity entity = serverEntity.entity;
                entity.xo = broadcastEntity.entityPosition.x;
                entity.yo = broadcastEntity.entityPosition.y;
                entity.zo = broadcastEntity.entityPosition.z;
                entity.setPos(broadcastEntity.entityPosition);
                entity.setDeltaMovement(broadcastEntity.entityVelocity);
                if (entity instanceof RemoteTickEntity remoteTickEntity) {
                    remoteTickEntity.readData(broadcastEntity.data);
                }
                serverEntity.updateTick = LocalVehiclePlayer.instance.getPlayer().tickCount;
            }
        }
    }

}
