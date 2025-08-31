package org.ywzj.vehicle.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DustStoneParticle extends TextureSheetParticle {
    public static DustStoneParticleProvider provider(SpriteSet spriteSet) {
        return new DustStoneParticleProvider(spriteSet);
    }

    public static class DustStoneParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public DustStoneParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new DustStoneParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }


    protected DustStoneParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.setSize(0.35f, 0.35f);
        this.quadSize *= (0.5f - world.random.nextFloat() * 0.2f);
        this.lifetime = 8;
        this.gravity = 0.1f;
        this.hasPhysics = true;
        this.xd = vx * 0.98;
        this.yd = vy * 0.98;
        this.zd = vz * 0.98;
        this.setSprite(spriteSet.get(world.random));
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
    }
}
