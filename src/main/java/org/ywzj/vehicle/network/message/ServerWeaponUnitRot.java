package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.misc.weapon.AbstractTurretUnit;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import java.util.function.Supplier;

public class ServerWeaponUnitRot {

    public int vehicleEntityId;
    public int weaponIndex;
    public float xRot;
    public float yRot;

    public ServerWeaponUnitRot() {}

    public ServerWeaponUnitRot(WeaponUnit weaponUnit) {
        this.vehicleEntityId = weaponUnit.getVehicle().getId();
        this.weaponIndex = weaponUnit.getIndex();
        this.xRot = weaponUnit.xRot;
        this.yRot = weaponUnit.yRot;
    }

    public ServerWeaponUnitRot(AbstractTurretUnit<?> weaponUnit) {
        this.vehicleEntityId = weaponUnit.getVehicle().getId();
        this.weaponIndex = weaponUnit.getIndex();
        this.xRot = weaponUnit.xRot;
        this.yRot = weaponUnit.yRot;
    }

    public static ServerWeaponUnitRot decode(FriendlyByteBuf buf) {
        ServerWeaponUnitRot serverWeaponUnitRot = new ServerWeaponUnitRot();
        serverWeaponUnitRot.vehicleEntityId = buf.readInt();
        serverWeaponUnitRot.weaponIndex  = buf.readInt();
        serverWeaponUnitRot.xRot = buf.readFloat();
        serverWeaponUnitRot.yRot = buf.readFloat();
        return serverWeaponUnitRot;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(weaponIndex);
        buf.writeFloat(xRot);
        buf.writeFloat(yRot);
    }

    public static void onServerMessageReceived(ServerWeaponUnitRot message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().setPacketHandled(true);
        ctxSupplier.get().enqueueWork(() -> AbstractVehicle.onServerWeaponUnitRot(message));
    }

}
