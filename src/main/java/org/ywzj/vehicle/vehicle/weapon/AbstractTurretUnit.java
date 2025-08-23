package org.ywzj.vehicle.vehicle.weapon;

import net.minecraft.util.Mth;
import net.minecraftforge.network.PacketDistributor;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerWeaponUnitRot;

public abstract class AbstractTurretUnit<T> extends AbstractWeaponUnit<T> {
    public float xAimRot;
    public float yAimRot;
    public float xRot;
    public float yRot;
    public float xRotO;
    public float yRotO;
    public float xRotSpeed;
    public float yRotSpeed;
    public float xRotMax;
    public float xRotMin;

    protected AbstractTurretUnit(AbstractVehicle vehicle, int index, T data) {
        super(vehicle, index, data);
    }

    public void tick() {
        this.xRotO = this.xRot;
        this.yRotO = this.yRot;

        float xDiff = Mth.wrapDegrees(this.xAimRot - this.xRot);
        float yDiff = Mth.wrapDegrees(this.yAimRot - this.yRot);

        if (Math.abs(xDiff) > xRotSpeed) {
            this.xRot += Math.signum(xDiff) * xRotSpeed;
        } else {
            this.xRot = this.xAimRot;
        }
        this.xRot = Math.max(Math.min(this.xRot, xRotMax), xRotMin);

        if (Math.abs(yDiff) > yRotSpeed) {
            this.yRot += Math.signum(yDiff) * yRotSpeed;
        } else {
            this.yRot = this.yAimRot;
        }

        if (!this.getVehicle().level().isClientSide()) {
            if (xDiff != 0 || yDiff != 0) {
                var packet = new ServerWeaponUnitRot(this);
                Channel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(this::getVehicle), packet);
            }
        }
    }
}
