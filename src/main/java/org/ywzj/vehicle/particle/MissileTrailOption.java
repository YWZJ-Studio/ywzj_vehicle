package org.ywzj.vehicle.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.all.AllParticleTypes;

import java.util.Locale;

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

    @SuppressWarnings("deprecation")
    public static final Deserializer<MissileTrailOption> DESERIALIZER = new Deserializer<>() {
        @Override
        public MissileTrailOption fromCommand(ParticleType<MissileTrailOption> type, StringReader reader)
                throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            reader.expect(' ');
            int lingerTicks = reader.readInt();
            reader.expect(' ');
            return new MissileTrailOption(size, lingerTicks, reader.readFloat());
        }

        @Override
        public MissileTrailOption fromNetwork(ParticleType<MissileTrailOption> type, FriendlyByteBuf buffer) {
            return new MissileTrailOption(buffer.readFloat(), buffer.readInt(), buffer.readFloat());
        }
    };

    @Override
    public ParticleType<?> getType() {
        return AllParticleTypes.MISSILE_TRAIL.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(size);
        buffer.writeInt(lingerTicks);
        buffer.writeFloat(range);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.3f %d %.3f",
                ForgeRegistries.PARTICLE_TYPES.getKey(getType()), size, lingerTicks, range);
    }

}
