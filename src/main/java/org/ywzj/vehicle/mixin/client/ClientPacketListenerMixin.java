package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {

    protected ClientPacketListenerMixin(Minecraft p_295454_, Connection p_294773_, CommonListenerCookie p_294647_) {
        super(p_295454_, p_294773_, p_294647_);
    }

    @Redirect(
            method = "handleSetEntityPassengersPacket",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 0
    )
    private MutableComponent redirectMountMessage(String pKey, Object[] pArgs, ClientboundSetPassengersPacket packet) {
        Entity vehicle = this.minecraft.level.getEntity(packet.getVehicle());
        if (vehicle instanceof AbstractVehicle) {
            return Component.empty();
        }
        return Component.translatable(pKey, pArgs);
    }

}
