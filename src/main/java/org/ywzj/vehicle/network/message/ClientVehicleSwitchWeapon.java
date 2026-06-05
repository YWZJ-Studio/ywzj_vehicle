package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

public record ClientVehicleSwitchWeapon(
        int vehicleEntityId,
        WeaponSwitchType switchType,
        boolean next
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientVehicleSwitchWeapon> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "vehicle_switch_weapon"));
    public static final StreamCodec<FriendlyByteBuf, ClientVehicleSwitchWeapon> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> encode(msg, buf),
            ClientVehicleSwitchWeapon::decode
    );

    public enum WeaponSwitchType {
        PRIMARY,
        SECONDARY,
        MULTI
    }

    public static void encode(ClientVehicleSwitchWeapon msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleEntityId);
        buf.writeEnum(msg.switchType);
        buf.writeBoolean(msg.next);
    }

    public static ClientVehicleSwitchWeapon decode(FriendlyByteBuf buf) {
        int vehicleEntityId = buf.readInt();
        WeaponSwitchType switchType = buf.readEnum(WeaponSwitchType.class);
        boolean next = buf.readBoolean();
        return new ClientVehicleSwitchWeapon(vehicleEntityId, switchType, next);
    }

    public static void handle(ClientVehicleSwitchWeapon msg, IPayloadContext ctx) {
        var serverPlayer = (ServerPlayer) ctx.player();
        if (serverPlayer.level().getEntity(msg.vehicleEntityId) instanceof AbstractVehicle vehicle) {
            var partUnit = vehicle.getOwnOperatorUnit(serverPlayer);
            if (partUnit instanceof WeaponUnit weaponUnit) {
                switch (msg.switchType) {
                    case MULTI -> weaponUnit.cycleMultiWeapon(msg.next);
                    case PRIMARY -> weaponUnit.switchWeapon(false, msg.next);
                    case SECONDARY -> weaponUnit.switchWeapon(true, msg.next);
                }
            }
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
