package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.AbstractVehicle;
import org.ywzj.vehicle.entity.WeaponUnit;

import java.util.function.Supplier;

public class ClientWeaponUnitControl {

    public int vehicleEntityId;
    public int weaponIndex;
    public boolean shoot;
    public float xRot;
    public float yRot;

    public ClientWeaponUnitControl() {}

    public static ClientWeaponUnitControl decode(FriendlyByteBuf buf) {
        ClientWeaponUnitControl control = new ClientWeaponUnitControl();
        control.vehicleEntityId = buf.readInt();
        control.weaponIndex =  buf.readInt();
        control.shoot = buf.readBoolean();
        if (control.shoot) {
            return control;
        }
        control.xRot = buf.readFloat();
        control.yRot = buf.readFloat();
        return control;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(weaponIndex);
        buf.writeBoolean(shoot);
        if (shoot) {
            return;
        }
        buf.writeFloat(xRot);
        buf.writeFloat(yRot);
    }

    public static void onClientMessageReceived(ClientWeaponUnitControl message, Supplier<NetworkEvent.Context> ctxSupplier) {
        if (ctxSupplier.get().getSender() != null) {
            Level level = ctxSupplier.get().getSender().level();
            Entity entity = level.getEntity(message.vehicleEntityId);
            if (entity instanceof AbstractVehicle vehicle) {
                if (message.weaponIndex < vehicle.weaponUnits.size()) {
                    if (message.shoot) {
                        vehicle.shoot(message.weaponIndex);
                    } else {
                        WeaponUnit serverWeaponUnit = vehicle.weaponUnits.get(message.weaponIndex);
                        serverWeaponUnit.aimXRot = message.xRot;
                        serverWeaponUnit.aimYRot = message.yRot % 360;
                    }
                }
            }
        }
    }

}
