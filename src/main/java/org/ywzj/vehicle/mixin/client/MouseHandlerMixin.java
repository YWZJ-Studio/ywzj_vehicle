package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.MouseHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Shadow private double accumulatedDX;

    @Shadow private double accumulatedDY;

    @ModifyVariable(
            method = "turnPlayer()V",
            at = @At(value = "STORE"),
            ordinal = 5)
    private double modifyD2(double d) {
//        Minecraft mc = Minecraft.getInstance();
//        Player player = mc.player;
//
//        if (player == null) return d;
//        VehicleCrossHairOverlay.x = accumulatedDX;
//        VehicleCrossHairOverlay.y = accumulatedDY;
        return d;
    }

    @Inject(method = "turnPlayer()V", at = @At(value = "HEAD"))
    private void modifyD3(CallbackInfo ci) {

    }

}
