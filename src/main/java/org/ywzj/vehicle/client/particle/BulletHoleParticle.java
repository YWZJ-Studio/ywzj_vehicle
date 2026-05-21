package org.ywzj.vehicle.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.particle.BulletHoleOption;

/**
 * 弹孔贴花粒子：在被命中方块表面渲染方块纹理的小贴片，从曳光颜色渐变到黑色后淡出。
 * 参考自TaCZ
 */
@OnlyIn(Dist.CLIENT)
public class BulletHoleParticle extends TextureSheetParticle {

    private final Direction direction;
    private final BlockPos pos;
    private int uOffset;
    private int vOffset;
    private float textureDensity;

    public BulletHoleParticle(ClientLevel world, double x, double y, double z,
                               Direction direction, BlockPos pos, float r, float g, float b, float caliber) {
        super(world, x, y, z);
        this.setSprite(getBlockSprite(world, pos));
        this.direction = direction;
        this.pos = pos;
        this.lifetime = 200 + world.random.nextInt(80);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = (float) (0.03f * Math.pow(caliber / 5.8f, 0.5f));
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.alpha = 0.9F;

        if (world.getBlockState(pos).isAir()) {
            this.remove();
        }
    }

    @Override
    protected void setSprite(TextureAtlasSprite sprite) {
        super.setSprite(sprite);
        this.uOffset = this.random.nextInt(16);
        this.vOffset = this.random.nextInt(16);
        this.textureDensity = (sprite.getU1() - sprite.getU0()) / 16.0F;
    }

    private static TextureAtlasSprite getBlockSprite(ClientLevel world, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            BlockState state = minecraft.level.getBlockState(pos);
            return minecraft.getBlockRenderer().getBlockModelShaper().getTexture(state, minecraft.level, pos);
        }
        return minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    protected float getU0() { return this.sprite.getU0() + this.uOffset * this.textureDensity; }
    @Override
    protected float getV0() { return this.sprite.getV0() + this.vOffset * this.textureDensity; }
    @Override
    protected float getU1() { return this.getU0() + this.textureDensity; }
    @Override
    protected float getV1() { return this.getV0() + this.textureDensity; }

    @Override
    public void tick() {
        super.tick();
        if (this.level.getBlockState(this.pos).isAir()) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Vec3 view = renderInfo.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - view.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - view.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - view.z());
        Quaternionf quaternion = this.direction.getRotation();
        Vector3f[] points = new Vector3f[]{
                new Vector3f(-1.0F, 0.01F, -1.0F),
                new Vector3f(-1.0F, 0.01F, 1.0F),
                new Vector3f(1.0F, 0.01F, 1.0F),
                new Vector3f(1.0F, 0.01F, -1.0F)
        };
        float scale = this.getQuadSize(partialTicks);

        for (int i = 0; i < 4; ++i) {
            Vector3f v = points[i];
            v.rotate(quaternion);
            v.mul(scale);
            v.add(px, py, pz);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        int light = Math.max(15 - this.age / 2, 0);
        int lightColor = LightTexture.pack(light, light);

        float colorPercent = light / 15.0f;
        float red = this.rCol * colorPercent;
        float green = this.gCol * colorPercent;
        float blue = this.bCol * colorPercent;

        float fade = 1.0f - (float) this.age / this.lifetime;
        float alphaFade = this.alpha * fade;

        buffer.vertex(points[0].x(), points[0].y(), points[0].z()).uv(u1, v1).color(red, green, blue, alphaFade).uv2(lightColor).endVertex();
        buffer.vertex(points[1].x(), points[1].y(), points[1].z()).uv(u1, v0).color(red, green, blue, alphaFade).uv2(lightColor).endVertex();
        buffer.vertex(points[2].x(), points[2].y(), points[2].z()).uv(u0, v0).color(red, green, blue, alphaFade).uv2(lightColor).endVertex();
        buffer.vertex(points[3].x(), points[3].y(), points[3].z()).uv(u0, v1).color(red, green, blue, alphaFade).uv2(lightColor).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<BulletHoleOption> {
        @Override
        public Particle createParticle(BulletHoleOption option, ClientLevel world, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
            return new BulletHoleParticle(world, x, y, z,
                    option.getDirection(), option.getPos(), option.getR(), option.getG(), option.getB(), option.getCaliber());
        }
    }

}
