package org.ywzj.vehicle.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import org.ywzj.vehicle.all.AllConfigs;
import org.ywzj.vehicle.api.entity.RemoteTickEntity;
import org.ywzj.vehicle.client.handler.DistantVehicleRenderHandler;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ServerBroadcastEntities {

    public ResourceLocation dimension;
    public long tick;
    public int interval;
    public List<BroadcastEntity> entities;
    public record BroadcastEntity(int entityId, ResourceLocation entityType, Vec3 entityPosition, Vec3 entityVelocity, CompoundTag data, RenderData renderData) {}
    public record RenderData(UUID uuid, ResourceLocation vehicleId, ResourceLocation displayId, Quaternionf rotation, float radius, CompoundTag data) {

        public static RenderData of(AbstractVehicle vehicle) {
            var box = vehicle.getBoundingBox();
            float radius = (float) (box.getCenter().distanceTo(vehicle.position())
                    + new Vec3(box.getXsize(), box.getYsize(), box.getZsize()).length() * 0.5);
            return new RenderData(vehicle.getUUID(), vehicle.getVehicleId(), vehicle.getDisplayId(),
                    vehicle.rotYXZ(), Math.max(1, radius), vehicle.serializeDistantRenderData());
        }

    }

    public static void broadcast(ServerLevel level, List<Entity> entities) {
        double distance = AllConfigs.server.extendedRenderDistance.get();
        int interval = AllConfigs.server.serverBroadcastEntitiesInterval.get();
        var chunkMap = level.getChunkSource().chunkMap;
        Map<Integer, BroadcastEntity> radarEntities = new HashMap<>();
        Map<Integer, BroadcastEntity> renderEntities = new HashMap<>();
        for (ServerPlayer player : level.players()) {
            boolean receiveRadar = player.getVehicle() instanceof AbstractVehicle;
            Vec3 observerVelocity = player.getRootVehicle().getDeltaMovement();
            ServerBroadcastEntities message = new ServerBroadcastEntities();
            message.dimension = level.dimension().location();
            message.tick = level.getGameTime();
            message.interval = interval;
            message.entities = new ArrayList<>();
            for (Entity target : entities) {
                var trackedEntity = chunkMap.entityMap.get(target.getId());
                if (trackedEntity != null && trackedEntity.seenBy.contains(player.connection)) {
                    // 与原版一致，取实体有效广播范围和服务端视距的较小值，按水平距离判断。
                    double trackingRange = Math.min(trackedEntity.getEffectiveRange(), chunkMap.viewDistance * 16);
                    // 提前两个同步周期缓存快照；考虑观察者乘坐载具的速度，并为低速/起步保留一个区块的余量。
                    double relativeSpeed = target.getDeltaMovement().subtract(observerVelocity).horizontalDistance();
                    double margin = Math.max(16.0, relativeSpeed * 2.0 * interval);
                    double innerRange = Math.max(0.0, trackingRange - margin);
                    if (target.position().subtract(player.position()).horizontalDistanceSqr() < innerRange * innerRange) {
                        continue;
                    }
                }
                boolean render = distance > 0 && target instanceof AbstractVehicle vehicle
                        && vehicle.renderBeyondRenderDistance && !vehicle.isInvisible()
                        && vehicle.distanceToSqr(player) <= distance * distance;
                boolean radar = receiveRadar && (!(target instanceof AbstractVehicle vehicle) || !vehicle.isDestroyed());
                if (!radar && !render) {
                    continue;
                }
                BroadcastEntity entity = radarEntities.computeIfAbsent(target.getId(), id -> {
                    CompoundTag data = new CompoundTag();
                    if (target instanceof RemoteTickEntity remoteEntity) {
                        remoteEntity.writeData(data);
                    }
                    return new BroadcastEntity(id, EntityType.getKey(target.getType()), target.position(), target.getDeltaMovement(), data, null);
                });
                message.entities.add(render ? renderEntities.computeIfAbsent(target.getId(), id ->
                        new BroadcastEntity(entity.entityId, entity.entityType, entity.entityPosition, entity.entityVelocity, entity.data, RenderData.of((AbstractVehicle) target)))
                        : entity);
            }
            Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    public static void encode(ServerBroadcastEntities message, FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.dimension);
        buf.writeLong(message.tick);
        buf.writeVarInt(message.interval);
        buf.writeVarInt(message.entities.size());
        for (BroadcastEntity entity : message.entities) {
            buf.writeInt(entity.entityId);
            buf.writeResourceLocation(entity.entityType);
            buf.writeDouble(entity.entityPosition.x);
            buf.writeDouble(entity.entityPosition.y);
            buf.writeDouble(entity.entityPosition.z);
            buf.writeDouble(entity.entityVelocity.x);
            buf.writeDouble(entity.entityVelocity.y);
            buf.writeDouble(entity.entityVelocity.z);
            buf.writeNbt(entity.data);
            RenderData render = entity.renderData;
            buf.writeBoolean(render != null);
            if (render != null) {
                buf.writeUUID(render.uuid);
                buf.writeResourceLocation(render.vehicleId);
                buf.writeResourceLocation(render.displayId);
                buf.writeFloat(render.rotation.x);
                buf.writeFloat(render.rotation.y);
                buf.writeFloat(render.rotation.z);
                buf.writeFloat(render.rotation.w);
                buf.writeFloat(render.radius);
                buf.writeNbt(render.data);
            }
        }
    }

    public static ServerBroadcastEntities decode(FriendlyByteBuf buf) {
        ServerBroadcastEntities message = new ServerBroadcastEntities();
        message.dimension = buf.readResourceLocation();
        message.tick = buf.readLong();
        message.interval = Math.max(1, buf.readVarInt());
        message.entities = new ArrayList<>();
        int size = buf.readVarInt();
        for (int index = 0; index < size; index++) {
            int id = buf.readInt();
            ResourceLocation type = buf.readResourceLocation();
            Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            CompoundTag data = buf.readNbt();
            RenderData render = null;
            if (buf.readBoolean()) {
                render = new RenderData(buf.readUUID(), buf.readResourceLocation(), buf.readResourceLocation(),
                        new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()).normalize(),
                        buf.readFloat(), buf.readNbt());
            }
            message.entities.add(new BroadcastEntity(id, type, position, velocity, data, render));
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
        Level level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(message.dimension)) {
            return;
        }
        DistantVehicleRenderHandler.receive(message);
        if (!LocalVehiclePlayer.instance.onVehicle()) {
            return;
        }
        WeaponUnit weaponUnit = LocalVehiclePlayer.instance.getWeaponUnit();
        if (weaponUnit == null) {
            return;
        }
        for (BroadcastEntity broadcastEntity : message.entities) {
            ConcurrentHashMap<Integer, LocalVehiclePlayer.ServerEntity> serverEntities = LocalVehiclePlayer.instance.serverEntities;
            LocalVehiclePlayer.ServerEntity serverEntity = serverEntities.get(broadcastEntity.entityId);
            if (serverEntity == null) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(broadcastEntity.entityType);
                Entity entity = type == null ? null : type.create(level);
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
