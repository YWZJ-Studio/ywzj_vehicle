package org.ywzj.vehicle.item;

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
import org.joml.Vector3f;
import org.ywzj.vehicle.client.render.item.DecorationItemRenderer;
import org.ywzj.vehicle.client.resource.ClientAssetsManager;
import org.ywzj.vehicle.client.resource.vehicle.BaseDisplay;
import org.ywzj.vehicle.client.screen.DecorationSelectScreen;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.network.Channel;
import org.ywzj.vehicle.network.message.ClientDecorationAction;
import org.ywzj.vehicle.util.VectorUtil;
import org.ywzj.vehicle.vehicle.LocalVehiclePlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

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
        Vec3 start = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 end = new Vec3(Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector().mul(8)).add(start);
        Optional<BaseDisplay> displayOptional = ClientAssetsManager.INSTANCE.getVehicleDisplay(vehicle.getDisplayId());
        if (displayOptional.isEmpty()) {
            return;
        }
        BaseDisplay display = displayOptional.get();
        if (display.getModel() == null) {
            return;
        }
        if (!display.getModel().hasBakedModel()) {
            return;
        }
        VectorUtil.HitBone hitBone = VectorUtil.hitBone(vehicle, start, end);
        if (hitBone != null) {
            Vec3 hitPos = hitBone.position();
            vehicle.level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.0F),
                    true, hitPos.x, hitPos.y, hitPos.z,
                    0, 0, 0);
            vehicle.level().playSound(LocalVehiclePlayer.instance.getPlayer(), hitPos.x, hitPos.y, hitPos.z,
                    SoundEvents.WOOL_PLACE, SoundSource.PLAYERS,
                    1f, 1f);
            ClientDecorationAction clientDecorationAction = new ClientDecorationAction();
            clientDecorationAction.action = ClientDecorationAction.Action.SET;
            clientDecorationAction.displayId = itemStack.getOrCreateTag().getString(TAG_DECORATION_DISPLAY_ID);
            if (StringUtils.isEmpty(clientDecorationAction.displayId)) {
                return;
            }
            clientDecorationAction.vehicleId = vehicle.getId();
            clientDecorationAction.decorationUnitId = UUID.randomUUID().toString();
            clientDecorationAction.baseBoneName = hitBone.boneName();
            clientDecorationAction.scale = 1;
            Vector3f rot = new Vector3f();
            hitBone.rotation().getEulerAnglesYXZ(rot);
            clientDecorationAction.selfXRot = (float) Math.toDegrees(rot.x);
            clientDecorationAction.selfYRot = (float) Math.toDegrees(-rot.y);
            clientDecorationAction.selfZRot = (float) Math.toDegrees(rot.z);
            clientDecorationAction.offsetFromBone = hitBone.offset();
            Channel.CHANNEL.sendToServer(clientDecorationAction);
        }
    }

}
