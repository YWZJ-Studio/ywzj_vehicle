package org.ywzj.vehicle.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

// From Immersive_Aircraft
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity> {

    @Inject(method = "setupRotations", at = @At("TAIL"))
    public void setupRotations(T entity, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks, CallbackInfo ci) {
        if (entity.getRootVehicle() != entity && entity.getRootVehicle() instanceof AbstractVehicle vehicle) {
            float a = Mth.wrapDegrees(Mth.lerp(pPartialTicks, entity.yBodyRotO, entity.yBodyRot) - Mth.lerp(pPartialTicks, vehicle.yRotO, vehicle.getYRot()));
            float r = (Mth.abs(a) - 90f) / 90f;
            float r2;
            if (Mth.abs(a) <= 90f) {
                r2 = a / 90f;
            } else {
                if (a < 0) {
                    r2 = -(180f + a) / 90f;
                } else {
                    r2 = (180f - a) / 90f;
                }
            }
            pPoseStack.mulPose(Axis.XP.rotationDegrees(r * vehicle.getViewXRot(pPartialTicks) - r2 * vehicle.getViewZRot(pPartialTicks)));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(r * vehicle.getViewZRot(pPartialTicks) + r2 * vehicle.getViewXRot(pPartialTicks)));
        }
    }

}
