package org.ywzj.vehicle.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.particle.TrackDustOption;

@OnlyIn(Dist.CLIENT)
public class TrackDustParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected TrackDustParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz,
                                TrackDustOption option, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        setSize(0.4f, 0.4f);
        quadSize *= option.scale();
        lifetime = 70 + random.nextInt(70);
        gravity = option.gravity();
        hasPhysics = false;
        xd = vx;
        yd = vy;
        zd = vz;
        setColor((option.color() >> 16 & 255) / 255f, (option.color() >> 8 & 255) / 255f, (option.color() & 255) / 255f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSprite(sprites.get(Math.min(age / 8 + 1, 8), 8));
        }
        if (age < lifetime && !(alpha <= 0)) {
            alpha = 1f - (float) age / lifetime;
        } else {
            remove();
        }
        xd *= 0.85;
        yd *= 0.85;
        zd *= 0.85;
    }

    @Override
    public int getLightColor(float partialTick) {
        BlockPos pos = BlockPos.containing(x, y + 1, z);
        return level.isLoaded(pos) ? LevelRenderer.getLightColor(level, pos) : 0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<TrackDustOption> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(TrackDustOption option, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new TrackDustParticle(level, x, y, z, vx, vy, vz, option, sprites);
        }

    }

}
