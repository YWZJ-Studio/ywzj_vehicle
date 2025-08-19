package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import java.util.function.Supplier;

public class ClientWeaponUnitControl {

    public int vehicleEntityId;
    public int weaponIndex;
    public boolean shoot;
    public float ammoX;
    public float ammoY;
    public float ammoZ;
    public float ammoXRot;
    public float ammoYRot;
    public float xRot;
    public float yRot;

    public ClientWeaponUnitControl() {}

    public static ClientWeaponUnitControl decode(FriendlyByteBuf buf) {
        ClientWeaponUnitControl control = new ClientWeaponUnitControl();
        control.vehicleEntityId = buf.readInt();
        control.weaponIndex =  buf.readInt();
        control.shoot = buf.readBoolean();
        if (control.shoot) {
            control.ammoX = buf.readFloat();
            control.ammoY = buf.readFloat();
            control.ammoZ = buf.readFloat();
            control.ammoXRot = buf.readFloat();
            control.ammoYRot = buf.readFloat();
        } else {
            control.xRot = buf.readFloat();
            control.yRot = buf.readFloat();
        }
        return control;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(weaponIndex);
        buf.writeBoolean(shoot);
        if (shoot) {
            buf.writeFloat(ammoX);
            buf.writeFloat(ammoY);
            buf.writeFloat(ammoZ);
            buf.writeFloat(ammoXRot);
            buf.writeFloat(ammoYRot);
        } else {
            buf.writeFloat(xRot);
            buf.writeFloat(yRot);
        }
    }

    public static void onClientMessageReceived(ClientWeaponUnitControl message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> WeaponUnit.onClientMessageReceived( message, ctxSupplier));
    }

}
