package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.vehicle.ControlUnit;

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
        ControlUnit.onClientMessageReceived(message, ctxSupplier);
    }

}
