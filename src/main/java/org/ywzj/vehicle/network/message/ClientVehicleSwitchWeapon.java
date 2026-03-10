package org.ywzj.vehicle.network.message;

import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.function.Supplier;

public record ClientVehicleSwitchWeapon(
        int vehicleEntityId,
        boolean secondary,
        boolean next
) {

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

    public static void onReceived(ClientVehicleSwitchWeapon msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                var serverPlayer = context.getSender();
                if (serverPlayer == null) {
                    return;
                }
                if (serverPlayer.level().getEntity(msg.vehicleEntityId) instanceof AbstractVehicle vehicle) {
                    var partUnit = vehicle.getOwnOperatorUnit(serverPlayer);
                    if (partUnit instanceof WeaponUnit weaponUnit) {
                        weaponUnit.switchWeapon(msg.secondary, msg.next);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }

}
