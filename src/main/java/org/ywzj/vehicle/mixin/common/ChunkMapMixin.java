package org.ywzj.vehicle.mixin.common;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.PacketDistributor;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ServerVehicleSeatsChange;

import java.util.List;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Inject(
            method = "playerLoadedChunk",
            at = @At("TAIL")
    )
    private void playerLoadedChunk(ServerPlayer pPlayer, MutableObject<ClientboundLevelChunkWithLightPacket> pPacketCache, LevelChunk pChunk, CallbackInfo ci, @Local(name = "list1") List<Entity> list1) {
        for (Entity entity : list1) {
            if (entity instanceof AbstractVehicle vehicle) {
                Channel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> pPlayer), new ServerVehicleSeatsChange(vehicle));
            }
        }
    }

}
