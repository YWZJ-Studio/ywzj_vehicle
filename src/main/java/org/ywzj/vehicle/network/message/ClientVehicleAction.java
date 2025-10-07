package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.PartUnit;

import java.util.function.Supplier;

public class ClientVehicleAction {

    public int vehicleEntityId;
    public boolean leaveVehicle;
    public int weaponIndex;
    public boolean shoot;
    public float ammoX;
    public float ammoY;
    public float ammoZ;
    public float ammoXRot;
    public float ammoYRot;
    public float xAimRot;
    public float yAimRot;

    public ClientVehicleAction() {}

    public static ClientVehicleAction decode(FriendlyByteBuf buf) {
        ClientVehicleAction control = new ClientVehicleAction();
        control.vehicleEntityId = buf.readInt();
        control.leaveVehicle = buf.readBoolean();
        if (control.leaveVehicle) {
            return control;
        }
        control.weaponIndex =  buf.readInt();
        control.shoot = buf.readBoolean();
        if (control.shoot) {
            control.ammoX = buf.readFloat();
            control.ammoY = buf.readFloat();
            control.ammoZ = buf.readFloat();
            control.ammoXRot = buf.readFloat();
            control.ammoYRot = buf.readFloat();
        } else {
            control.xAimRot = buf.readFloat();
            control.yAimRot = buf.readFloat();
        }
        return control;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeBoolean(leaveVehicle);
        if (leaveVehicle) {
            return;
        }
        buf.writeInt(weaponIndex);
        buf.writeBoolean(shoot);
        if (shoot) {
            buf.writeFloat(ammoX);
            buf.writeFloat(ammoY);
            buf.writeFloat(ammoZ);
            buf.writeFloat(ammoXRot);
            buf.writeFloat(ammoYRot);
        } else {
            buf.writeFloat(xAimRot);
            buf.writeFloat(yAimRot);
        }
    }

    public static void onClientMessageReceived(ClientVehicleAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            if (message.leaveVehicle) {
                AbstractVehicle.onClientVehicleAction(message, ctxSupplier);
            } else {
                PartUnit.onClientMessageReceived(message, ctxSupplier);
            }
        });
    }

}
