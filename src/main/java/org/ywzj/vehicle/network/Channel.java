package org.ywzj.vehicle.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.ywzj.vehicle.Vehicle;
import org.ywzj.vehicle.network.message.ClientVehicleMoveControl;
import org.ywzj.vehicle.network.message.ClientWeaponUnitControl;

import java.util.Optional;

import static net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER;

public class Channel {

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Vehicle.MOD_ID, Vehicle.CHANNEL))
            .networkProtocolVersion(() -> Vehicle.PROTOCOL)
            .clientAcceptedVersions(Vehicle.PROTOCOL::equals)
            .serverAcceptedVersions(Vehicle.PROTOCOL::equals)
            .simpleChannel();

    @SubscribeEvent
    public static void onCommonSetupEvent(FMLCommonSetupEvent event) {

        CHANNEL.registerMessage(PacketId.C_VEHICLE_CONTROL.value(), ClientVehicleMoveControl.class,
                ClientVehicleMoveControl::encode, ClientVehicleMoveControl::decode,
                ClientVehicleMoveControl::onClientMessageReceived,
                Optional.of(PLAY_TO_SERVER));

        CHANNEL.registerMessage(PacketId.C_WEAPON_UNIT_CONTROL.value(), ClientWeaponUnitControl.class,
                ClientWeaponUnitControl::encode, ClientWeaponUnitControl::decode,
                ClientWeaponUnitControl::onClientMessageReceived,
                Optional.of(PLAY_TO_SERVER));

    }

}

enum PacketId {

    C_VEHICLE_CONTROL(100),
    C_WEAPON_UNIT_CONTROL(101);

    private final int id;

    PacketId(int id) {
        this.id = id;
    }

    int value() {
        return id;
    }

}
