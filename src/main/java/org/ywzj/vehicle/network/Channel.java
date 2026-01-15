package org.ywzj.vehicle.network;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.audio.SoundManager;
import org.ywzj.vehicle.network.message.*;

import java.util.Optional;

import static net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT;
import static net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER;

public class Channel {

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(YwzjVehicle.modLocation(YwzjVehicle.CHANNEL))
            .networkProtocolVersion(() -> YwzjVehicle.PROTOCOL)
            .clientAcceptedVersions(YwzjVehicle.PROTOCOL::equals)
            .serverAcceptedVersions(YwzjVehicle.PROTOCOL::equals)
            .simpleChannel();

    @SubscribeEvent
    public static void onCommonSetupEvent(FMLCommonSetupEvent event) {

        CHANNEL.registerMessage(PacketId.C_VEHICLE_CONTROL.value(), ClientVehicleMoveControl.class,
                ClientVehicleMoveControl::encode, ClientVehicleMoveControl::decode,
                ClientVehicleMoveControl::onClientMessageReceived,
                Optional.of(PLAY_TO_SERVER));

        CHANNEL.registerMessage(PacketId.C_WEAPON_UNIT_CONTROL.value(), ClientVehicleAction.class,
                ClientVehicleAction::encode, ClientVehicleAction::decode,
                ClientVehicleAction::onClientMessageReceived,
                Optional.of(PLAY_TO_SERVER));

        CHANNEL.registerMessage(PacketId.S_VEHICLE_SEATS_CHANGE.value(), ServerVehicleSeatsChange.class,
                ServerVehicleSeatsChange::encode, ServerVehicleSeatsChange::decode,
                ServerVehicleSeatsChange::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.C_VEHICLE_CHANGE_SEAT.value(), ClientVehicleChangeSeat.class,
                ClientVehicleChangeSeat::encode, ClientVehicleChangeSeat::decode,
                ClientVehicleChangeSeat::onClientMessageReceived,
                Optional.of(PLAY_TO_SERVER));

        CHANNEL.registerMessage(PacketId.S_SOUND_EVENT.value(), ServerSoundEvent.class,
                ServerSoundEvent::encode, ServerSoundEvent::decode,
                SoundManager::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.S_HIT_VEHICLE_EVENT.value(), ServerHitVehicleEvent.class,
                ServerHitVehicleEvent::encode, ServerHitVehicleEvent::decode,
                ServerHitVehicleEvent::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));


        CHANNEL.registerMessage(PacketId.S_ENTITY_SYNC_DATA.value(), ServerEntityDataUpdate.class,
                ServerEntityDataUpdate::encode, ServerEntityDataUpdate::decode,
                ServerEntityDataUpdate::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.C_VEHICLE_SWITCH_WEAPON.value(), ClientVehicleSwitchWeapon.class,
                ClientVehicleSwitchWeapon::encode, ClientVehicleSwitchWeapon::decode,
                ClientVehicleSwitchWeapon::onReceived,
                Optional.of(PLAY_TO_SERVER));

        CHANNEL.registerMessage(PacketId.S_VEHICLE_FIRE.value(), ServerVehicleFire.class,
                ServerVehicleFire::encode, ServerVehicleFire::decode,
                ServerVehicleFire::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.S_VEHICLE_WARN.value(), ServerVehicleWarn.class,
                ServerVehicleWarn::encode, ServerVehicleWarn::decode,
                ServerVehicleWarn::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.S_VEHICLE_HURT_ENTITY.value(), ServerVehicleHurtEntity.class,
                ServerVehicleHurtEntity::encode, ServerVehicleHurtEntity::decode,
                ServerVehicleHurtEntity::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.C_MACHINE_MAX_ACTION.value(), ClientMachineMaxAction.class,
                ClientMachineMaxAction::encode, ClientMachineMaxAction::decode,
                ClientMachineMaxAction::onClientMessageReceived,
                Optional.of(PLAY_TO_SERVER));

        CHANNEL.registerMessage(PacketId.C_FIGURE_BOX_UPDATE.value(), ClientFigureBoxUpdate.class,
                ClientFigureBoxUpdate::encode, ClientFigureBoxUpdate::decode,
                ClientFigureBoxUpdate::onClientMessageReceived,
                Optional.of(PLAY_TO_SERVER));

        CHANNEL.registerMessage(PacketId.S_SLICED_PACKET.value(), ServerSlicedPacket.class,
                ServerSlicedPacket::encode, ServerSlicedPacket::decode,
                ServerSlicedPacket::handle,
                Optional.of(PLAY_TO_CLIENT));
    }

}

enum PacketId {

    C_VEHICLE_CONTROL(100),
    C_WEAPON_UNIT_CONTROL(101),
    S_VEHICLE_SEATS_CHANGE(102),
    C_VEHICLE_CHANGE_SEAT(103),
    C_VEHICLE_SWITCH_WEAPON(104),
    S_VEHICLE_FIRE(105),
    S_VEHICLE_WARN(106),
    S_VEHICLE_HURT_ENTITY(107),
    C_MACHINE_MAX_ACTION(108),
    C_FIGURE_BOX_UPDATE(109),

    S_SLICED_PACKET(110),

    S_ENTITY_SYNC_DATA(120),

    S_SYNC_WEAPON_DATA(150),
    S_SYNC_VEHICLE_DATA(151),
    S_SOUND_EVENT(201),
    S_HIT_VEHICLE_EVENT(202);

    private final int id;

    PacketId(int id) {
        this.id = id;
    }

    int value() {
        return id;
    }

}
