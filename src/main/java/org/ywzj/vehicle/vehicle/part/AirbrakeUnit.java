package org.ywzj.vehicle.vehicle.part;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.ywzj.vehicle.custom.part.data.AirbrakeUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class AirbrakeUnit extends SwitchableUnit<AirbrakeUnitData> {

    private double level;
    private boolean changing;

    public AirbrakeUnit(int index, AbstractVehicle vehicle, AirbrakeUnitData data) {
        super(index, vehicle, data);
    }

    @Override
    public void setOn(boolean on) {
        if (update(on)) {
            if (vehicle.getDriver() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(on ? "tips.airbrake_on" : "tips.airbrake_off"), true);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (changing) {
            double step = 1.0 / 20;
            if (this.on) {
                level += step;
                if (level >= 1.0) {
                    level = 1.0;
                    changing = false;
                }
            } else {
                level -= step;
                if (level <= 0.0) {
                    level = 0.0;
                    changing = false;
                }
            }
        }
    }

    public boolean update(boolean newState) {
        if (changing) {
            return false;
        }
        if (newState != this.on) {
            changing = true;
        }
        this.on = newState;
        return true;
    }

    public double level() {
        return level;
    }

    public float getDragK() {
        return data.getDragK();
    }

}
