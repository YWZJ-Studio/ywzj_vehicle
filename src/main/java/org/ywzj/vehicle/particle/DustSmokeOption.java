package org.ywzj.vehicle.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.all.AllParticleTypes;

import java.util.Locale;

public record DustSmokeOption(float scale) implements ParticleOptions {
    public static final Codec<DustSmokeOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.FLOAT.fieldOf("scale").forGetter(DustSmokeOption::scale)
            ).apply(builder, DustSmokeOption::new));

    @SuppressWarnings("deprecation")
    public static final Deserializer<DustSmokeOption> DESERIALIZER = new Deserializer<>() {
        @Override
        public DustSmokeOption fromCommand(ParticleType<DustSmokeOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float scale = reader.readFloat();
            return new DustSmokeOption(scale);
        }

        @Override
        public DustSmokeOption fromNetwork(ParticleType<DustSmokeOption> particleType, FriendlyByteBuf buffer) {
            return new DustSmokeOption(buffer.readFloat());
        }
    };

    @Override
    public ParticleType<?> getType() {
        return AllParticleTypes.DUST_SMOKE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.scale);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f", ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()), this.scale);
    }
}
