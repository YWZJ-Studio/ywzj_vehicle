package org.ywzj.vehicle.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.ywzj.vehicle.all.AllParticleTypes;

public record DustSmokeOption(float scale) implements ParticleOptions {
    public static final Codec<DustSmokeOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.FLOAT.fieldOf("scale").forGetter(DustSmokeOption::scale)
            ).apply(builder, DustSmokeOption::new));

    public static final StreamCodec<FriendlyByteBuf, DustSmokeOption> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> option.writeToNetwork(buf),
            buf -> new DustSmokeOption(buf.readFloat())
    );

    @Override
    public ParticleType<?> getType() {
        return AllParticleTypes.DUST_SMOKE.get();
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.scale);
    }

}
