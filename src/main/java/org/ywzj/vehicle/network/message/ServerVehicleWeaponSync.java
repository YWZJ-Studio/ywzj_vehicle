package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.misc.weapon.AbstractVehicleWeapon;
import org.ywzj.vehicle.vehicle.WeaponUnit;

import java.util.function.Supplier;

/**
 * 服务器同步车辆武器状态消息
 */
public class ServerVehicleWeaponSync {
    public int vehicleEntityId;
    public int vehiclePartUnitId;
    public int weaponIndex;

    public FriendlyByteBuf buf;
    private AbstractVehicleWeapon<?> vehicleWeapon;

    private ServerVehicleWeaponSync() {}

    public ServerVehicleWeaponSync(WeaponUnit weaponUnit, AbstractVehicleWeapon<?> vehicleWeapon) {
        this.vehicleEntityId = weaponUnit.getVehicle().getId();
        this.vehiclePartUnitId = weaponUnit.getIndex();
        this.weaponIndex = vehicleWeapon.getIndex();
        this.vehicleWeapon = vehicleWeapon;
    }

    public static ServerVehicleWeaponSync decode(FriendlyByteBuf buf) {
        ServerVehicleWeaponSync weaponSync = new ServerVehicleWeaponSync();
        weaponSync.vehicleEntityId = buf.readInt();
        weaponSync.vehiclePartUnitId = buf.readInt();
        weaponSync.weaponIndex = buf.readInt();
        weaponSync.buf = buf;
        return weaponSync;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(vehicleEntityId);
        buf.writeInt(vehiclePartUnitId);
        buf.writeInt(weaponIndex);
        vehicleWeapon.writeSyncData(buf);
    }

    public FriendlyByteBuf getBuf() {
        return buf;
    }

    public static void onServerMessageReceived(ServerVehicleWeaponSync msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handle(msg));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handle(ServerVehicleWeaponSync message) {
        WeaponUnit.onSyncData(message);
    }

}
