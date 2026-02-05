package org.ywzj.vehicle.vehicle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerVehicleSeatsChange;
import org.ywzj.vehicle.vehicle.parts.DoorUnit;
import org.ywzj.vehicle.vehicle.parts.PartUnit;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeatManager {
    private final AbstractVehicle vehicle;
    private final List<AbstractVehicle.Seat> seats;

    public SeatManager(AbstractVehicle vehicle, List<AbstractVehicle.Seat> seats) {
        this.vehicle = vehicle;
        this.seats = seats;
    }

    public void onEnter(LivingEntity entity) {
        if (vehicle.level().isClientSide()) return;

        Optional<AbstractVehicle.Seat> emptySeat = seats.stream()
                .filter(s -> s.passengerId == -1)
                .findFirst();

        emptySeat.ifPresent(seat -> {
            if (seat.seatIndex == 0) {
                vehicle.controlUnit.setOperator(entity);
                vehicle.toggleEngine(true);
                vehicle.getPartUnits().stream()
                        .filter(p -> p instanceof DoorUnit)
                        .forEach(p -> ((DoorUnit) p).setOn(false));
            }
            seat.partUnit.setOwner(entity);
            seat.passengerId = entity.getId();
            syncSeats();
        });
        entity.setSprinting(false);
    }

    public void onLeave(LivingEntity entity) {
        if (vehicle.level().isClientSide()) return;

        seats.stream()
                .filter(s -> s.passengerId == entity.getId())
                .findFirst()
                .ifPresent(seat -> {
                    if (seat.seatIndex == 0) vehicle.controlUnit.setOperator(null);
                    seat.partUnit.setOwner(null);
                    seat.passengerId = -1;
                    syncSeats();
                });
    }

    public boolean changeSeat(LivingEntity entity, int toIndex) {
        if (toIndex >= seats.size() || seats.get(toIndex).passengerId != -1) return false;

        onLeave(entity);

        AbstractVehicle.Seat toSeat = seats.get(toIndex);
        if (toSeat.seatIndex == 0) {
            vehicle.controlUnit.setOperator(entity);
            vehicle.toggleEngine(true);
        }
        toSeat.partUnit.setOwner(entity);
        toSeat.passengerId = entity.getId();
        toSeat.partUnit.applySeatRot(entity);
        syncSeats();
        return true;
    }

    private void syncSeats() {
        if (vehicle.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ServerVehicleSeatsChange(vehicle));
            }
        }
    }

    public void setSeatsClient(int[] ids) {
        Player player = LocalVehiclePlayer.instance.getPlayer();
        List<Integer> idList = new ArrayList<>();
        for (int id : ids) idList.add(id);

        boolean wasPassenger = seats.stream().anyMatch(s -> s.passengerId == player.getId());
        if (wasPassenger && !idList.contains(player.getId())) {
            LocalVehiclePlayer.instance.switchViewType(LocalVehiclePlayer.ViewType.THIRD_PERSON);
        }

        for (int i = 0; i < seats.size() && i < ids.length; i++) {
            AbstractVehicle.Seat seat = seats.get(i);
            int id = ids[i];
            if (id != -1) {
                if (i == 0) vehicle.controlUnit.setOperatorId(id);
                seat.partUnit.setOwnerId(id);
                seat.passengerId = id;
                if (id == player.getId()) seat.partUnit.applySeatRot(player);
            } else {
                if (i == 0) vehicle.controlUnit.setOperator(null);
                seat.partUnit.setOwner(null);
                seat.passengerId = -1;
            }
        }
    }
}
