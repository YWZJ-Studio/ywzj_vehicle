package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.PartUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientVehicleAction {

    public int vehicleEntityId;
    public boolean leaveVehicle;
    public boolean toggleEngine;
    public int partUnitIndex;
    public boolean shoot;
    public List<Vec3> ammoSpawnPositions = new ArrayList<>();
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
        control.toggleEngine = buf.readBoolean();
        if (control.toggleEngine) {
            return control;
        }
        control.partUnitIndex = buf.readInt();
        control.shoot = buf.readBoolean();
        if (control.shoot) {
            int ammoCount = buf.readInt();
            for (int index = 0; index < ammoCount; index += 1) {
                control.ammoSpawnPositions.add(new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));
            }
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
        buf.writeBoolean(toggleEngine);
        if (toggleEngine) {
            return;
        }
        buf.writeInt(partUnitIndex);
        buf.writeBoolean(shoot);
        if (shoot) {
            int ammoCount = ammoSpawnPositions.size();
            buf.writeInt(ammoCount);
            for (int index = 0; index < ammoCount; index += 1) {
                Vec3 ammoSpawnPosition = ammoSpawnPositions.get(index);
                buf.writeFloat((float) ammoSpawnPosition.x);
                buf.writeFloat((float) ammoSpawnPosition.y);
                buf.writeFloat((float) ammoSpawnPosition.z);
            }
            buf.writeFloat(ammoXRot);
            buf.writeFloat(ammoYRot);
        } else {
            buf.writeFloat(xAimRot);
            buf.writeFloat(yAimRot);
        }
    }

    public static void onClientMessageReceived(ClientVehicleAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            if (message.leaveVehicle || message.toggleEngine) {
                AbstractVehicle.onClientVehicleAction(message, ctxSupplier);
            } else {
                PartUnit.onClientMessageReceived(message, ctxSupplier);
            }
        });
    }

}
