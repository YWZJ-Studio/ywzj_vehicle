package org.ywzj.vehicle.vehicle.control;

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
    public boolean leftYaw;
    public boolean rightYaw;
    public boolean functionalUp;
    public boolean functionalDown;
    public boolean functionalLeft;
    public boolean functionalRight;
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
        leftYaw = false;
        rightYaw = false;
        functionalUp = false;
        functionalDown = false;
        functionalLeft = false;
        functionalRight = false;
        xRot = 0;
        yRot = 0;
    }

    public void update(ControlUnit controlUnit) {
        forward = controlUnit.forward;
        backward = controlUnit.backward;
        left = controlUnit.left;
        right = controlUnit.right;
        up = controlUnit.up;
        down = controlUnit.down;
        leftYaw = controlUnit.leftYaw;
        rightYaw = controlUnit.rightYaw;
        functionalUp = controlUnit.functionalUp;
        functionalDown = controlUnit.functionalDown;
        functionalLeft = controlUnit.functionalLeft;
        functionalRight = controlUnit.functionalRight;
        xRot = controlUnit.xRot;
        yRot = controlUnit.yRot;
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
                vehicle.controlUnit.leftYaw = message.leftYaw;
                vehicle.controlUnit.rightYaw = message.rightYaw;
                vehicle.controlUnit.functionalUp = message.functionalUp;
                vehicle.controlUnit.functionalDown = message.functionalDown;
                vehicle.controlUnit.functionalLeft = message.functionalLeft;
                vehicle.controlUnit.functionalRight = message.functionalRight;
                vehicle.controlUnit.xRot = message.xRot;
                vehicle.controlUnit.yRot = message.yRot;
            }
        }
    }

}
