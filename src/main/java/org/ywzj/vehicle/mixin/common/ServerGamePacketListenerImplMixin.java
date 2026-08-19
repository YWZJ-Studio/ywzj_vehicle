package org.ywzj.vehicle.mixin.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.ywzj.vehicle.vehicle.parenting.DeckAttachment;
import org.ywzj.vehicle.vehicle.parenting.VehicleParenting;

/**
 * Lets a player standing on a vehicle move as fast as the vehicle is carrying it.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @ModifyConstant(
            method = "handleMovePlayer",
            constant = @Constant(floatValue = 100.0F))
    private float ywzj_vehicle$allowCarriedMotion(float allowance) {
        DeckAttachment attachment = VehicleParenting.attachmentOf(this.player);
        if (attachment == null || !attachment.gripped()) {
            return allowance;
        }
        // Compared against squared distance, so the platform's contribution enters squared.
        return allowance + (float) attachment.lastCarrySpeedSqr();
    }

}
