package org.ywzj.vehicle.network;

import net.minecraft.resources.ResourceLocation;
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
            .named(new ResourceLocation(YwzjVehicle.MOD_ID, YwzjVehicle.CHANNEL))
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

        CHANNEL.registerMessage(PacketId.S_ROTATABLE_UNIT_ROT.value(), ServerRotatableUnitRot.class,
                ServerRotatableUnitRot::encode, ServerRotatableUnitRot::decode,
                ServerRotatableUnitRot::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.C_VEHICLE_CHANGE_SEAT.value(), ClientVehicleChangeSeat.class,
                ClientVehicleChangeSeat::encode, ClientVehicleChangeSeat::decode,
                ClientVehicleChangeSeat::onClientMessageReceived,
                Optional.of(PLAY_TO_SERVER));

        CHANNEL.registerMessage(PacketId.S_SYNC_DATA.value(), ServerSyncData.class,
                ServerSyncData::encode, ServerSyncData::decode,
                ServerSyncData::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.S_SOUND_EVENT.value(), ServerSoundEvent.class,
                ServerSoundEvent::encode, ServerSoundEvent::decode,
                SoundManager::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.S_HIT_VEHICLE_EVENT.value(), ServerHitVehicleEvent.class,
                ServerHitVehicleEvent::encode, ServerHitVehicleEvent::decode,
                ServerHitVehicleEvent::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));

        CHANNEL.registerMessage(PacketId.S_VEHICLE_WEAPON_SYNC_DATA.value(), ServerVehicleWeaponSync.class,
                ServerVehicleWeaponSync::encode, ServerVehicleWeaponSync::decode,
                ServerVehicleWeaponSync::onServerMessageReceived,
                Optional.of(PLAY_TO_CLIENT));
    }

}

enum PacketId {

    C_VEHICLE_CONTROL(100),
    C_WEAPON_UNIT_CONTROL(101),
    S_VEHICLE_SEATS_CHANGE(102),
    S_ROTATABLE_UNIT_ROT(103),
    C_VEHICLE_CHANGE_SEAT(104),
    S_VEHICLE_WEAPON_SYNC_DATA(105),

    S_SYNC_DATA(200),
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
