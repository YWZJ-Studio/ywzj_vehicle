package org.ywzj.vehicle.network.message;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
public record ClientVehicleSwitchWeapon(
        int vehicleEntityId,
        boolean secondary,
        boolean next
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientVehicleSwitchWeapon> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "vehicle_switch_weapon"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ClientVehicleSwitchWeapon> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ClientVehicleSwitchWeapon::decode);

    public static void encode(ClientVehicleSwitchWeapon msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleEntityId);
        buf.writeBoolean(msg.secondary);
        buf.writeBoolean(msg.next);
    }

    public static ClientVehicleSwitchWeapon decode(net.minecraft.network.FriendlyByteBuf buf) {
        int vehicleEntityId = buf.readInt();
        boolean secondary = buf.readBoolean();
        boolean next = buf.readBoolean();
        return new ClientVehicleSwitchWeapon(vehicleEntityId, secondary, next);
    }

    public static void handle(ClientVehicleSwitchWeapon msg, IPayloadContext ctxSupplier) {
        var serverPlayer = (net.minecraft.server.level.ServerPlayer) ctxSupplier.player();
        if (serverPlayer.level().getEntity(msg.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            var partUnit = vehicle.getOwnOperatorUnit(serverPlayer);
            if (partUnit instanceof WeaponUnit weaponUnit) {
                weaponUnit.switchWeapon(msg.secondary, msg.next);
            }
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
