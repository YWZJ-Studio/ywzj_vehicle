package org.ywzj.vehicle.all;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.particle.DustSmokeOption;

public class AllParticleTypes {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, YwzjVehicle.MOD_ID);

    public static final RegistryObject<ParticleType<DustSmokeOption>> DUST_SMOKE = PARTICLE_TYPES.register("dust_smoke",
            () -> createOptions(DustSmokeOption.CODEC, DustSmokeOption.DESERIALIZER)
    );

    public static final RegistryObject<SimpleParticleType> DUST_STONE = PARTICLE_TYPES.register("dust_stone",
            () -> new SimpleParticleType(false)
    );

    public static final RegistryObject<SimpleParticleType> TRACK = PARTICLE_TYPES.register("track",
            () -> new SimpleParticleType(false)
    );

    public static final RegistryObject<SimpleParticleType> SMOKE_CLOUD = PARTICLE_TYPES.register("smoke_cloud",
            () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

    @SuppressWarnings("deprecation")
    public static <T extends ParticleOptions> ParticleType<T> createOptions(Codec<T> codec, ParticleOptions.Deserializer<T> deserializer) {
        return new ParticleType<>(false, deserializer) {
            public @NotNull Codec<T> codec() {
                return codec;
            }
        };
    }

}
