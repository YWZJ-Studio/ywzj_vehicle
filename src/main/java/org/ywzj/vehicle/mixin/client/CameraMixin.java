package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.client.handler.FirstPersonHandler;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.part.PartUnit;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double pX, double pY, double pZ);

    @Shadow
    protected abstract void move(float pDistanceOffset, float pVerticalOffset, float pHorizontalOffset);

    @Shadow
    protected abstract float getMaxZoom(float pStartingDistance);

    @Shadow
    public abstract float getXRot();

    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float zRot);

    @Shadow
    private Entity entity;

    @Inject(method = "setup", at = @At("TAIL"))
    public void setupVehicleCamera(BlockGetter pLevel, Entity pEntity, boolean pDetached, boolean pThirdPersonReverse, float pPartialTick, CallbackInfo ci) {
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        LocalVehiclePlayer instance = LocalVehiclePlayer.instance;
        Player player = instance.getPlayer();
        if (pEntity != player && pEntity.getVehicle() instanceof AbstractVehicle vehicle) {
            Vec3 pos = vehicle.thirdPersonPosition(pPartialTick);
            this.setPosition(pos.x, pos.y, pos.z);
            zoomThirdPerson(vehicle);
            return;
        }
        AbstractVehicle vehicle = instance.vehicle;
        if (vehicle != null) {
            PartUnit<?> operatorUnit = instance.seat.partUnit;
            if (operatorUnit == null) {
                return;
            }
            Vec3 position = instance.cameraPosition(operatorUnit, pPartialTick);
            this.setPosition(position.x, position.y, position.z);
            Vector3f euler = instance.cameraRotationO()
                    .slerp(instance.cameraRotation(), pPartialTick)
                    .getEulerAnglesYXZ(new Vector3f());
            this.setRotation((float) Math.toDegrees(-euler.y) + (float) FirstPersonHandler.getCurrentYawShake(),
                    (float) Math.toDegrees(euler.x) + (float) FirstPersonHandler.getCurrentPitchShake(),
                    (float) Math.toDegrees(euler.z) + (float) FirstPersonHandler.getCurrentRollShake());
            if (instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
                zoomThirdPerson(vehicle);
            }
        }
    }

    private void zoomThirdPerson(AbstractVehicle vehicle) {
        move(-getMaxZoom((float) vehicle.thirdPersonDistance(getXRot())), 0, 0);
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    public void isDetached(CallbackInfoReturnable<Boolean> cir) {
        if (entity != null
                && entity.getVehicle() instanceof AbstractVehicle
                && LocalVehiclePlayer.instance.viewType == LocalVehiclePlayer.ViewType.THIRD_PERSON) {
            cir.setReturnValue(true);
        }
    }

}
