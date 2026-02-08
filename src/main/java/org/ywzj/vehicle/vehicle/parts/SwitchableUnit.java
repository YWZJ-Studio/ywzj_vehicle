package org.ywzj.vehicle.vehicle.parts;

import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.PauseState;
import com.maydaymemory.mae.control.runner.PlayingState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.ywzj.vehicle.api.custom.sync.SyncDataSerializers;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

public class SwitchableUnit<T extends PartUnitData> extends PartUnit<T> {

    private boolean on;
    private boolean animationOn;
    private AnimationRunner switchRunner;

    public SwitchableUnit(int index, AbstractVehicle vehicle, T data) {
        super(index, vehicle, data);
//        if (vehicle.level().isClientSide()) {
//            EntityContext<?> context = vehicle.getAnimationInstance().getStateMachine().getContext();
//            if (context != null) {
//                PlayingState playingState = new PlayingState(System::nanoTime, PauseState::new);
//                playingState.setSpeed(-1);
//                switchRunner = context.addAnimationRunner(getId(), playingState);
//            }
//        }
        this.getSyncData().define(SyncDataSerializers.BOOLEAN, this::setOn, this::isOn, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (vehicle.level().isClientSide) {
            if (animationOn != on) {
                if (switchRunner != null && switchRunner.getState() instanceof PauseState) {
                    PlayingState playingState = new PlayingState(System::nanoTime, PauseState::new);
                    if (animationOn) {
                        playingState.setSpeed(-1);
                        animationOn = false;
                        vehicle.level().playSound(LocalVehiclePlayer.instance.getPlayer(), vehicle.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 1f, 1f);
                    } else {
                        playingState.setSpeed(1);
                        animationOn = true;
                        vehicle.level().playSound(LocalVehiclePlayer.instance.getPlayer(), vehicle.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1f, 1f);
                    }
                    switchRunner.setState(playingState);
                }
            }
        }
    }

    public boolean onEntityInteract(Player player, InteractionHand hand) {
        if (!vehicle.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (!on) {
                on = true;
                return false;
            } else {
                on = false;
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
        if (vehicle.level().isClientSide()) {
            this.animationOn = this.on;
            if (switchRunner.getState() instanceof PlayingState playingState) {
                if (!animationOn) {
                    playingState.setSpeed(-1);
                } else {
                    playingState.setSpeed(1);
                    switchRunner.setProgress(switchRunner.getMaxProgress());
                }
            }
        }
    }

}
