package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.SwitchableUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.ArrayList;
import java.util.List;

public class ClientVehicleAction implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, ClientVehicleAction> STREAM_CODEC = StreamCodec.of((buf, msg) -> msg.encode(buf), ClientVehicleAction::decode);
    public static final CustomPacketPayload.Type<ClientVehicleAction> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(YwzjVehicle.MOD_ID, "vehicle_action"));
    public int vehicleEntityId;
    public boolean leaveVehicle;
    public boolean toggleEngine;
    public boolean toggleLandingGear;
    public boolean toggleAirbrake;
    public boolean toggleHoverMode;
    public boolean toggleAerobaticSmoke;
    public int aerobaticSmokeR;
    public int aerobaticSmokeG;
    public int aerobaticSmokeB;
    public boolean togglePartUnitState;
    public int partUnitIndex;
    public boolean lockEntity;
    public int lockedEntityId;
    public boolean shoot;
    public int weaponIndex;
    public List<AimContext> aimContexts = new ArrayList<>();
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
        control.toggleEngine = buf.readBoolean();
        if (control.toggleEngine) {
            return control;
        }
        control.toggleLandingGear = buf.readBoolean();
        if (control.toggleLandingGear) {
            return control;
        }
        control.toggleAirbrake = buf.readBoolean();
        if (control.toggleAirbrake) {
            return control;
        }
        control.toggleHoverMode = buf.readBoolean();
        if (control.toggleHoverMode) {
            return control;
        }
        control.toggleAerobaticSmoke = buf.readBoolean();
        if (control.toggleAerobaticSmoke) {
            control.aerobaticSmokeR = buf.readInt();
            control.aerobaticSmokeG = buf.readInt();
            control.aerobaticSmokeB = buf.readInt();
            return control;
        }
        control.togglePartUnitState = buf.readBoolean();
        if (control.togglePartUnitState) {
            control.partUnitIndex = buf.readInt();
            return control;
        }
        control.lockEntity = buf.readBoolean();
        if (control.lockEntity) {
            control.lockedEntityId = buf.readInt();
            return control;
        }
        control.partUnitIndex = buf.readInt();
        control.shoot = buf.readBoolean();
        if (control.shoot) {
            int ammoCount = buf.readInt();
            for (int index = 0; index < ammoCount; index += 1) {
                AimContext aimContext = new AimContext();
                aimContext.from = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
                aimContext.direction = new Vec2(buf.readFloat(), buf.readFloat());
                aimContext.position = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
                control.aimContexts.add(aimContext);
            }
            control.weaponIndex = buf.readInt();
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
        buf.writeBoolean(toggleEngine);
        if (toggleEngine) {
            return;
        }
        buf.writeBoolean(toggleLandingGear);
        if (toggleLandingGear) {
            return;
        }
        buf.writeBoolean(toggleAirbrake);
        if (toggleAirbrake) {
            return;
        }
        buf.writeBoolean(toggleHoverMode);
        if (toggleHoverMode) {
            return;
        }
        buf.writeBoolean(toggleAerobaticSmoke);
        if (toggleAerobaticSmoke) {
            buf.writeInt(aerobaticSmokeR);
            buf.writeInt(aerobaticSmokeG);
            buf.writeInt(aerobaticSmokeB);
            return;
        }
        buf.writeBoolean(togglePartUnitState);
        if (togglePartUnitState) {
            buf.writeInt(partUnitIndex);
            return;
        }
        buf.writeBoolean(lockEntity);
        if (lockEntity) {
            buf.writeInt(lockedEntityId);
            return;
        }
        buf.writeInt(partUnitIndex);
        buf.writeBoolean(shoot);
        if (shoot) {
            int ammoCount = aimContexts.size();
            buf.writeInt(ammoCount);
            for (int index = 0; index < ammoCount; index += 1) {
                AimContext aimContext = aimContexts.get(index);
                buf.writeFloat((float) aimContext.from.x);
                buf.writeFloat((float) aimContext.from.y);
                buf.writeFloat((float) aimContext.from.z);
                buf.writeFloat(aimContext.direction.x);
                buf.writeFloat(aimContext.direction.y);
                buf.writeFloat((float) aimContext.position.x);
                buf.writeFloat((float) aimContext.position.y);
                buf.writeFloat((float) aimContext.position.z);
            }
            buf.writeInt(weaponIndex);
        } else {
            buf.writeFloat(xAimRot);
            buf.writeFloat(yAimRot);
        }
    }

    public static void handle(ClientVehicleAction message, IPayloadContext ctx) {
        Player player = ctx.player();
        Level level = player.level();
        Entity entity = level.getEntity(message.vehicleEntityId);
        if (!(entity instanceof AbstractVehicle vehicle)) {
            return;
        }
        if (message.togglePartUnitState) {
            if (message.partUnitIndex < vehicle.getPartUnits().size()
                    && vehicle.getPartUnits().get(message.partUnitIndex) instanceof SwitchableUnit<?> switchableUnit) {
                switchableUnit.setOn(!switchableUnit.isOn());
            }
        } else if (message.leaveVehicle || message.toggleEngine || message.toggleLandingGear || message.toggleAirbrake || message.toggleHoverMode || message.toggleAerobaticSmoke || message.lockEntity) {
            vehicle.onClientVehicleAction(message, player);
        } else if (message.partUnitIndex < vehicle.getPartUnits().size()) {
            vehicle.getPartUnits().get(message.partUnitIndex).onClientMessageReceived(message, player);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
