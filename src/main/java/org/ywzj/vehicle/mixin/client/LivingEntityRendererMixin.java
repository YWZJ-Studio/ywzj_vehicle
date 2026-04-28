package org.ywzj.vehicle.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity> {

    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    public void setupRotations(T entity, PoseStack pPoseStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks, float p_320045_, CallbackInfo ci) {
        if (entity.getRootVehicle() != entity && entity.getRootVehicle() instanceof AbstractVehicle vehicle) {
            float yRotDiff = Mth.wrapDegrees(180 + pRotationYaw - Mth.lerp(pPartialTicks, vehicle.yRotO, vehicle.getYRot()));
            Quaternionf q = new Quaternionf();
            q.rotateY((float) Math.toRadians(Mth.lerp(pPartialTicks, -vehicle.yRotO, -vehicle.getYRot())))
                    .rotateX((float) Math.toRadians(Mth.lerp(pPartialTicks, vehicle.xRotO, vehicle.getXRot())))
                    .rotateZ((float) Math.toRadians(Mth.lerp(pPartialTicks, vehicle.zRotO, vehicle.getZRot())));
            q.rotateY((float) Math.toRadians(-yRotDiff));
            Vector3f rot = new Vector3f();
            q.getEulerAnglesYXZ(rot);
            pPoseStack.mulPose(Axis.YP.rotation(rot.y));
            pPoseStack.mulPose(Axis.XP.rotation(rot.x));
            pPoseStack.mulPose(Axis.ZP.rotation(rot.z));
            ci.cancel();
        }
    }

}
