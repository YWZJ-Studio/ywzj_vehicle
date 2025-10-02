package org.ywzj.vehicle.vehicle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ClientVehicleMoveControl;

import java.util.function.Supplier;

public class ControlUnit {

    public LivingEntity operator;
    public boolean forward;
    public boolean backward;
    public boolean left;
    public boolean right;
    public boolean up;
    public boolean down;
    public float xRot;
    public float yRot;

    public void setOperator(LivingEntity operator) {
        this.operator = operator;
    }

    public void reset() {
        forward = false;
        backward = false;
        left = false;
        right = false;
        up = false;
        down = false;
        xRot = 0;
        yRot = 0;
    }

    public static void onClientMessageReceived(ClientVehicleMoveControl message, Supplier<NetworkEvent.Context> ctxSupplier) {
        if (ctxSupplier.get().getSender() != null) {
            Level level = ctxSupplier.get().getSender().level();
            Entity entity = level.getEntity(message.vehicleEntityId);
            if (entity instanceof AbstractVehicle vehicle) {
                if (ctxSupplier.get().getSender() != vehicle.controlUnit.operator) {
                    return;
                }
                vehicle.controlUnit.forward = message.forward;
                vehicle.controlUnit.backward = message.backward;
                vehicle.controlUnit.left = message.left;
                vehicle.controlUnit.right = message.right;
                vehicle.controlUnit.up = message.up;
                vehicle.controlUnit.down = message.down;
                vehicle.controlUnit.xRot = message.xRot;
                vehicle.controlUnit.yRot = message.yRot;
            }
        }
    }

}
