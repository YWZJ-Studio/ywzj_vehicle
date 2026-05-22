package org.ywzj.vehicle.network.message;

import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.function.Supplier;

public record ClientVehicleSwitchWeapon(
        int vehicleEntityId,
        WeaponSwitchType switchType,
        boolean next
) {

    public enum WeaponSwitchType {
        PRIMARY,
        SECONDARY,
        MULTI
    }

    public static void encode(ClientVehicleSwitchWeapon msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleEntityId);
        buf.writeEnum(msg.switchType);
        buf.writeBoolean(msg.next);
    }

    public static ClientVehicleSwitchWeapon decode(net.minecraft.network.FriendlyByteBuf buf) {
        int vehicleEntityId = buf.readInt();
        WeaponSwitchType switchType = buf.readEnum(WeaponSwitchType.class);
        boolean next = buf.readBoolean();
        return new ClientVehicleSwitchWeapon(vehicleEntityId, switchType, next);
    }

    public static void onReceived(ClientVehicleSwitchWeapon msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                var serverPlayer = context.getSender();
                if (serverPlayer == null) {
                    return;
                }
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
            });
        }
    }

}
