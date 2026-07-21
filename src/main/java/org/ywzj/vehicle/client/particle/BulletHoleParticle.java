package org.ywzj.vehicle.client.particle;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.particle.BulletHoleOption;

/** 弹孔贴花同时兼容 v1 骨骼绑定和 baked 实例附件骨骼绑定。 */
@OnlyIn(Dist.CLIENT)
public class BulletHoleParticle extends TextureSheetParticle {

    private final Direction direction;
    private final BlockPos pos;
    private int uOffset;
    private int vOffset;
    private float textureDensity;
    private AbstractVehicle vehicle;
    private BedrockBone legacyBone;
    private int entityId;
    private int attachmentBoneIndex = -1;
    private Vec3 offsetFromBone;
    private Quaternionf selfRotation;

    public BulletHoleParticle(ClientLevel level, double x, double y, double z,
                              Direction direction, BlockPos pos, float r, float g, float b, float caliber) {
        super(level, x, y, z);
        this.setSprite(getBlockSprite(pos));
        this.direction = direction;
        this.pos = pos;
        this.lifetime = 600 + level.random.nextInt(100);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = (float) (0.03f * Math.pow(caliber / 5.8f, 0.5f));
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.alpha = 0.9F;
        if (level.getBlockState(pos).isAir()) {
            this.remove();
        }
    }

    public BulletHoleParticle(ClientLevel level, double x, double y, double z,
                              Direction direction, BlockPos pos, float r, float g, float b, float caliber,
                              int entityId, String boneName, Quaternionf selfRotation, Vec3 offsetFromBone) {
        this(level, x, y, z, direction, pos, r, g, b, caliber);
        if (this.removed || !(level.getEntity(entityId) instanceof AbstractVehicle entity)) {
            this.remove();
            return;
        }
        this.vehicle = entity;
        var display = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId()).orElse(null);
        if (display == null || display.getModel() == null) {
            this.remove();
            return;
        }
        this.legacyBone = display.getModel().getBoneMap().get(boneName);
        if (legacyBone == null) {
            this.remove();
            return;
        }
        this.entityId = entityId;
        this.offsetFromBone = offsetFromBone;
        this.selfRotation = new Quaternionf(selfRotation);
    }

    public BulletHoleParticle(ClientLevel level, double x, double y, double z,
                              Direction direction, BlockPos pos, float r, float g, float b, float caliber,
                              int entityId, int attachmentBoneIndex, Quaternionf selfRotation, Vec3 attachmentOffset) {
        this(level, x, y, z, direction, pos, r, g, b, caliber);
        if (this.removed || !(level.getEntity(entityId) instanceof AbstractVehicle entity)) {
            this.remove();
            return;
        }
        this.vehicle = entity;
        this.entityId = entityId;
        this.attachmentBoneIndex = attachmentBoneIndex;
        this.offsetFromBone = attachmentOffset;
        this.selfRotation = new Quaternionf(selfRotation);
    }

    @Override
    public void remove() {
        this.removed = true;
        if (vehicle != null) {
            vehicle.getBulletHoleParticles().remove(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (vehicle != null) {
            if (!vehicle.isAlive()) {
                this.remove();
            }
        } else if (this.level.getBlockState(this.pos).isAir()) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        if (vehicle != null) {
            return;
        }
        Vec3 view = renderInfo.getPosition();
        Vector3f[] points = newQuadPoints();
        float scale = this.getQuadSize(partialTicks);
        for (Vector3f point : points) {
            point.rotate(this.direction.getRotation());
            point.mul(scale);
            point.add((float) (this.x - view.x()), (float) (this.y - view.y()), (float) (this.z - view.z()));
        }
        emitDoubleSidedQuad(buffer, points, renderData());
    }

    /** 维持专用 v1 renderer 的原有弹孔路径。 */
    public void renderOnVehicle(float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (vehicle == null || legacyBone == null) {
            return;
        }
        Quaternionf globalRotation = new Quaternionf(legacyBone.rotation);
        globalRotation.premul(selfRotation);
        globalRotation.rotateX((float) (Math.PI / 2));
        Vector3f offset = legacyBone.rotation.transform(offsetFromBone.toVector3f());
        Vector3f globalPivot = new Vector3f(legacyBone.x / 16.0F + offset.x, legacyBone.y / 16.0F + offset.y, legacyBone.z / 16.0F + offset.z);
        BedrockBone parent = legacyBone.parent;
        while (parent != null) {
            parent.rotation.transform(globalPivot);
            globalPivot.add(parent.x / 16, parent.y / 16, parent.z / 16);
            globalRotation.premul(parent.rotation);
            parent = parent.parent;
        }
        Vector3f[] points = newQuadPoints();
        float scale = this.getQuadSize(partialTicks);
        for (Vector3f point : points) {
            point.rotate(globalRotation);
            point.mul(scale);
            point.add(globalPivot);
        }
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        emitDoubleSidedQuad(buffer, poseStack.last(), points, renderData());
    }

    /** baked 通用载具路径使用当前实体实例的附件骨骼或静态模型根变换。 */
    public void renderOnVehicle(float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource,
                                BakedModelInstance modelInstance) {
        if (vehicle == null || legacyBone != null || offsetFromBone == null) {
            return;
        }
        Matrix4f attachmentTransform = new Matrix4f();
        if (attachmentBoneIndex >= 0) {
            if (modelInstance.getBone(attachmentBoneIndex) == null) {
                return;
            }
            attachmentTransform = modelInstance.getGlobalTransform(attachmentBoneIndex);
        }
        Vector3f localOffset = offsetFromBone.toVector3f();
        localOffset.add(new Vector3f(0.0F, 0.002F, 0.0F).rotate(selfRotation));
        Vector3f[] points = newQuadPoints();
        float scale = this.getQuadSize(partialTicks);
        for (Vector3f point : points) {
            point.rotate(selfRotation);
            point.mul(scale);
            point.add(localOffset);
            point.mulPosition(attachmentTransform);
        }
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        emitDoubleSidedQuad(buffer, poseStack.last(), points, renderData());
    }

    private static Vector3f[] newQuadPoints() {
        return new Vector3f[]{
                new Vector3f(-1.0F, 0.01F, -1.0F), new Vector3f(-1.0F, 0.01F, 1.0F),
                new Vector3f(1.0F, 0.01F, 1.0F), new Vector3f(1.0F, 0.01F, -1.0F)
        };
    }

    private record RenderData(float u0, float u1, float v0, float v1, float red, float green, float blue, float alpha, int lightColor) {}

    private RenderData renderData() {
        int light = Math.max(15 - this.age / 2, 0);
        int lightColor = LightTexture.pack(light, light);
        float colorPercent = light / 15.0f;
        float fade = 1.0f - (float) this.age / this.lifetime;
        return new RenderData(getU0(), getU1(), getV0(), getV1(), rCol * colorPercent, gCol * colorPercent,
                bCol * colorPercent, alpha * fade, lightColor);
    }

    private static void emitDoubleSidedQuad(VertexConsumer buffer, Vector3f[] points, RenderData data) {
        vertex(buffer, points[0], data.u1(), data.v1(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
        vertex(buffer, points[1], data.u1(), data.v0(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
        vertex(buffer, points[2], data.u0(), data.v0(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
        vertex(buffer, points[3], data.u0(), data.v1(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
        vertex(buffer, points[3], data.u0(), data.v1(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
        vertex(buffer, points[2], data.u0(), data.v0(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
        vertex(buffer, points[1], data.u1(), data.v0(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
        vertex(buffer, points[0], data.u1(), data.v1(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
    }

    private static void emitDoubleSidedQuad(VertexConsumer buffer, PoseStack.Pose pose, Vector3f[] points, RenderData data) {
        for (int[] index : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
            vertex(buffer, pose, points[index[0]], data.u1(), data.v1(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
            vertex(buffer, pose, points[index[1]], data.u1(), data.v0(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
            vertex(buffer, pose, points[index[2]], data.u0(), data.v0(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
            vertex(buffer, pose, points[index[3]], data.u0(), data.v1(), data.red(), data.green(), data.blue(), data.alpha(), data.lightColor());
        }
    }

    private static void vertex(VertexConsumer buffer, Vector3f point, float u, float v, float red, float green, float blue, float alpha, int lightColor) {
        buffer.vertex(point.x(), point.y(), point.z()).uv(u, v).color(red, green, blue, alpha).uv2(lightColor).endVertex();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3f point, float u, float v, float red, float green, float blue, float alpha, int lightColor) {
        buffer.vertex(pose.pose(), point.x(), point.y(), point.z()).color(red, green, blue, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightColor).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }

    private static TextureAtlasSprite getBlockSprite(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            BlockState state = minecraft.level.getBlockState(pos);
            return minecraft.getBlockRenderer().getBlockModelShaper().getTexture(state, minecraft.level, pos);
        }
        return minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.TERRAIN_SHEET; }

    @Override
    protected void setSprite(TextureAtlasSprite sprite) {
        super.setSprite(sprite);
        uOffset = random.nextInt(16);
        vOffset = random.nextInt(16);
        textureDensity = (sprite.getU1() - sprite.getU0()) / 16.0F;
    }

    @Override
    protected float getU0() { return sprite.getU0() + uOffset * textureDensity; }
    @Override
    protected float getV0() { return sprite.getV0() + vOffset * textureDensity; }
    @Override
    protected float getU1() { return getU0() + textureDensity; }
    @Override
    protected float getV1() { return getV0() + textureDensity; }

    public static class Provider implements ParticleProvider<BulletHoleOption> {
        @Override
        public Particle createParticle(BulletHoleOption option, ClientLevel world, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            if (option.getEntityId() >= 0 && option.isBakedAttachment()) {
                return new BulletHoleParticle(world, x, y, z, option.getDirection(), option.getPos(), option.getR(), option.getG(), option.getB(), option.getCaliber(),
                        option.getEntityId(), option.getAttachmentBoneIndex(), option.getSelfRotation(), option.getBoneOffset());
            }
            if (option.getEntityId() >= 0) {
                return new BulletHoleParticle(world, x, y, z, option.getDirection(), option.getPos(), option.getR(), option.getG(), option.getB(), option.getCaliber(),
                        option.getEntityId(), option.getBoneName(), option.getSelfRotation(), option.getBoneOffset());
            }
            return new BulletHoleParticle(world, x, y, z, option.getDirection(), option.getPos(), option.getR(), option.getG(), option.getB(), option.getCaliber());
        }
    }
}
