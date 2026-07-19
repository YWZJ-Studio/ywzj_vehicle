package org.ywzj.vehicle.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.particle.SmokeCloudOption;

@OnlyIn(Dist.CLIENT)
public class SmokeCloudParticle extends TextureSheetParticle {

    public static SmokeCloudParticleProvider provider(SpriteSet spriteSet) {
        return new SmokeCloudParticleProvider(spriteSet);
    }

    public static class SmokeCloudParticleProvider implements ParticleProvider<SmokeCloudOption> {
        private final SpriteSet spriteSet;

        public SmokeCloudParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SmokeCloudOption option, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SmokeCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, option, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;
    private final float startR, startG, startB;
    private final float endR,   endG,   endB;
    private final float startAlpha, endAlpha;
    private final float startSize, endSize;
    private final boolean changing;

    protected SmokeCloudParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz,
                                 SmokeCloudOption option, SpriteSet spriteSet) {
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
        this.startAlpha = option.alpha();
        this.endAlpha = option.endAlpha();
        this.startSize = option.size();
        this.endSize = option.endSize();

        this.rCol = this.startR;
        this.gCol = this.startG;
        this.bCol = this.startB;
        this.alpha = this.startAlpha;
        this.changing = option.changing();
        if (this.changing) {
            this.setSpriteFromAge(spriteSet);
        } else {
            this.pickSprite(spriteSet);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            if (changing) {
                this.setSpriteFromAge(this.spriteSet);
            }
            this.xd *= 0.88;
            this.zd *= 0.88;
            float progress = (float) this.age / this.lifetime;
            this.alpha = lerp(this.startAlpha, this.endAlpha, progress);
            this.rCol = lerp(this.startR, this.endR, progress);
            this.gCol = lerp(this.startG, this.endG, progress);
            this.bCol = lerp(this.startB, this.endB, progress);
            float sizeProgress = (float) (1.0F - Math.pow((1.0F - progress), 8));
            this.quadSize = lerp(this.startSize, this.endSize, sizeProgress);
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
