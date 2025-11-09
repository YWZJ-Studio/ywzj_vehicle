package org.ywzj.vehicle.api.entity;

import net.minecraft.world.entity.LivingEntity;

/**
 * Codes Based On @TACZ
 */
public interface KnockBackModifier {
    static KnockBackModifier fromLivingEntity(LivingEntity entity) {
        return (KnockBackModifier)entity;
    }

    void ywzj_vehicle$resetKnockBackStrength();

    double ywzj_vehicle$getKnockBackStrength();

    void ywzj_vehicle$setKnockBackStrength(double var1);
}