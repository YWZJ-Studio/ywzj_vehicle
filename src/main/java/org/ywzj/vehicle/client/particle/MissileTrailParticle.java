package org.ywzj.vehicle.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.ywzj.vehicle.particle.MissileTrailOption;

@OnlyIn(Dist.CLIENT)
public class MissileTrailParticle extends TextureSheetParticle {

    private static final ImprovedNoise FLOW = new ImprovedNoise(RandomSource.create(0x59A17L));
    private static final float INITIAL_ALPHA = 0.5f;
    private static final float SMOKE_ALPHA = 0.65f;
    private final MissileTrailOption option;
    private final Vec3 exhaust;
    private Vec3 surfaceNormal;
    private float surfaceBulk;
    private float surfaceAlpha;
    private final Vec3 sideways;
    private final Vec3 upwards;
    private final double flowX, flowY, flowZ;
    private final float windInfluence;
    private float startSize;
    private final float initialSize;
    private final float shade;
    private float rollSpeed;
    private final int lingerTicks;

    protected MissileTrailParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz,
                                   MissileTrailOption option, SpriteSet sprites) {
        super(level, x, y, z);
        this.option = option;
        Vec3 direction = new Vec3(vx, vy, vz);
        Vec3 axis = direction.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : direction.normalize();
        this.exhaust = axis;
        Vec3 reference = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        this.sideways = axis.cross(reference).normalize();
        this.upwards = axis.cross(sideways).normalize();
        this.startSize = 0.2f * (float) Math.pow(5, option.size() * 1000f / 2250f);
        this.initialSize = startSize;
        this.quadSize = initialSize;
        this.lingerTicks = option.lingerTicks();
        this.lifetime = lingerTicks + 160 + (int) (Math.min(option.size(), 0.85f) * 160) + random.nextInt(30);
        this.hasPhysics = true;
        this.gravity = 0;
        this.friction = 1;
        this.xd = this.yd = this.zd = 0;
        this.shade = 0.95f + random.nextFloat() * 0.04f;
        this.setColor(shade, shade, shade);
        this.alpha = INITIAL_ALPHA;
        this.roll = this.oRoll = random.nextFloat() * Mth.TWO_PI;
        this.rollSpeed = (random.nextFloat() - 0.5f) * 0.025f;
        this.windInfluence = Mth.lerp(smoothstep((option.size() - 0.227f) / (2.25f - 0.227f)), 0.2f, 0.001f);
        double frequency = 0.16 / (1 + option.size());
        this.flowX = x * frequency;
        this.flowY = y * frequency;
        this.flowZ = z * frequency;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        if (surfaceNormal != null) {
            tickSurface();
            return;
        }
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.oRoll = roll;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        int smokeAge = age;
        float maturity = smoothstep(smokeAge / 45f);
        double time = (level.getGameTime() % 1000000L) * 0.002;
        double lateral = FLOW.noise(flowX + time, flowY, flowZ);
        double vertical = FLOW.noise(flowX, flowY - time * 0.7, flowZ + 31.7);
        double drift = maturity * windInfluence;
        double strength = drift * 0.025;
        this.xd = strength * (sideways.x * lateral + upwards.x * vertical) + drift * 0.006;
        this.yd = strength * (sideways.y * lateral + upwards.y * vertical) + drift * 0.004;
        this.zd = strength * (sideways.z * lateral + upwards.z * vertical) + drift * 0.003;
        move(xd, yd, zd);
        float progress = (float) smokeAge / lifetime;
        float expansion = smoothstep((progress - 0.03f) / 0.97f);
        this.quadSize = Mth.lerp(expansion, initialSize, startSize * 3);
        int fadeStart = Math.max(lingerTicks, 40);
        float fade = Mth.clamp((float) (smokeAge - fadeStart) / (lifetime - fadeStart), 0, 1);
        float smokeAlpha = Mth.lerp(smoothstep(smokeAge / 24f), INITIAL_ALPHA, SMOKE_ALPHA);
        this.alpha = smokeAlpha * (1 - fade) * (1 - fade) / (1 + 1.5f * smoothstep(progress));
        float color = shade - 0.12f * smoothstep(smokeAge / 35f);
        this.setColor(color, color, color);
        this.roll += rollSpeed * maturity * windInfluence;
    }

    public static void spawnSurface(MissileTrailOption option, BlockHitResult impact,
                                    Vec3 exhaust, double distance, int count) {
        Vec3 normal = Vec3.atLowerCornerOf(impact.getDirection().getNormal());
        Vec3 position = impact.getLocation().add(normal.scale(0.15));
        for (int i = 0; i < count; i++) {
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(option,
                    position.x, position.y, position.z, exhaust.x, exhaust.y, exhaust.z);
            if (particle instanceof MissileTrailParticle trail) {
                trail.spreadOnSurface(impact, distance);
            }
        }
    }

    private void spreadOnSurface(BlockHitResult impact, double distance) {
        if (impact.isInside()) {
            remove();
            return;
        }
        surfaceNormal = Vec3.atLowerCornerOf(impact.getDirection().getNormal());
        Vec3 position = impact.getLocation().add(surfaceNormal.scale(0.15));
        setPos(position.x, position.y, position.z);
        Vec3 reference = Math.abs(surfaceNormal.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 tangent = surfaceNormal.cross(reference).normalize();
        Vec3 bitangent = surfaceNormal.cross(tangent);
        double angle = random.nextDouble() * Mth.TWO_PI;
        Vec3 radial = tangent.scale(Math.cos(angle)).add(bitangent.scale(Math.sin(angle)));
        float caliber = option.size() * 1000f;
        float caliberCurve = Mth.sqrt(Mth.clamp(caliber / 2250f, 0, 1));
        float scale = Mth.lerp(caliberCurve, 1.2f, 2.5f);
        float largeCaliber = Mth.lerp(caliberCurve, 0.25f, 1f);
        double strength = Mth.clamp(1 - distance / option.range(), 0, 1)
                * Math.max(0, -exhaust.dot(surfaceNormal));
        double speed = 0.55 * (0.12 + 0.15 * scale) * strength * (1 + 1.6 * largeCaliber)
                * (0.7 - 0.45 * largeCaliber + random.nextDouble() * (0.6 + 0.45 * largeCaliber));
        Vec3 downstream = exhaust.subtract(surfaceNormal.scale(exhaust.dot(surfaceNormal)));
        Vec3 drift = radial.add(downstream.scale(0.65)).scale(speed)
                .add(surfaceNormal.scale(0.012 + random.nextDouble() * 0.025));
        float size = Mth.clamp((0.18f + 0.16f * scale) * (float) (0.6 + 0.4 * strength)
                * (1 + 5 * largeCaliber), 0.1f, 4.8f);
        size = smokeSize(size);
        surfaceBulk = Mth.clamp((size - 0.58f) / 0.9f, 0, 1);
        surfaceAlpha = 0.55f + 0.13f * surfaceBulk;
        startSize = size * (0.85f + random.nextFloat() * 0.3f);
        quadSize = startSize;
        age = 0;
        lifetime = 65 + random.nextInt(35) + Math.round(140 * surfaceBulk);
        setSize(0.12f, 0.12f);
        friction = 0.985f + 0.005f * surfaceBulk;
        xd = drift.x;
        yd = drift.y;
        zd = drift.z;
        float color = 0.76f + random.nextFloat() * 0.12f;
        setColor(color, color * 0.97f, color * 0.91f);
        alpha = surfaceAlpha;
        rollSpeed = (random.nextFloat() - 0.5f) * 0.035f;
    }

    private void tickSurface() {
        oRoll = roll;
        xd += surfaceNormal.x * 0.00016;
        yd += surfaceNormal.y * 0.00016;
        zd += surfaceNormal.z * 0.00016;
        super.tick();
        if (removed) {
            return;
        }
        float progress = (float) age / lifetime;
        float expansion = 1 - (1 - progress) * (1 - progress);
        quadSize = startSize * (1 + 2 * expansion);
        alpha = surfaceAlpha * (1 - progress) * (1 - progress * (1 - 0.75f * surfaceBulk))
                / (1 + (1 - 0.4f * surfaceBulk) * expansion);
        roll += rollSpeed;
    }

    private static float smokeSize(float size) {
        return 0.9f * Mth.sqrt(size);
    }

    private static float smoothstep(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<MissileTrailOption> {
        @Override
        public Particle createParticle(MissileTrailOption option, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new MissileTrailParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, option, sprites);
        }
    }

}
