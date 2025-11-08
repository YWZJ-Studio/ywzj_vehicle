package org.ywzj.vehicle.all;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.YwzjVehicle;

public class AllDamageTypes {

    public static final ResourceKey<DamageType> BULLET;

    static {
        BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(YwzjVehicle.MOD_ID, "bullet"));
    }

    public static class Sources {

        private static Holder.Reference<DamageType> getHolder(RegistryAccess access, ResourceKey<DamageType> damageTypeKey) {
            return access.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageTypeKey);
        }

        public static DamageSource bullet(RegistryAccess access, Entity bullet, Entity shooter, Vec3 damageSourcePosition) {
            return new DamageSource(getHolder(access, BULLET), bullet, shooter, damageSourcePosition);
        }

    }

}
