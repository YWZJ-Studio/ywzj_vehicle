package org.ywzj.vehicle.entity.vehicle.custom;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.audio.VehicleSound;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.WheeledVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientVehicleAction;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.part.RotatableUnit;

public class DumpTruck extends WheeledVehicle {

    private VehicleSound bedTurnSoundInstance;

    public DumpTruck(EntityType<? extends AbstractVehicle> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (getDriver() != null) {
                if (controlUnit.leftYaw || controlUnit.rightYaw) {
                    RotatableUnit bed = (RotatableUnit) partUnits.get(1);
                    if (controlUnit.leftYaw) {
                        bed.setXAimRot(bed.getXAimRot() - 5);
                    } else {
                        bed.setXAimRot(bed.getXAimRot() + 5);
                    }
                    bed.setXAimRot(Mth.clamp(bed.getXAimRot(), bed.xRotMin, bed.xRotMax));
                    ClientVehicleAction control = new ClientVehicleAction();
                    control.vehicleEntityId = this.getId();
                    control.partUnitIndex = bed.getIndex();
                    control.xAimRot = bed.getXAimRot();
                    control.yAimRot = 0;
                    Channel.CHANNEL.sendToServer(control);
                }
            }
        }
    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        super.interact(pPlayer, pHand);
        if (!this.level().isClientSide() && pPlayer.isShiftKeyDown()) {
            Vec3 eyePosition = pPlayer.getEyePosition();
            PartUnit<?> partUnit = VectorUtil.hitPartUnit(this, eyePosition, eyePosition.add(pPlayer.getLookAngle().scale(4)));
            if (partUnit == partUnitMap.get("dump_truck_bed")) {
                Vec3 bedPos = relativeRotPos(position().add(0, 5, 0), true);
                pPlayer.teleportTo(bedPos.x, bedPos.y, bedPos.z);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void tickSound() {
        super.tickSound();
        RotatableUnit bed = (RotatableUnit) partUnits.get(1);
        if (Math.abs(bed.getXAimRot() - bed.getXRot()) > 1 && bed.getXRot() < bed.xRotMax && bed.getXRot() > bed.xRotMin) {
            if (bedTurnSoundInstance == null) {
                bedTurnSoundInstance = new VehicleSound(AllSounds.TURRET_TURN_SERVO_V.get(), 1f, 1f, 1f, true, 10, true, true, this.getId());
                bedTurnSoundInstance.play();
            }
        } else {
            if (bedTurnSoundInstance != null) {
                bedTurnSoundInstance.stop();
                bedTurnSoundInstance = null;
            }
        }
    }

}
