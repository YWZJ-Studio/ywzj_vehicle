package org.ywzj.vehicle.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TrackParticle extends TextureSheetParticle {

    private final float trackScalar;
    private final float yRot;

    protected TrackParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.friction = 0.96F;
        this.hasPhysics = false;
        this.lifetime = 1200;
        this.trackScalar = (float) vx;
        this.yRot = (float) vy;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level.getBlockState(BlockPos.containing(new Vec3(this.x, this.y - 0.01f, this.z))).isAir()) {
            this.remove();
            return;
        }
        // 渐隐
        this.alpha = ((float) this.lifetime - this.age) / this.lifetime;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // 使用粒子纹理图集
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        Vec3 vec3 = pRenderInfo.getPosition();
        float f = (float)(Mth.lerp(pPartialTicks, this.xo, this.x) - vec3.x());
        float f1 = (float)(Mth.lerp(pPartialTicks, this.yo, this.y) - vec3.y() + 0.001);
        float f2 = (float)(Mth.lerp(pPartialTicks, this.zo, this.z) - vec3.z());

        Quaternionf quaternionf = new Quaternionf();
        quaternionf.rotateY((float) -Math.toRadians(yRot + 180));
        quaternionf.rotateX((float) (Math.PI / 2));

        Vector3f[] avector3f = new Vector3f[] {
            new Vector3f(-1.0F, -1.0F, 0.0F),
            new Vector3f(-1.0F, 1.0F, 0.0F),
            new Vector3f(1.0F, 1.0F, 0.0F),
            new Vector3f(1.0F, -1.0F, 0.0F)
        };

        for(int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.rotate(quaternionf);
            vector3f.mul(trackScalar);
            vector3f.add(f, f1, f2);
        }

        float f6 = this.getU0();
        float f7 = this.getU1();
        float f4 = this.getV0();
        float f5 = this.getV1();
        int j = this.getLightColor(pPartialTicks);
        pBuffer.addVertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z()).setUv(f7, f5).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setUv2(j & 0xFFFF, j >> 16 & 0xFFFF);
        pBuffer.addVertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z()).setUv(f7, f4).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setUv2(j & 0xFFFF, j >> 16 & 0xFFFF);
        pBuffer.addVertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z()).setUv(f6, f4).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setUv2(j & 0xFFFF, j >> 16 & 0xFFFF);
        pBuffer.addVertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z()).setUv(f6, f5).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setUv2(j & 0xFFFF, j >> 16 & 0xFFFF);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprite;

        public Factory(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            TrackParticle p = new TrackParticle(level, x, y, z, vx, vy, vz);
            p.pickSprite(sprite);
            return p;
        }

    }

}
