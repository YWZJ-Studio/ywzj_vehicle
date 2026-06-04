package org.ywzj.vehicle.vehicle.part;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class WeaponBayUnit extends SwitchableUnit<PartUnitData> {

    public WeaponBayUnit(int index, AbstractVehicle vehicle, PartUnitData data) {
        super(index, vehicle, data);
    }

    @Override
    public void setOn(boolean on) {
        super.setOn(on);
        if (vehicle.getDriver() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(on ? "tips.bay_open" : "tips.bay_close"), true);
        }
    }

}
