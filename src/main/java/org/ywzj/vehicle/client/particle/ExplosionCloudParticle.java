package org.ywzj.vehicle.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.ywzj.vehicle.particle.ExplosionCloudOption;

public class ExplosionCloudParticle extends TextureSheetParticle {

    public static ExplosionCloudParticleProvider provider(SpriteSet spriteSet) {
        return new ExplosionCloudParticleProvider(spriteSet);
    }

    public static class ExplosionCloudParticleProvider implements ParticleProvider<ExplosionCloudOption> {
        private final SpriteSet spriteSet;

        public ExplosionCloudParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(ExplosionCloudOption option, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ExplosionCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, option, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;

    private final float startR, startG, startB;
    private final float endR,   endG,   endB;

    protected ExplosionCloudParticle(ClientLevel level, double x, double y, double z,
                                     double vx, double vy, double vz,
                                     ExplosionCloudOption option, SpriteSet spriteSet) {
        super(level, x, y, z);
        this.spriteSet = spriteSet;
        this.quadSize = option.size();
        this.lifetime = option.life();
        this.gravity = option.gravity();
        this.hasPhysics = false;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.startR = option.getRed();
        this.startG = option.getGreen();
        this.startB = option.getBlue();
        this.endR   = option.getEndRed();
        this.endG   = option.getEndGreen();
        this.endB   = option.getEndBlue();

        this.rCol = this.startR;
        this.gCol = this.startG;
        this.bCol = this.startB;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.spriteSet);
            // 随时间减速
            this.xd *= 0.88;
            this.zd *= 0.88;
            // 随时间透明化
            float progress = (float) Math.pow((double) this.age / this.lifetime, 0.8f);
            this.alpha = 1.0f - progress;
            // 起始色 → 结束色 线性插值（橙→黑）
            this.rCol = this.startR + (this.endR - this.startR) * progress;
            this.gCol = this.startG + (this.endG - this.startG) * progress;
            this.bCol = this.startB + (this.endB - this.startB) * progress;
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

}
