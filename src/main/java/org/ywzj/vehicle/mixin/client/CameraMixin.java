package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.client.handler.FirstPersonHandler;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double pX, double pY, double pZ);

    @Shadow
    protected abstract void setRotation(float pYRot, float pXRot);

    @Shadow
    private Entity entity;

    @Inject(method = "setup", at = @At("TAIL"))
    public void setupVehicleCamera(BlockGetter pLevel, Entity pEntity, boolean pDetached, boolean pThirdPersonReverse, float pPartialTick, CallbackInfo ci) {
        if (pEntity.getVehicle() instanceof AbstractVehicle vehicle) {
            if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                return;
            }
            LocalVehiclePlayer localVehiclePlayer = LocalVehiclePlayer.instance;
            if (pEntity != localVehiclePlayer.getPlayer()) {
                if (pEntity instanceof LivingEntity passenger) {
                    Vec3 pos = vehicle.thirdPersonPosition(passenger, pPartialTick);
                    this.setPosition(pos.x, pos.y, pos.z);
                }
                return;
            }
            this.setPosition(Mth.lerp(pPartialTick, localVehiclePlayer.cameraXO, localVehiclePlayer.cameraX),
                    Mth.lerp(pPartialTick, localVehiclePlayer.cameraYO, localVehiclePlayer.cameraY),
                    Mth.lerp(pPartialTick, localVehiclePlayer.cameraZO, localVehiclePlayer.cameraZ));
            if (localVehiclePlayer.viewType == LocalVehiclePlayer.ViewType.SCOPE
                    || localVehiclePlayer.viewType == LocalVehiclePlayer.ViewType.OPERATOR
                    || LocalVehiclePlayer.instance.playerLerpSteps > 0) {
                FirstPersonHandler.zRot = Mth.lerp(pPartialTick, localVehiclePlayer.cameraAimRotZO, localVehiclePlayer.cameraAimRotZ);
                setRotation(Mth.lerp(pPartialTick, localVehiclePlayer.cameraAimRotYO, localVehiclePlayer.cameraAimRotY),
                        Mth.lerp(pPartialTick, localVehiclePlayer.cameraAimRotXO, localVehiclePlayer.cameraAimRotX));
            }
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    public void isDetached(CallbackInfoReturnable<Boolean> cir) {
        if (entity.getVehicle() instanceof AbstractVehicle
                && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
            cir.setReturnValue(true);
        }
    }

}
