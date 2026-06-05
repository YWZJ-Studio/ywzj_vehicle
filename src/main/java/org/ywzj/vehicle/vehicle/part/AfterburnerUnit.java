package org.ywzj.vehicle.vehicle.part;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.control.runner.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.part.data.PartUnitPojo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class AfterburnerUnit extends SwitchableUnit<PartUnitData> {

    private static final ResourceLocation MODEL_PATH = YwzjVehicle.modLocation("effect/afterburner_flame");
    private static final ResourceLocation ANIMATION_PATH = YwzjVehicle.modLocation("effect/afterburner_flame.animation");
    private static final ResourceLocation TEXTURE_PATH = YwzjVehicle.modLocation("textures/entity/afterburner_flame.png");
    private static VehicleBedrockModel model;
    private static Map<String, BedrockAnimation> animationMap;
    private final Vec3 afterburnerOffset;
    private final float scale;
    private State state = State.IDLE;
    private boolean wasOn;
    private AnimationRunner currentRunner;
    private enum State { IDLE, STARTING, SUSTAINING, CLOSING }

    public AfterburnerUnit(int index, AbstractVehicle vehicle, Vec3 afterburnerOffset, float scale) {
        super(index, vehicle, createData(index));
        this.afterburnerOffset = afterburnerOffset;
        this.scale = scale;
        this.setPivotOffset(afterburnerOffset);
    }

    public static void init() {
        model = ClientAssetsManager.INSTANCE.getModel(MODEL_PATH)
                .map(pojo -> new VehicleBedrockModel(pojo, List.of()))
                .orElse(null);
        animationMap = ClientAssetsManager.INSTANCE.getAnimation(ANIMATION_PATH)
                .map(pojo -> {
                    Map<String, BedrockAnimation> map = new HashMap<>();
                    for (BedrockAnimation anim : BedrockAnimation.createAnimation(pojo, model)) {
                        map.put(anim.getName(), anim);
                    }
                    return map;
                })
                .orElse(new HashMap<>());
    }

    private static PartUnitData createData(int index) {
        PartUnitPojo pojo = new PartUnitPojo();
        pojo.id = "afterburner_" + index;
        pojo.name = "afterburner_" + index;
        pojo.isSeat = false;
        return new PartUnitData(pojo);
    }

    @Override
    public void tick() {
        super.tick();
        if (vehicle.level().isClientSide()) {
            boolean on = isOn();
            if (on != wasOn) {
                if (on) {
                    transitionToStarting();
                } else {
                    transitionToClosing();
                }
                wasOn = on;
            }
            if (state == State.STARTING && currentRunner.getAnimationContext().isEnd()) {
                transitionToSustaining();
            } else if (state == State.CLOSING && currentRunner.getAnimationContext().isEnd()) {
                transitionToIdle();
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack pPoseStack, MultiBufferSource bufferSource, int pPackedLight) {
        if (model == null || animationMap == null) {
            return;
        }
        if (currentRunner != null) {
            currentRunner.tick();
            model.applyPose(BLENDER.blend(model.getBindPose(), currentRunner.evaluate()));
        } else if (state == State.IDLE) {
            return;
        }
        pPoseStack.pushPose();
        {
            pPoseStack.translate(afterburnerOffset.x, afterburnerOffset.y, afterburnerOffset.z);
            pPoseStack.scale(scale, scale, scale);
            model.renderToBuffer(pPoseStack, bufferSource,
                    RenderType.entityTranslucent(TEXTURE_PATH),
                    BedrockModelRenderTypes.polyMeshCutout(TEXTURE_PATH),
                    pPackedLight,
                    OverlayTexture.pack(0f, false));
        }
        pPoseStack.popPose();
    }

    private void transitionToStarting() {
        BedrockAnimation anim = animationMap.get("afterburner_on");
        if (anim == null) return;
        currentRunner = new AnimationRunner(anim, new AnimationContext(anim.getSpecifiedEndTimeS()));
        currentRunner.setState(new PlayingState(System::nanoTime, StopState::new));
        state = State.STARTING;
    }

    private void transitionToSustaining() {
        BedrockAnimation anim = animationMap.get("afterburner_loop");
        if (anim == null) return;
        currentRunner = new AnimationRunner(anim, new AnimationContext(anim.getSpecifiedEndTimeS()));
        currentRunner.setState(new LoopingState(System::nanoTime));
        state = State.SUSTAINING;
    }

    private void transitionToClosing() {
        BedrockAnimation anim = animationMap.get("afterburner_off");
        if (anim == null) return;
        currentRunner = new AnimationRunner(anim, new AnimationContext(anim.getSpecifiedEndTimeS()));
        currentRunner.setState(new PlayingState(System::nanoTime, StopState::new));
        state = State.CLOSING;
    }

    private void transitionToIdle() {
        currentRunner = null;
        state = State.IDLE;
    }

    public List<OBB> getOBBs() {
        return List.of();
    }

    public List<VehicleCubeOBB> getPartCubeOBBs() {
        return List.of();
    }

}
