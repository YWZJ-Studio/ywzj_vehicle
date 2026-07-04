package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;

import java.util.function.Supplier;

public record ClientVehicleSelectPartWeapon(int vehicleEntityId, int partUnitIndex, int weaponIndex) {

    private static final double MAX_INTERACTION_DISTANCE_SQ = 256.0;

    public static void encode(ClientVehicleSelectPartWeapon msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleEntityId);
        buf.writeInt(msg.partUnitIndex);
        buf.writeInt(msg.weaponIndex);
    }

    public static ClientVehicleSelectPartWeapon decode(FriendlyByteBuf buf) {
        return new ClientVehicleSelectPartWeapon(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void onClientMessageReceived(ClientVehicleSelectPartWeapon message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player == null || !hasModdingTool(player.getItemInHand(InteractionHand.MAIN_HAND), player.getItemInHand(InteractionHand.OFF_HAND))) {
                    return;
                }
                if (!(player.level().getEntity(message.vehicleEntityId) instanceof AbstractVehicle vehicle)) {
                    return;
                }
                if (player.distanceToSqr(vehicle) > MAX_INTERACTION_DISTANCE_SQ) {
                    return;
                }
                vehicle.getPartUnit(message.partUnitIndex).ifPresent(partUnit -> {
                    if (partUnit instanceof WeaponUnit weaponUnit
                            && weaponUnit.isInteractive()
                            && message.weaponIndex >= 0
                            && message.weaponIndex < weaponUnit.weapons.size()) {
                        weaponUnit.selectWeaponIndex(false, message.weaponIndex);
                    }
                });
            });
        }
    }

    private static boolean hasModdingTool(ItemStack mainHand, ItemStack offHand) {
        return mainHand.is(AllItems.MODDING_TOOL.get()) || offHand.is(AllItems.MODDING_TOOL.get());
    }

}
