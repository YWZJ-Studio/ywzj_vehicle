package org.ywzj.vehicle.mixin.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.ywzj.vehicle.api.entity.OBBEntity;

@Mixin(SubLevelEntityCollision.class)
public class SubLevelEntityCollisionMixin {

    @WrapOperation(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
            )
    )
    private static AABB wrapGetBoundingBox(Entity instance, Operation<AABB> original) {
        if (instance instanceof OBBEntity) {
            return AABB.ofSize(instance.position().add(0, 0.5, 0), 1, 1, 1);
        }
        return original.call(instance);
    }

}
