package org.ywzj.vehicle.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.ywzj.vehicle.api.animation.IAnimationEntity;
import org.ywzj.vehicle.client.render.item.DecorationItemRenderer;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.screen.DecorationSelectScreen;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientDecorationAction;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;
import org.ywzj.vehicle.vehicle.structure.OBB;

import java.util.*;
import java.util.function.Consumer;

import static org.ywzj.vehicle.client.render.animation.util.PoseBlenders.BLENDER;

public class DecorationItem extends VehicleItem {

    public static final String TAG_DECORATION_DISPLAY_ID = "YwzjVehicleDecorationDisplayId";

    public DecorationItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private DecorationItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new DecorationItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }

        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (player.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            openScreen();
        }
        return InteractionResultHolder.pass(itemStack);
    }

    @Override
    public InteractionResult interactEntity(ItemStack itemStack, Player player, Entity target, InteractionHand hand) {
        if (player.level().isClientSide() && hand == InteractionHand.MAIN_HAND && target instanceof AbstractVehicle vehicle) {
            decorate(vehicle, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    public void openScreen() {
        if (!(Minecraft.getInstance().hitResult instanceof EntityHitResult)) {
            Minecraft.getInstance().setScreen(new DecorationSelectScreen());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void decorate(AbstractVehicle vehicle, ItemStack itemStack) {
        Vector3f start = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f();
        Vector3f end = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector().mul(8).add(start);
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId());
        if (displayOptional.isEmpty()) {
            return;
        }
        BaseDisplay display = displayOptional.get();
        BedrockModel model = display.getModel();
        if (model == null) {
            return;
        }

        if (vehicle instanceof IAnimationEntity<?,?> animationEntity) {
            var instance = animationEntity.getAnimationInstance();
            if (instance != null) {
                instance.getContext().setPartialTick(1);
                model.applyPose(BLENDER.blend(model.getBindPose(), instance.getCurrentPose()));
            }
        }
        String decorationBoneName = null;
        Vec3 decorationOffset = null;
        Quaternionf decorationRotation = null;
        double minDistance = Double.MAX_VALUE;
        HashSet<BedrockBone> namedBones = new HashSet<>(model.getBoneMap().values());
        for (Map.Entry<String, BedrockBone> boneEntry : model.getBoneMap().entrySet()) {
            BedrockBone bone = boneEntry.getValue();
            List<OBB.CubeOBB> cubeOBBs = OBB.getOBBsFromBone(bone, vehicle, namedBones);
            for (OBB.CubeOBB cubeOBB : cubeOBBs) {
                OBB obb = cubeOBB.obb();
                Vector3f[] axes = obb.getAxes();
                Optional<Vector3f> hitPosOptional = obb.clip(start, end);
                if (hitPosOptional.isEmpty()) {
                    continue;
                }
                Vector3f hitPos = hitPosOptional.get();
                if (Double.isNaN(hitPos.x) || Double.isNaN(hitPos.y) || Double.isNaN(hitPos.z)) {
                    continue;
                }
                double distance = hitPos.distance(start);
                if (distance > minDistance) {
                    continue;
                }
                minDistance = distance;
                Matrix4f globalTransform = bone.getGlobalTransform();
                Vector3f offset = vehicle.relativeRotDirection(new Vec3(hitPos).subtract(vehicle.position()), true)
                        .toVector3f()
                        .sub(globalTransform.transformPosition(new Vector3f()));
                globalTransform.getUnnormalizedRotation(new Quaternionf())
                        .conjugate()
                        .transform(offset);
                Vector3f center = obb.center();
                Vector3f localHit = new Vector3f(hitPos).sub(center);
                float xDist = localHit.dot(axes[0]);
                float yDist = localHit.dot(axes[1]);
                float zDist = localHit.dot(axes[2]);
                Vector3f extents = obb.extents();
                float fx = Math.abs(xDist / extents.x);
                float fy = Math.abs(yDist / extents.y);
                float fz = Math.abs(zDist / extents.z);
                float max = Math.max(fx, Math.max(fy, fz));
                Quaternionf cubeRot = new Quaternionf(cubeOBB.bone().rotation);
                if (max == fx) {
                    if (xDist > 0) {
                        cubeRot.rotateY((float) (Math.PI / 2));
                    } else {
                        cubeRot.rotateY((float) (-Math.PI / 2));
                    }
                } else if (max == fy) {
                    if (yDist > 0) {
                        cubeRot.rotateX((float) (-Math.PI / 2));
                    } else {
                        cubeRot.rotateX((float) (Math.PI / 2));
                    }
                } else {
                    if (zDist < 0) {
                        cubeRot.rotateY((float) Math.PI);
                    }
                }
                BedrockBone parent = cubeOBB.bone().parent;
                while (parent != null && !namedBones.contains(parent)) {
                    cubeRot.premul(parent.rotation);
                    parent = parent.parent;
                }
                vehicle.level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.0F),
                        true, hitPos.x, hitPos.y, hitPos.z,
                        0, 0, 0);
                vehicle.level().playSound(LocalVehiclePlayer.instance.getPlayer(), hitPos.x, hitPos.y, hitPos.z,
                        SoundEvents.WOOL_PLACE, SoundSource.PLAYERS,
                        1f, 1f);
                decorationRotation = cubeRot;
                decorationOffset = new Vec3(offset);
                decorationBoneName = boneEntry.getKey();
            }
        }
        model.applyPose(model.getBindPose());

        if (decorationBoneName != null) {
            ClientDecorationAction clientDecorationAction = new ClientDecorationAction();
            clientDecorationAction.action = ClientDecorationAction.Action.SET;
            clientDecorationAction.displayId = itemStack.getOrCreateTag().getString(TAG_DECORATION_DISPLAY_ID);
            if (StringUtils.isEmpty(clientDecorationAction.displayId)) {
                return;
            }
            clientDecorationAction.vehicleId = vehicle.getId();
            clientDecorationAction.decorationUnitId = UUID.randomUUID().toString();
            clientDecorationAction.baseBoneName = decorationBoneName;
            clientDecorationAction.scale = 1;
            Vector3f rot = new Vector3f();
            decorationRotation.getEulerAnglesYXZ(rot);
            clientDecorationAction.selfXRot = (float) Math.toDegrees(rot.x);
            clientDecorationAction.selfYRot = (float) Math.toDegrees(-rot.y);
            clientDecorationAction.selfZRot = (float) Math.toDegrees(rot.z);
            clientDecorationAction.offsetFromBone = decorationOffset;
            Channel.CHANNEL.sendToServer(clientDecorationAction);
        }
    }

}
