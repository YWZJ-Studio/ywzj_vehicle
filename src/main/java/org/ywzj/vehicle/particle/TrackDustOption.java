package org.ywzj.vehicle.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.ywzj.vehicle.all.AllParticleTypes;

public record TrackDustOption(int color, float scale, float gravity) implements ParticleOptions {

    public static final Codec<TrackDustOption> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.INT.fieldOf("color").forGetter(TrackDustOption::color),
            Codec.FLOAT.fieldOf("scale").forGetter(TrackDustOption::scale),
            Codec.FLOAT.fieldOf("gravity").forGetter(TrackDustOption::gravity)
    ).apply(builder, TrackDustOption::new));

    public static final StreamCodec<FriendlyByteBuf, TrackDustOption> STREAM_CODEC = StreamCodec.of(
            (buffer, option) -> option.writeToNetwork(buffer),
            buffer -> new TrackDustOption(buffer.readInt(), buffer.readFloat(), buffer.readFloat())
    );

    @Override
    public ParticleType<?> getType() {
        return AllParticleTypes.TRACK_DUST.get();
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(color);
        buffer.writeFloat(scale);
        buffer.writeFloat(gravity);
    }

}
