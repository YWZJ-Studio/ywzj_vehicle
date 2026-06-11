package org.ywzj.vehicle.all;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.particle.BulletHoleOption;
import org.ywzj.vehicle.particle.DustSmokeOption;
import org.ywzj.vehicle.particle.SmokeCloudOption;

public class AllParticleTypes {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(net.minecraft.core.registries.Registries.PARTICLE_TYPE, YwzjVehicle.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<DustSmokeOption>> DUST_SMOKE = PARTICLE_TYPES.register("dust_smoke",
            () -> createOptions(DustSmokeOption.CODEC, DustSmokeOption.STREAM_CODEC)
    );

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_STONE = PARTICLE_TYPES.register("dust_stone",
            () -> new SimpleParticleType(false)
    );

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TRACK = PARTICLE_TYPES.register("track",
            () -> new SimpleParticleType(false)
    );

    public static final RegistryObject<ParticleType<BulletHoleOption>> BULLET_HOLE = PARTICLE_TYPES.register("bullet_hole",
            () -> createOptions(BulletHoleOption.CODEC, BulletHoleOption.DESERIALIZER)
    );

    public static final RegistryObject<ParticleType<SmokeCloudOption>> CHANGING_CLOUD = PARTICLE_TYPES.register("changing_cloud",
            () -> createOptions(SmokeCloudOption.CODEC, SmokeCloudOption.DESERIALIZER)
    );

    public static final RegistryObject<ParticleType<SmokeCloudOption>> FIXED_CLOUD = PARTICLE_TYPES.register("fixed_cloud",
            () -> createOptions(SmokeCloudOption.CODEC, SmokeCloudOption.DESERIALIZER)
    );

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

    @SuppressWarnings("deprecation")
    public static <T extends ParticleOptions> ParticleType<T> createOptions(Codec<T> codec, StreamCodec<? super FriendlyByteBuf, T> streamCodec) {
        return new ParticleType<>(false) {
            public @NotNull MapCodec<T> codec() {
                return codec.fieldOf("particle");
            }
            public @NotNull StreamCodec<? super FriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        };
    }

}
