package org.ywzj.vehicle.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.api.entity.OBBEntity;
import org.ywzj.vehicle.client.render.util.OBBRenderer;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.part.PartUnit;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "renderHitbox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;F)V",
            at = @At("RETURN"))
    private static void renderHitbox(PoseStack pMatrixStack, VertexConsumer pBuffer, Entity pEntity, float pPartialTicks, CallbackInfo ci) {
        if (pEntity instanceof OBBEntity obbEntity) {
            OBBRenderer.INSTANCE.render(pEntity.position(),
                    obbEntity.getOBBs(),
                    pMatrixStack, pBuffer, 0, 1, 0, 1, pPartialTicks);
            if (pEntity instanceof AbstractVehicle vehicle) {
                OBBRenderer.INSTANCE.render(pEntity.position(),
                        List.of(vehicle.getMainCubeOBB().obb()),
                        pMatrixStack, pBuffer, 0, 0, 1, 1, pPartialTicks);
                OBBRenderer.INSTANCE.render(pEntity.position(),
                        vehicle.getDecorationUnits().values().stream().map(PartUnit::getOBBs).flatMap(List::stream).collect(Collectors.toList()),
                        pMatrixStack, pBuffer, 1, 0, 0, 1, pPartialTicks);
            }
        }
    }

}
