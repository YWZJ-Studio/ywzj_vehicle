package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.stream.client.ClientDetachedView;

@Mixin(LevelRenderer.class)
public class LevelRendererOriginMixin {

    @Unique
    private Entity ywzj$detachedView;

    @Inject(method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At("HEAD"))
    private void ywzj$resolveDetachedView(Camera camera, Frustum frustum, boolean capturedFrustum,
                                          boolean spectator, CallbackInfo ci) {
        this.ywzj$detachedView = ClientDetachedView.viewedVehicle();
    }

    @Redirect(method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double ywzj$originX(LocalPlayer player) {
        Entity vehicle = this.ywzj$detachedView;
        return vehicle != null ? vehicle.getX() : player.getX();
    }

    @Redirect(method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double ywzj$originY(LocalPlayer player) {
        Entity vehicle = this.ywzj$detachedView;
        return vehicle != null ? vehicle.getY() : player.getY();
    }

    @Redirect(method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double ywzj$originZ(LocalPlayer player) {
        Entity vehicle = this.ywzj$detachedView;
        return vehicle != null ? vehicle.getZ() : player.getZ();
    }

}
