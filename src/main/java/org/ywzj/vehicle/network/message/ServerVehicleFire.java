package org.ywzj.vehicle.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.event.VehicleFireEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.WeaponUnit;
public record ServerVehicleFire (
        int vehicleEntityId,
        int operatorEntityId,
        int partUnitIndex,
        int weaponIndex
) implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ServerVehicleFire> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), ServerVehicleFire::decode);
    public static final CustomPacketPayload.Type<ServerVehicleFire> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "vehicle_fire"));

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

    public static void handle(ServerVehicleFire msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> onClient(msg));
    }

    @OnlyIn(Dist.CLIENT)
    private static void onClient(ServerVehicleFire message) {
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
            NeoForge.EVENT_BUS.post(
                    new VehicleFireEvent.Post(
                            vehicle,
                            weaponUnit.indexedWeapons.get(message.weaponIndex),
                            operator
                    )
            );
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
