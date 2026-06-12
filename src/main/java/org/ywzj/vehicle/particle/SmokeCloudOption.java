package org.ywzj.vehicle.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.ywzj.vehicle.all.AllParticleTypes;

public record SmokeCloudOption(boolean changing, int color, int endColor, float alpha, float endAlpha, int life, float size, float endSize, float gravity) implements ParticleOptions {

    public static final Codec<SmokeCloudOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.BOOL.fieldOf("changing").forGetter(SmokeCloudOption::changing),
                    Codec.INT.fieldOf("color").forGetter(SmokeCloudOption::color),
                    Codec.INT.fieldOf("endColor").forGetter(SmokeCloudOption::endColor),
                    Codec.FLOAT.fieldOf("alpha").forGetter(SmokeCloudOption::alpha),
                    Codec.FLOAT.fieldOf("endAlpha").forGetter(SmokeCloudOption::endAlpha),
                    Codec.INT.fieldOf("life").forGetter(SmokeCloudOption::life),
                    Codec.FLOAT.fieldOf("size").forGetter(SmokeCloudOption::size),
                    Codec.FLOAT.fieldOf("endSize").forGetter(SmokeCloudOption::endSize),
                    Codec.FLOAT.fieldOf("gravity").forGetter(SmokeCloudOption::gravity)
            ).apply(builder, SmokeCloudOption::new));

    public static final StreamCodec<FriendlyByteBuf, SmokeCloudOption> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> option.writeToNetwork(buf),
            SmokeCloudOption::fromNetwork
    );

    /** 单色构造器（endColor 默认纯黑，endSize 默认同 size，alpha 默认1，endAlpha 默认0） */
    public SmokeCloudOption(float r, float g, float b, int life, float size, float gravity) {
        this(true, Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255),
                0x000000, 1f, 0f, life, size, size, gravity);
    }

    /** 单色构造器，指定初始alpha */
    public SmokeCloudOption(float r, float g, float b, float alpha, int life, float size, float gravity) {
        this(true, Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255),
                0x000000, alpha, 0f, life, size, size, gravity);
    }

    /** 单色构造器，指定初始alpha与最终大小 */
    public SmokeCloudOption(float r, float g, float b, float alpha, int life, float size, float endSize, float gravity) {
        this(true, Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255),
                0x000000, alpha, 0f, life, size, endSize, gravity);
    }

    /** 单色构造器，指定初始alpha与最终alpha */
    public SmokeCloudOption(float r, float g, float b, float alpha, float endAlpha, int life, float size, float gravity) {
        this(true, Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255),
                0x000000, alpha, endAlpha, life, size, size, gravity);
    }

    /** 单色构造器，指定初始alpha、最终alpha与最终大小 */
    public SmokeCloudOption(float r, float g, float b, float alpha, float endAlpha, int life, float size, float endSize, float gravity) {
        this(true, Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255),
                0x000000, alpha, endAlpha, life, size, endSize, gravity);
    }

    /** 双色构造器，支持自定义起始/结束颜色（endSize 默认同 size，alpha 默认1，endAlpha 默认0） */
    public SmokeCloudOption(float r, float g, float b,
                            float er, float eg, float eb,
                            int life, float size, float gravity) {
        this(true, Math.round(r  * 255) << 16 | Math.round(g  * 255) << 8 | Math.round(b  * 255),
                Math.round(er * 255) << 16 | Math.round(eg * 255) << 8 | Math.round(eb * 255),
                1f, 0f, life, size, size, gravity);
    }

    /** 双色构造器，指定初始alpha */
    public SmokeCloudOption(float r, float g, float b,
                            float er, float eg, float eb,
                            float alpha, int life, float size, float gravity) {
        this(true, Math.round(r  * 255) << 16 | Math.round(g  * 255) << 8 | Math.round(b  * 255),
                Math.round(er * 255) << 16 | Math.round(eg * 255) << 8 | Math.round(eb * 255),
                alpha, 0f, life, size, size, gravity);
    }

    /** 双色构造器，指定初始alpha与最终大小 */
    public SmokeCloudOption(float r, float g, float b,
                            float er, float eg, float eb,
                            float alpha, int life, float size, float endSize, float gravity) {
        this(true, Math.round(r  * 255) << 16 | Math.round(g  * 255) << 8 | Math.round(b  * 255),
                Math.round(er * 255) << 16 | Math.round(eg * 255) << 8 | Math.round(eb * 255),
                alpha, 0f, life, size, endSize, gravity);
    }

    /** 双色构造器，指定初始alpha与最终alpha */
    public SmokeCloudOption(float r, float g, float b,
                            float er, float eg, float eb,
                            float alpha, float endAlpha, int life, float size, float gravity) {
        this(true, Math.round(r  * 255) << 16 | Math.round(g  * 255) << 8 | Math.round(b  * 255),
                Math.round(er * 255) << 16 | Math.round(eg * 255) << 8 | Math.round(eb * 255),
                alpha, endAlpha, life, size, size, gravity);
    }

    /** 双色构造器，指定初始alpha、最终alpha与最终大小 */
    public SmokeCloudOption(boolean changing, float r, float g, float b,
                            float er, float eg, float eb,
                            float alpha, float endAlpha, int life, float size, float endSize, float gravity) {
        this(changing, Math.round(r  * 255) << 16 | Math.round(g  * 255) << 8 | Math.round(b  * 255),
                Math.round(er * 255) << 16 | Math.round(eg * 255) << 8 | Math.round(eb * 255),
                alpha, endAlpha, life, size, endSize, gravity);
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
        return this.changing ? AllParticleTypes.CHANGING_CLOUD.get() : AllParticleTypes.FIXED_CLOUD.get();
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.changing);
        buffer.writeInt(this.color);
        buffer.writeInt(this.endColor);
        buffer.writeFloat(this.alpha);
        buffer.writeFloat(this.endAlpha);
        buffer.writeInt(this.life);
        buffer.writeFloat(this.size);
        buffer.writeFloat(this.endSize);
        buffer.writeFloat(this.gravity);
    }

    public static SmokeCloudOption fromNetwork(FriendlyByteBuf buffer) {
        return new SmokeCloudOption(buffer.readBoolean(), buffer.readInt(), buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

}
