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

public record TrackDustOption(int color, float scale, float gravity) implements ParticleOptions {

    public static final Codec<TrackDustOption> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.INT.fieldOf("color").forGetter(TrackDustOption::color),
            Codec.FLOAT.fieldOf("scale").forGetter(TrackDustOption::scale),
            Codec.FLOAT.fieldOf("gravity").forGetter(TrackDustOption::gravity)
    ).apply(builder, TrackDustOption::new));

    @SuppressWarnings("deprecation")
    public static final Deserializer<TrackDustOption> DESERIALIZER = new Deserializer<>() {

        @Override
        public TrackDustOption fromCommand(ParticleType<TrackDustOption> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            float scale = reader.readFloat();
            reader.expect(' ');
            return new TrackDustOption(color, scale, reader.readFloat());
        }

        @Override
        public TrackDustOption fromNetwork(ParticleType<TrackDustOption> type, FriendlyByteBuf buffer) {
            return new TrackDustOption(buffer.readInt(), buffer.readFloat(), buffer.readFloat());
        }

    };

    @Override
    public ParticleType<?> getType() {
        return AllParticleTypes.TRACK_DUST.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(color);
        buffer.writeFloat(scale);
        buffer.writeFloat(gravity);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d %.2f %.2f", ForgeRegistries.PARTICLE_TYPES.getKey(getType()), color, scale, gravity);
    }

}
