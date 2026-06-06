package org.ywzj.vehicle.client.render.entity.weapon;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.entity.weapon.AmmoEntity;
import org.ywzj.vehicle.resource.BedrockModelLoader;

import java.util.Optional;
import java.util.function.Function;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class AmmoEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    private final Function<T, ResourceLocation> weaponIdGetter;
    private final ResourceLocation defaultModel;
    private final ResourceLocation defaultTexture;

    public AmmoEntityRenderer(EntityRendererProvider.Context pContext,
                              Function<T, ResourceLocation> weaponIdGetter,
                              String defaultModelPath,
                              String defaultTexturePath) {
        super(pContext);
        this.weaponIdGetter = weaponIdGetter;
        this.defaultModel = YwzjVehicle.modLocation(defaultModelPath);
        this.defaultTexture = YwzjVehicle.modLocation(defaultTexturePath);
    }

    @Override
    public boolean shouldRender(T pEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return true;
    }

    @Override
    public void render(T pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        pPoseStack.pushPose();
        {
            Vec3 root = new Vec3(0, 0, 0);
            pPoseStack.rotateAround(Axis.YP.rotationDegrees(-pEntityYaw), (float) root.x, (float) root.y, (float) root.z);
            pPoseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot())), (float) root.x, (float) root.y, (float) root.z);

            BedrockModel model = null;
            ResourceLocation texture = null;
            Optional<BaseDisplay> weaponDisplayOptional = ClientAssetsManager.INSTANCE.getWeaponDisplay(weaponIdGetter.apply(pEntity));
            if (weaponDisplayOptional.isPresent()) {
                BaseDisplay weaponDisplay = weaponDisplayOptional.get();
                if (weaponDisplay.getModel() != null) {
                    model = weaponDisplay.getModel();
                }
                if (weaponDisplay.getTexture() != null) {
                    texture = weaponDisplay.getTexture();
                }
            }
            if (model == null) {
                model = BedrockModelLoader.getModel(defaultModel);
            }
            if (texture == null) {
                texture = defaultTexture;
            }
            if (pEntity instanceof AmmoEntity ammoEntity) {
                var runner = ammoEntity.getAnimationRunner();
                if (runner != null) {
                    runner.tick();
                    model.applyPose(BLENDER.blend(model.getBindPose(), runner.evaluate()));
                }
            }
            model.renderToBuffer(pPoseStack, bufferSource,
                    RenderType.entityCutout(texture),
                    BedrockModelRenderTypes.polyMeshCutout(texture),
                    pPackedLight,
                    OverlayTexture.pack(0f, false)
            );
            model.applyPose(model.getBindPose());
        }
        pPoseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(T pEntity) {
        return null;
    }

}
