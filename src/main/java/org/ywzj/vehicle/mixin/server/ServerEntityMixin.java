package org.ywzj.vehicle.mixin.server;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerVehicleSeatsChange;

import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {

    @Shadow @Final private Entity entity;

    @Inject(
            method = "sendPairingData",
            at = @At("TAIL")
    )
    public void sendPairingData(ServerPlayer pPlayer, Consumer<Packet<ClientGamePacketListener>> pConsumer, CallbackInfo ci) {
        if (this.entity instanceof AbstractVehicle vehicle) {
            Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> pPlayer), new ServerVehicleSeatsChange(vehicle));
        }
    }

}
