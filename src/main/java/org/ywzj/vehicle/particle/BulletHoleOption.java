package org.ywzj.vehicle.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import org.ywzj.vehicle.all.AllParticleTypes;

import java.util.Locale;

public class BulletHoleOption implements ParticleOptions {

    public static final Codec<BulletHoleOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.INT.fieldOf("dir").forGetter(o -> o.direction.ordinal()),
                    Codec.LONG.fieldOf("pos").forGetter(o -> o.pos.asLong()),
                    Codec.FLOAT.fieldOf("r").forGetter(o -> o.r),
                    Codec.FLOAT.fieldOf("g").forGetter(o -> o.g),
                    Codec.FLOAT.fieldOf("b").forGetter(o -> o.b),
                    Codec.FLOAT.fieldOf("caliber").forGetter(o -> o.caliber)
            ).apply(builder, BulletHoleOption::new));

    @SuppressWarnings("deprecation")
    public static final Deserializer<BulletHoleOption> DESERIALIZER = new Deserializer<>() {
        @Override
        public BulletHoleOption fromCommand(ParticleType<BulletHoleOption> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int dir = reader.readInt();
            reader.expect(' ');
            long pos = reader.readLong();
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            reader.expect(' ');
            float caliber = reader.readFloat();
            return new BulletHoleOption(dir, pos, r, g, b, caliber);
        }

        @Override
        public BulletHoleOption fromNetwork(ParticleType<BulletHoleOption> type, FriendlyByteBuf buffer) {
            return new BulletHoleOption(buffer.readVarInt(), buffer.readLong(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }
    };

    private final Direction direction;
    private final BlockPos pos;
    private final float r;
    private final float g;
    private final float b;
    private final float caliber;
    private int entityId = -1;
    private String boneName = "";
    private int attachmentBoneIndex = -1;
    private boolean bakedAttachment;
    private Vec3 boneOffset = Vec3.ZERO;
    private Quaternionf selfRotation = new Quaternionf();

    public BulletHoleOption(int dir, long pos, float r, float g, float b, float caliber) {
        this.direction = Direction.values()[dir];
        this.pos = BlockPos.of(pos);
        this.r = r;
        this.g = g;
        this.b = b;
        this.caliber = caliber;
    }

    public BulletHoleOption(Direction dir, BlockPos pos, float r, float g, float b, float caliber) {
        this.direction = dir;
        this.pos = pos;
        this.r = r;
        this.g = g;
        this.b = b;
        this.caliber = caliber;
    }

    /** v1 模型骨骼绑定，仅客户端本地粒子使用。 */
    public BulletHoleOption withBone(int entityId, String boneName, Vec3 boneOffset, Quaternionf boneRotation) {
        this.entityId = entityId;
        this.boneName = boneName;
        this.boneOffset = boneOffset;
        this.selfRotation = new Quaternionf(boneRotation);
        return this;
    }

    /** v2 baked 模型附件绑定；索引为 -1 时表示静态模型根空间。 */
    public BulletHoleOption withBakedBone(int entityId, int attachmentBoneIndex, Vec3 attachmentOffset, Quaternionf selfRotation) {
        this.entityId = entityId;
        this.attachmentBoneIndex = attachmentBoneIndex;
        this.bakedAttachment = true;
        this.boneOffset = attachmentOffset;
        this.selfRotation = new Quaternionf(selfRotation);
        return this;
    }

    public Direction getDirection() { return direction; }
    public BlockPos getPos() { return pos; }
    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
    public float getCaliber() { return caliber; }
    public int getEntityId() { return entityId; }
    public String getBoneName() { return boneName; }
    public int getAttachmentBoneIndex() { return attachmentBoneIndex; }
    public boolean isBakedAttachment() { return bakedAttachment; }
    public Vec3 getBoneOffset() { return boneOffset; }
    public Quaternionf getSelfRotation() { return new Quaternionf(selfRotation); }

    @Override
    public ParticleType<?> getType() {
        return AllParticleTypes.BULLET_HOLE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.direction);
        buffer.writeBlockPos(this.pos);
        buffer.writeFloat(this.r);
        buffer.writeFloat(this.g);
        buffer.writeFloat(this.b);
        buffer.writeFloat(this.caliber);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d %d %f %f %f %f",
                ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()),
                this.direction.ordinal(), this.pos.asLong(), this.r, this.g, this.b, this.caliber);
    }
}
