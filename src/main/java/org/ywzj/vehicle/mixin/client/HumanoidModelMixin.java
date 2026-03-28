package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.PartUnit;
import org.ywzj.vehicle.vehicle.pojo.PassengerPose;

@Mixin(value = HumanoidModel.class)
public class HumanoidModelMixin {

    @Shadow
    @Final
    public ModelPart leftArm;

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftLeg;

    @Shadow
    @Final
    public ModelPart rightLeg;

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At(value = "TAIL")
    )
    private void setupAnim(LivingEntity livingEntity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (livingEntity.getVehicle() instanceof AbstractVehicle vehicle) {
            if (livingEntity instanceof Player) {
                PartUnit<?> partUnit = vehicle.getOwnOperatorUnit(livingEntity);
                if (partUnit != null && partUnit.getPassengerPose() != null) {
                    setPose(partUnit.getPassengerPose());
                }
            }
        }
    }

    private void setPose(PassengerPose pose) {
        if (pose.leftArmX != null) {
            this.leftArm.x = pose.leftArmX;
        }
        if (pose.leftArmY != null) {
            this.leftArm.y = pose.leftArmY;
        }
        if (pose.leftArmZ != null) {
            this.leftArm.z = pose.leftArmZ;
        }
        if (pose.leftArmRotX != null) {
            this.leftArm.xRot = pose.leftArmRotX;
        }
        if (pose.leftArmRotY != null) {
            this.leftArm.yRot = pose.leftArmRotY;
        }
        if (pose.leftArmRotZ != null) {
            this.leftArm.zRot = pose.leftArmRotZ;
        }
        if (pose.rightArmX != null) {
            this.rightArm.x = pose.rightArmX;
        }
        if (pose.rightArmY != null) {
            this.rightArm.y = pose.rightArmY;
        }
        if (pose.rightArmZ != null) {
            this.rightArm.z = pose.rightArmZ;
        }
        if (pose.rightArmRotX != null) {
            this.rightArm.xRot = pose.rightArmRotX;
        }
        if (pose.rightArmRotY != null) {
            this.rightArm.yRot = pose.rightArmRotY;
        }
        if (pose.rightArmRotZ != null) {
            this.rightArm.zRot = pose.rightArmRotZ;
        }
        if (pose.leftLegX != null) {
            this.leftLeg.x = pose.leftLegX;
        }
        if (pose.leftLegY != null) {
            this.leftLeg.y = pose.leftLegY;
        }
        if (pose.leftLegZ != null) {
            this.leftLeg.z = pose.leftLegZ;
        }
        if (pose.leftLegRotX != null) {
            this.leftLeg.xRot = pose.leftLegRotX;
        }
        if (pose.leftLegRotY != null) {
            this.leftLeg.yRot = pose.leftLegRotY;
        }
        if (pose.leftLegRotZ != null) {
            this.leftLeg.zRot = pose.leftLegRotZ;
        }
        if (pose.rightLegX != null) {
            this.rightLeg.x = pose.rightLegX;
        }
        if (pose.rightLegY != null) {
            this.rightLeg.y = pose.rightLegY;
        }
        if (pose.rightLegZ != null) {
            this.rightLeg.z = pose.rightLegZ;
        }
        if (pose.rightLegRotX != null) {
            this.rightLeg.xRot = pose.rightLegRotX;
        }
        if (pose.rightLegRotY != null) {
            this.rightLeg.yRot = pose.rightLegRotY;
        }
        if (pose.rightLegRotZ != null) {
            this.rightLeg.zRot = pose.rightLegRotZ;
        }
    }

}
