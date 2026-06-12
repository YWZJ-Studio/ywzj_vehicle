package org.ywzj.vehicle.vehicle.part;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.control.runner.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.FixedWingVehicleDisplay;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;
import org.ywzj.vehicle.custom.part.data.PartUnitData;
import org.ywzj.vehicle.custom.part.data.PartUnitPojo;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.structure.OBB;
import org.ywzj.vehicle.vehicle.structure.VehicleCubeOBB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class AfterburnerUnit extends SwitchableUnit<PartUnitData> {

    private final Vec3 afterburnerOffset;
    private final float scale;
    private State state = State.IDLE;
    private boolean wasOn;
    private AnimationRunner currentRunner;
    private float xRot;
    private float yRot;
    private enum State { IDLE, STARTING, SUSTAINING, CLOSING }

    public AfterburnerUnit(int index, AbstractVehicle vehicle, Vec3 afterburnerOffset, float scale) {
        super(index, vehicle, createData(index));
        this.afterburnerOffset = afterburnerOffset;
        this.scale = scale;
        this.setPivotOffset(afterburnerOffset);
    }

    private static PartUnitData createData(int index) {
        PartUnitPojo pojo = new PartUnitPojo();
        pojo.id = "afterburner_" + index;
        pojo.name = "afterburner_" + index;
        pojo.isSeat = false;
        return new PartUnitData(pojo);
    }

    @Nullable
    private FixedWingVehicleDisplay getFixedWingDisplay() {
        var opt = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId());
        if (opt.isPresent() && opt.get() instanceof FixedWingVehicleDisplay display) {
            return display;
        }
        return null;
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
        FixedWingVehicleDisplay display = getFixedWingDisplay();
        if (display == null) {
            return;
        }
        VehicleBedrockModel afterburnerModel = display.getAfterburnerModel();
        Map<String, BedrockAnimation> animMap = display.getAfterburnerAnimations();
        ResourceLocation texture = display.getAfterburnerTexture();
        if (afterburnerModel == null || animMap == null || texture == null) {
            return;
        }
        if (currentRunner != null) {
            currentRunner.tick();
            afterburnerModel.applyPose(BLENDER.blend(afterburnerModel.getBindPose(), currentRunner.evaluate()));
        } else if (state == State.IDLE) {
            return;
        }
        pPoseStack.pushPose();
        {
            pPoseStack.translate(afterburnerOffset.x, afterburnerOffset.y, afterburnerOffset.z);
            pPoseStack.scale(scale, scale, scale);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(xRot));
            afterburnerModel.renderToBuffer(pPoseStack, bufferSource,
                    RenderType.entityTranslucent(texture),
                    BedrockModelRenderTypes.polyMeshCutout(texture),
                    pPackedLight,
                    OverlayTexture.pack(0f, false));
        }
        pPoseStack.popPose();
    }

    private void transitionToStarting() {
        FixedWingVehicleDisplay display = getFixedWingDisplay();
        if (display == null) {
            return;
        }
        Map<String, BedrockAnimation> animations = display.getAfterburnerAnimations();
        if (animations == null) {
            return;
        }
        BedrockAnimation animation = animations.get("afterburner_on");
        if (animation == null) {
            return;
        }
        currentRunner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
        currentRunner.setState(new PlayingState(System::nanoTime, StopState::new));
        state = State.STARTING;
    }

    private void transitionToSustaining() {
        FixedWingVehicleDisplay display = getFixedWingDisplay();
        if (display == null) {
            return;
        }
        Map<String, BedrockAnimation> animations = display.getAfterburnerAnimations();
        if (animations == null) {
            return;
        }
        BedrockAnimation animation = animations.get("afterburner_loop");
        if (animation == null) {
            return;
        }
        currentRunner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
        currentRunner.setState(new LoopingState(System::nanoTime));
        state = State.SUSTAINING;
    }

    private void transitionToClosing() {
        FixedWingVehicleDisplay display = getFixedWingDisplay();
        if (display == null) {
            return;
        }
        Map<String, BedrockAnimation> animations = display.getAfterburnerAnimations();
        if (animations == null) {
            return;
        }
        BedrockAnimation animation = animations.get("afterburner_off");
        if (animation == null) {
            return;
        }
        currentRunner = new AnimationRunner(animation, new AnimationContext(animation.getSpecifiedEndTimeS()));
        currentRunner.setState(new PlayingState(System::nanoTime, StopState::new));
        state = State.CLOSING;
    }

    private void transitionToIdle() {
        currentRunner = null;
        state = State.IDLE;
    }

    public float getXRot() {
        return xRot;
    }

    public void setXRot(float xRot) {
        this.xRot = xRot;
    }

    public float getYRot() {
        return yRot;
    }

    public void setYRot(float yRot) {
        this.yRot = yRot;
    }

    public List<OBB> getOBBs() {
        return List.of();
    }

    public List<VehicleCubeOBB> getPartCubeOBBs() {
        return List.of();
    }

}
