package org.ywzj.vehicle.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.WeaponUnit;

import java.util.function.Supplier;

public record ServerVehicleFire (
        int vehicleEntityId,
        int operatorEntityId,
        int partUnitIndex,
        int weaponIndex
) {

    public static void encode(ServerVehicleFire msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleEntityId);
        buf.writeInt(msg.operatorEntityId);
        buf.writeInt(msg.partUnitIndex);
        buf.writeInt(msg.weaponIndex);
    }

    public static ServerVehicleFire decode(FriendlyByteBuf buf) {
        return new ServerVehicleFire(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void onServerMessageReceived(ServerVehicleFire msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.setPacketHandled(true);
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handle(msg));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handle(ServerVehicleFire message) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(message.vehicleEntityId);
        if (entity instanceof AbstractVehicle vehicle) {
            var parts = vehicle.getPartUnits();
            if (message.partUnitIndex < 0 || message.partUnitIndex >= parts.size()) {
                return;
            }
            var partUnit = parts.get(message.partUnitIndex);
            if (!(partUnit instanceof WeaponUnit weaponUnit)) {
                return;
            }
            if (message.weaponIndex < 0 || message.weaponIndex >= weaponUnit.indexedWeapons.size()) {
                return;
            }
            LivingEntity operator = null;
            if (message.operatorEntityId != -1) {
                Entity operatorEntity = level.getEntity(message.operatorEntityId);
                if (operatorEntity instanceof LivingEntity livingEntity) {
                    operator = livingEntity;
                }
            }
            MinecraftForge.EVENT_BUS.post(
                    new VehicleFireEvent.Post(
                            vehicle,
                            weaponUnit.indexedWeapons.get(message.weaponIndex),
                            operator
                    )
            );
        }
    }

}
