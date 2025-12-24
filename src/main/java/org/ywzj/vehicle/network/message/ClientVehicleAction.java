package org.ywzj.vehicle.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.custom.part.data.WeaponUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.pojo.AimContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientVehicleAction {

    public int vehicleEntityId;
    public boolean leaveVehicle;
    public boolean toggleEngine;
    public boolean lockEntity;
    public int lockedEntityId;
    public WeaponUnitData.FireControlSensorType sensorType;
    public int partUnitIndex;
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
        control.lockEntity = buf.readBoolean();
        if (control.lockEntity) {
            control.lockedEntityId = buf.readInt();
            control.sensorType = buf.readEnum(WeaponUnitData.FireControlSensorType.class);
            return control;
        }
        control.partUnitIndex = buf.readInt();
        control.shoot = buf.readBoolean();
        if (control.shoot) {
            int ammoCount = buf.readInt();
            for (int index = 0; index < ammoCount; index += 1) {
                AimContext aimContext = new AimContext();
                aimContext.position = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
                aimContext.direction = new Vec2(buf.readFloat(), buf.readFloat());
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
        buf.writeBoolean(lockEntity);
        if (lockEntity) {
            buf.writeInt(lockedEntityId);
            buf.writeEnum(sensorType);
            return;
        }
        buf.writeInt(partUnitIndex);
        buf.writeBoolean(shoot);
        if (shoot) {
            int ammoCount = aimContexts.size();
            buf.writeInt(ammoCount);
            for (int index = 0; index < ammoCount; index += 1) {
                AimContext aimContext = aimContexts.get(index);
                buf.writeFloat((float) aimContext.position.x);
                buf.writeFloat((float) aimContext.position.y);
                buf.writeFloat((float) aimContext.position.z);
                buf.writeFloat(aimContext.direction.x);
                buf.writeFloat(aimContext.direction.y);
            }
            buf.writeInt(weaponIndex);
        } else {
            buf.writeFloat(xAimRot);
            buf.writeFloat(yAimRot);
        }
    }

    public static void onClientMessageReceived(ClientVehicleAction message, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            if (message.leaveVehicle || message.toggleEngine || message.lockEntity) {
                AbstractVehicle.onClientVehicleAction(message, ctxSupplier);
            } else {
                PartUnit.onClientMessageReceived(message, ctxSupplier);
            }
        });
    }

}
