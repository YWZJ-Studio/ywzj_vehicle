package org.ywzj.vehicle.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.ywzj.vehicle.particle.DustSmokeOption;

public class DustSmokeParticle extends TextureSheetParticle {
    public static DustSmokeParticleProvider provider(SpriteSet spriteSet) {
        return new DustSmokeParticleProvider(spriteSet);
    }

    public static class DustSmokeParticleProvider implements ParticleProvider<DustSmokeOption> {
        private final SpriteSet spriteSet;

        public DustSmokeParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(DustSmokeOption typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            float scale = Math.max(0.01f, typeIn.scale());
            return new DustSmokeParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, scale, this.spriteSet);
        }
    }

    private final SpriteSet spriteSet;

    protected DustSmokeParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, float scale, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.35f, 0.35f);
        this.quadSize *= scale;
        this.lifetime = 8;
        this.gravity = -0.2f;
        this.hasPhysics = true;
        this.xd = vx * 0.98;
        this.yd = vy * 0.98;
        this.zd = vz * 0.98;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(spriteSet);
        }
    }
}
