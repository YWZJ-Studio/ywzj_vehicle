package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.AbstractVehicle;

import java.util.function.Supplier;

public class ClientVehicleMoveControl {

    public int vehicleEntityId;
    public boolean forward;
    public boolean backward;
    public boolean left;
    public boolean right;

    public ClientVehicleMoveControl() {}

    public static ClientVehicleMoveControl decode(FriendlyByteBuf buf) {
        ClientVehicleMoveControl control = new ClientVehicleMoveControl();
        control.vehicleEntityId = buf.readInt();
        control.forward = buf.readBoolean();
        control.backward = buf.readBoolean();
        control.left = buf.readBoolean();
        control.right = buf.readBoolean();
        return control;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeBoolean(forward);
        buf.writeBoolean(backward);
        buf.writeBoolean(left);
        buf.writeBoolean(right);
    }

    public static void onClientMessageReceived(ClientVehicleMoveControl message, Supplier<NetworkEvent.Context> ctxSupplier) {
        if (ctxSupplier.get().getSender() != null) {
            Level level = ctxSupplier.get().getSender().level();
            Entity entity = level.getEntity(message.vehicleEntityId);
            if (entity instanceof AbstractVehicle vehicle) {
                if (ctxSupplier.get().getSender() != vehicle.controlUnit.operator) {
                    return;
                }
                vehicle.controlUnit.forward = message.forward;
                vehicle.controlUnit.backward = message.backward;
                vehicle.controlUnit.left = message.left;
                vehicle.controlUnit.right = message.right;
            }
        }
    }

}
