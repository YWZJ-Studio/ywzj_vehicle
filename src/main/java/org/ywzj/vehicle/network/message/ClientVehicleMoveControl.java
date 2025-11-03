package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.vehicle.control.ControlUnit;

import java.util.function.Supplier;

public class ClientVehicleMoveControl {

    public int vehicleEntityId;
    public boolean forward;
    public boolean backward;
    public boolean left;
    public boolean right;
    public boolean up;
    public boolean down;
    public boolean leftYaw;
    public boolean rightYaw;
    public boolean functionalUp;
    public boolean functionalDown;
    public boolean functionalLeft;
    public boolean functionalRight;
    public float xRot;
    public boolean xRotKeep;
    public float yRot;
    public boolean yRotKeep;

    public ClientVehicleMoveControl() {}

    public static ClientVehicleMoveControl decode(FriendlyByteBuf buf) {
        ClientVehicleMoveControl control = new ClientVehicleMoveControl();
        control.vehicleEntityId = buf.readInt();
        control.forward = buf.readBoolean();
        control.backward = buf.readBoolean();
        control.left = buf.readBoolean();
        control.right = buf.readBoolean();
        control.up = buf.readBoolean();
        control.down = buf.readBoolean();
        control.leftYaw = buf.readBoolean();
        control.rightYaw = buf.readBoolean();
        control.functionalUp = buf.readBoolean();
        control.functionalDown = buf.readBoolean();
        control.functionalLeft = buf.readBoolean();
        control.functionalRight = buf.readBoolean();
        control.xRot = buf.readFloat();
        control.xRotKeep = buf.readBoolean();
        control.yRot = buf.readFloat();
        control.yRotKeep = buf.readBoolean();
        return control;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeBoolean(forward);
        buf.writeBoolean(backward);
        buf.writeBoolean(left);
        buf.writeBoolean(right);
        buf.writeBoolean(up);
        buf.writeBoolean(down);
        buf.writeBoolean(leftYaw);
        buf.writeBoolean(rightYaw);
        buf.writeBoolean(functionalUp);
        buf.writeBoolean(functionalDown);
        buf.writeBoolean(functionalLeft);
        buf.writeBoolean(functionalRight);
        buf.writeFloat(xRot);
        buf.writeBoolean(xRotKeep);
        buf.writeFloat(yRot);
        buf.writeBoolean(yRotKeep);
    }

    public static void onClientMessageReceived(ClientVehicleMoveControl message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> ControlUnit.onClientMessageReceived(message, ctxSupplier));
    }

}
