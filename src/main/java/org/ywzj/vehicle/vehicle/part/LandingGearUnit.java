package org.ywzj.vehicle.vehicle.part;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.UnmodifiableView;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.Map;

public class LandingGearUnit extends SwitchableUnit<PartUnitData> {

    private double maxHeight;
    private double vehicleToHeight;
    private boolean changing;

    public LandingGearUnit(int index, AbstractVehicle vehicle, PartUnitData data) {
        super(index, vehicle, data);
    }

    @Override
    public void combineAndInit(@UnmodifiableView Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        super.combineAndInit(partUnitsView, vehicle);
        maxHeight = this.getOBBs().stream()
                .mapToDouble(obb -> obb.extents().y * 2)
                .max()
                .orElse(0);
        vehicleToHeight = vehicle.getMainCubeOBB().height;
    }

    @Override
    public boolean onInteract(Player player, InteractionHand hand) {
        return true;
    }

    @Override
    public void setOn(boolean on) {
        if (update(on)) {
            // on -> true，起落架 -> 收起
            if (vehicle.getDriver() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable(on ? "tips.gear_up" : "tips.gear_down"), true);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (changing) {
            VehicleCubeOBB mainCubeOBB = vehicle.getMainCubeOBB();
            double heightStep = maxHeight / 40;
            if (this.on) {
                mainCubeOBB.height -= heightStep;
                mainCubeOBB.y += heightStep;
                if (mainCubeOBB.height <= vehicleToHeight) {
                    changing = false;
                }
            } else {
                mainCubeOBB.height += heightStep;
                mainCubeOBB.y -= heightStep;
                if (mainCubeOBB.height >= vehicleToHeight) {
                    changing = false;
                }
            }
            mainCubeOBB.rebuild();
        }
    }

    public boolean update(boolean newState) {
        if (changing) {
            return false;
        }
        if (newState != this.on) {
            vehicleToHeight = vehicle.getMainCubeOBB().height + (newState ? -1 : 1) * maxHeight;
            changing = true;
        }
        this.on = newState;
        return true;
    }

    public double level() {
        double progress = Math.abs((vehicleToHeight - vehicle.getMainCubeOBB().height)) / maxHeight;
        if (on) {
            return progress;
        } else {
            return 1 - progress;
        }
    }

    public double getMaxHeight() {
        return maxHeight;
    }

}
