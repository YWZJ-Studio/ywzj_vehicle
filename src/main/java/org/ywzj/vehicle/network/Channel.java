package org.ywzj.vehicle.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.audio.SoundManager;
import org.ywzj.vehicle.network.message.*;

public class Channel {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(YwzjVehicle.PROTOCOL);
        registrar.playToServer(ClientVehicleMoveControl.TYPE, ClientVehicleMoveControl.STREAM_CODEC, ClientVehicleMoveControl::handle);
        registrar.playToServer(ClientVehicleAction.TYPE, ClientVehicleAction.STREAM_CODEC, ClientVehicleAction::handle);
        registrar.playToClient(ServerVehicleSeatsChange.TYPE, ServerVehicleSeatsChange.STREAM_CODEC, ServerVehicleSeatsChange::handle);
        registrar.playToServer(ClientVehicleChangeSeat.TYPE, ClientVehicleChangeSeat.STREAM_CODEC, ClientVehicleChangeSeat::handle);
        registrar.playToClient(ServerSoundEvent.TYPE, ServerSoundEvent.STREAM_CODEC, SoundManager::onServerMessageReceived);
        registrar.playToClient(ServerHitVehicleEvent.TYPE, ServerHitVehicleEvent.STREAM_CODEC, ServerHitVehicleEvent::handle);
        registrar.playToClient(ServerEntityDataUpdate.TYPE, ServerEntityDataUpdate.STREAM_CODEC, ServerEntityDataUpdate::handle);
        registrar.playToServer(ClientVehicleSwitchWeapon.TYPE, ClientVehicleSwitchWeapon.STREAM_CODEC, ClientVehicleSwitchWeapon::handle);
        registrar.playToClient(ServerVehicleFire.TYPE, ServerVehicleFire.STREAM_CODEC, ServerVehicleFire::handle);
        registrar.playToClient(ServerVehicleWarn.TYPE, ServerVehicleWarn.STREAM_CODEC, ServerVehicleWarn::handle);
        registrar.playToClient(ServerVehicleHurtEntity.TYPE, ServerVehicleHurtEntity.STREAM_CODEC, ServerVehicleHurtEntity::handle);
        registrar.playToServer(ClientMachineMaxAction.TYPE, ClientMachineMaxAction.STREAM_CODEC, ClientMachineMaxAction::handle);
        registrar.playToServer(ClientFigureBoxUpdate.TYPE, ClientFigureBoxUpdate.STREAM_CODEC, ClientFigureBoxUpdate::handle);
        registrar.playToClient(ServerVehicleChangeDisplay.TYPE, ServerVehicleChangeDisplay.STREAM_CODEC, ServerVehicleChangeDisplay::handle);
        registrar.playToServer(ClientVehicleChangeDisplay.TYPE, ClientVehicleChangeDisplay.STREAM_CODEC, ClientVehicleChangeDisplay::handle);
        registrar.playToClient(ServerVehicleExplosion.TYPE, ServerVehicleExplosion.STREAM_CODEC, ServerVehicleExplosion::handle);
        registrar.playToServer(ClientDecorationAction.TYPE, ClientDecorationAction.STREAM_CODEC, ClientDecorationAction::handle);
        registrar.playToClient(ServerDecorationAction.TYPE, ServerDecorationAction.STREAM_CODEC, ServerDecorationAction::handle);
        registrar.playToClient(ServerBroadcastEntities.TYPE, ServerBroadcastEntities.STREAM_CODEC, ServerBroadcastEntities::handle);
        registrar.playToServer(ClientRadarAction.TYPE, ClientRadarAction.STREAM_CODEC, ClientRadarAction::handle);
        registrar.playToServer(ClientVehicleSelectPartWeapon.TYPE, ClientVehicleSelectPartWeapon.STREAM_CODEC, ClientVehicleSelectPartWeapon::handle);
        registrar.playToClient(ServerSlicedPacket.TYPE, ServerSlicedPacket.STREAM_CODEC, ServerSlicedPacket::handle);
    }

}
