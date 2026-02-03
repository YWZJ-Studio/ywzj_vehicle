package org.ywzj.vehicle.vehicle.parts;

import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.PauseState;
import com.maydaymemory.mae.control.runner.PlayingState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.UnmodifiableView;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.client.render.animation.EntityContext;
import org.ywzj.vehicle.custom.part.data.DoorUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.Map;

public class DoorUnit extends PartUnit<DoorUnitData> {

    private PartUnit<?> seatUnitOfDoor;
    private boolean open;
    private boolean animationOpen;
    private AnimationRunner doorMoveRunner;

    public DoorUnit(int index, AbstractVehicle vehicle, DoorUnitData data) {
        super(index, vehicle, data);
        if (vehicle.level().isClientSide()) {
            EntityContext<?> context = vehicle.getAnimationInstance().getStateMachine().getContext();
            if (context != null) {
                PlayingState playingState = new PlayingState(System::nanoTime, PauseState::new);
                playingState.setSpeed(-1);
                doorMoveRunner = context.addAnimationRunner(getId(), playingState);
            }
        }
        this.getSyncData().define(SyncDataSerializers.BOOLEAN, this::setOpen, this::isOpen, false);
    }

    public void combineAndInit(@UnmodifiableView Map<String, PartUnit<?>> partUnitsView, AbstractVehicle vehicle) {
        this.seatUnitOfDoor = partUnitsView.get(data.getDoorForSeatId());
    }

    @Override
    public void tick() {
        super.tick();
        if (vehicle.level().isClientSide) {
            if (animationOpen != open) {
                if (doorMoveRunner != null && doorMoveRunner.getState() instanceof PauseState) {
                    PlayingState playingState = new PlayingState(System::nanoTime, PauseState::new);
                    if (animationOpen) {
                        playingState.setSpeed(-1);
                        animationOpen = false;
                        vehicle.level().playSound(LocalVehiclePlayer.instance.getPlayer(), vehicle.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 1f, 1f);
                    } else {
                        playingState.setSpeed(1);
                        animationOpen = true;
                        vehicle.level().playSound(LocalVehiclePlayer.instance.getPlayer(), vehicle.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1f, 1f);
                    }
                    doorMoveRunner.setState(playingState);
                }
            }
        }
    }

    public boolean onEntityInteract(Player player, InteractionHand hand) {
        if (!vehicle.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (!open) {
                open = true;
                return false;
            } else {
                open = false;
                return !player.isShiftKeyDown();
            }
        }
        return true;
    }

    public PartUnit<?> getSeatUnitOfDoor() {
        return seatUnitOfDoor;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putBoolean("open", this.open);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        this.open = nbt.getBoolean("open");
        if (vehicle.level().isClientSide()) {
            this.animationOpen = this.open;
            if (doorMoveRunner.getState() instanceof PlayingState playingState) {
                if (!animationOpen) {
                    playingState.setSpeed(-1);
                } else {
                    playingState.setSpeed(1);
                    doorMoveRunner.setProgress(doorMoveRunner.getMaxProgress());
                }
            }
        }
    }

}
