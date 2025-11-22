package org.ywzj.vehicle.vehicle.control;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.message.ClientVehicleMoveControl;

import java.util.function.Supplier;

public class ControlUnit {

    private final AbstractVehicle vehicle;
    private LivingEntity operator;
    private int operatorId;
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
    public boolean xRotKeep;
    public float yRot;
    public boolean yRotKeep;

    public ControlUnit(AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public LivingEntity getOperator() {
        if (operator == null && operatorId != -1) {
            if (vehicle.level().getEntity(operatorId) instanceof LivingEntity livingEntity) {
                operator = livingEntity;
            }
        }
        return operator;
    }

    public void setOperator(LivingEntity operator) {
        if (operator == null) {
            this.operator = null;
            this.operatorId = -1;
        } else {
            this.operator = operator;
            this.operatorId = operator.getId();
        }
    }

    public void setOperatorId(int operatorId) {
        this.operatorId = operatorId;
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
        xRotKeep = false;
        yRot = 0;
        yRotKeep = false;
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
        xRotKeep = controlUnit.xRotKeep;
        yRot = controlUnit.yRot;
        yRotKeep = controlUnit.yRotKeep;
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
                vehicle.controlUnit.xRotKeep = message.xRotKeep;
                vehicle.controlUnit.yRot = message.yRot;
                vehicle.controlUnit.yRotKeep = message.yRotKeep;
            }
        }
    }

}
