package org.ywzj.vehicle.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.network.codec.StreamCodec;
import org.ywzj.vehicle.all.AllParticleTypes;

public record MissileTrailOption(float size, int lingerTicks, float range) implements ParticleOptions {

    public MissileTrailOption {
        size = Float.isFinite(size) ? Math.max(size, 0.12f) : 0.2f;
        lingerTicks = Mth.clamp(lingerTicks, 0, 600);
        range = Float.isFinite(range) ? Mth.clamp(range, 0.1f, 48) : 4;
    }

    public static final Codec<MissileTrailOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.FLOAT.fieldOf("size").forGetter(MissileTrailOption::size),
                    Codec.INT.optionalFieldOf("linger_ticks", 0).forGetter(MissileTrailOption::lingerTicks),
                    Codec.FLOAT.optionalFieldOf("range", 4f).forGetter(MissileTrailOption::range))
                    .apply(builder, MissileTrailOption::new));

    public static final StreamCodec<FriendlyByteBuf, MissileTrailOption> STREAM_CODEC = StreamCodec.of(
            (buffer, option) -> {
                buffer.writeFloat(option.size);
                buffer.writeInt(option.lingerTicks);
                buffer.writeFloat(option.range);
            },
            buffer -> new MissileTrailOption(buffer.readFloat(), buffer.readInt(), buffer.readFloat())
    );

    @Override
    public ParticleType<?> getType() {
        return AllParticleTypes.MISSILE_TRAIL.get();
    }

}
