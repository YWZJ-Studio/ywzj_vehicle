package org.ywzj.vehicle.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.ywzj.vehicle.all.AllParticleTypes;

public record ExplosionCloudOption(int color, int endColor, int life, float size, float gravity) implements ParticleOptions {

    public static final Codec<ExplosionCloudOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.INT.fieldOf("color").forGetter(ExplosionCloudOption::color),
                    Codec.INT.fieldOf("endColor").forGetter(ExplosionCloudOption::endColor),
                    Codec.INT.fieldOf("life").forGetter(ExplosionCloudOption::life),
                    Codec.FLOAT.fieldOf("size").forGetter(ExplosionCloudOption::size),
                    Codec.FLOAT.fieldOf("gravity").forGetter(ExplosionCloudOption::gravity)
            ).apply(builder, ExplosionCloudOption::new));

    public static final StreamCodec<FriendlyByteBuf, ExplosionCloudOption> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> option.writeToNetwork(buf),
            buf -> new ExplosionCloudOption(buf.readInt(), buf.readInt(), buf.readInt(), buf.readFloat(), buf.readFloat())
    );

    /** 单色构造器（endColor 默认纯黑，即渐变到黑色） */
    public ExplosionCloudOption(float r, float g, float b, int life, float size, float gravity) {
        this(Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255),
             0x000000, life, size, gravity);
    }

    /** 双色构造器，支持自定义起始/结束颜色 */
    public ExplosionCloudOption(float r, float g, float b,
                                float er, float eg, float eb,
                                int life, float size, float gravity) {
        this(Math.round(r  * 255) << 16 | Math.round(g  * 255) << 8 | Math.round(b  * 255),
             Math.round(er * 255) << 16 | Math.round(eg * 255) << 8 | Math.round(eb * 255),
             life, size, gravity);
    }

    // ── 起始色分量 ────────────────────────────────────────────
    public float getRed()   { return (this.color >> 16 & 255) / 255f; }
    public float getGreen() { return (this.color >> 8  & 255) / 255f; }
    public float getBlue()  { return (this.color        & 255) / 255f; }

    // ── 结束色分量 ────────────────────────────────────────────
    public float getEndRed()   { return (this.endColor >> 16 & 255) / 255f; }
    public float getEndGreen() { return (this.endColor >> 8  & 255) / 255f; }
    public float getEndBlue()  { return (this.endColor        & 255) / 255f; }

    @Override
    public ParticleType<?> getType() {
        return AllParticleTypes.EXPLOSION_CLOUD.get();
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(this.color);
        buffer.writeInt(this.endColor);
        buffer.writeInt(this.life);
        buffer.writeFloat(this.size);
        buffer.writeFloat(this.gravity);
    }

}
