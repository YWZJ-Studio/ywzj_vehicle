package org.ywzj.vehicle.vehicle.parts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class SwitchableUnit<T extends PartUnitData> extends PartUnit<T> {

    private boolean on;

    public SwitchableUnit(int index, AbstractVehicle vehicle, T data) {
        super(index, vehicle, data);
        this.getSyncData().define(SyncDataSerializers.BOOLEAN, this::setOn, this::isOn, false);
    }

    @Override
    public void tick() {
        super.tick();
    }

    public boolean onEntityInteract(Player player, InteractionHand hand) {
        if (!vehicle.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (!on) {
                this.on = true;
                return false;
            } else {
                this.on = false;
                return !player.isShiftKeyDown();
            }
        }
        return true;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putBoolean("on", this.on);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        this.on = nbt.getBoolean("on");
    }

    public boolean defaultOpen() {
        return false;
    }
}
