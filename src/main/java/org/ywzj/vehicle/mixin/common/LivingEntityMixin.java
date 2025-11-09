package org.ywzj.vehicle.mixin.common;


import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.ywzj.vehicle.api.entity.KnockBackModifier;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements KnockBackModifier {
    @Unique
    public double ywzj_vehicle$knockbackStrength;

    @Unique
    public void ywzj_vehicle$resetKnockBackStrength() {
        this.ywzj_vehicle$knockbackStrength = -1.0F;
    }

    @Unique
    public double ywzj_vehicle$getKnockBackStrength() {
        return this.ywzj_vehicle$knockbackStrength;
    }

    @Unique
    public void ywzj_vehicle$setKnockBackStrength(double strength) {
        this.ywzj_vehicle$knockbackStrength = strength;
    }
}