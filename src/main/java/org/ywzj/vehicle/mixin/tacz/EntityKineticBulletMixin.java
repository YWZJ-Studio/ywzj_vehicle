//package org.ywzj.vehicle.mixin.tacz;
//
//import com.tacz.guns.entity.EntityKineticBullet;
//import com.tacz.guns.util.TacHitResult;
//import net.minecraft.core.particles.ParticleTypes;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.phys.Vec3;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import org.ywzj.vehicle.api.entity.OBBEntity;
//
//@Mixin(EntityKineticBullet.class)
//public class EntityKineticBulletMixin {
//
//    @Inject(method = "onHitEntity", at = @At("HEAD"), remap = false)
//    public void onHitEntity(TacHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
//        // 测试用，用于观察落点
//        if (result.getEntity() instanceof OBBEntity obbEntity) {
//            if (result.getEntity().level() instanceof ServerLevel serverLevel) {
//                serverLevel.sendParticles(ParticleTypes.FLAME,
//                        result.getLocation().x, result.getLocation().y, result.getLocation().z,
//                        1, 0, 0, 0, 0);
//            }
//        }
//    }
//
//}
