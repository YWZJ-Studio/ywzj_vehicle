package org.ywzj.vehicle.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.ywzj.vehicle.client.render.item.RepairItemRenderer;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.function.Consumer;

public class RepairToolItem extends Item {
    public RepairToolItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new RepairItemRenderer();
            }
        });
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 72000;
    }

    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        if (pHand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.pass(pPlayer.getItemInHand(pHand));
        }
        return ItemUtils.startUsingInstantly(pLevel, pPlayer, pHand);
    }

    @Override
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
        super.onUseTick(pLevel, pLivingEntity, pStack, pRemainingUseDuration);
        if (pLivingEntity.tickCount % 2 == 0 && pLivingEntity.getTicksUsingItem() > 8) {
            Vec3 viewVector = pLivingEntity.getViewVector(1.0F);
            Vec3 startPos = pLivingEntity.getEyePosition();
            if (pLevel.isClientSide()) {
//                pLevel.addParticle(
//                        ParticleTypes.FLAME,
//                        startPos.x + 0.1 + viewVector.x,
//                        startPos.y - 0.15 + viewVector.y,
//                        startPos.z + viewVector.z,
//                        viewVector.x * 0.2 + pLivingEntity.getDeltaMovement().x + pLivingEntity.getRandom().nextGaussian() * 0.05,
//                        viewVector.y * 0.2,
//                        viewVector.z * 0.2 + pLivingEntity.getDeltaMovement().z + pLivingEntity.getRandom().nextGaussian() * 0.05
//                );
            } else {
                Vec3 endPos = startPos.add(viewVector.scale(3.0));
                var result = pLevel.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pLivingEntity));
                if (result.getType() != HitResult.Type.MISS) {
                    endPos = result.getLocation();
                }
                var hitEntity = ProjectileUtil.getEntityHitResult(
                        pLivingEntity, startPos, endPos,
                        pLivingEntity.getBoundingBox().expandTowards(viewVector.scale(6.0)).inflate(1.0),
                        e -> e instanceof AbstractVehicle vehicle && !vehicle.isDestroyed(), 50
                );
                if (hitEntity != null && hitEntity.getEntity() instanceof AbstractVehicle vehicle) {
                    float h = Math.min(vehicle.getHealth() + 2.0f, vehicle.getMaxHealth());
                    vehicle.setHealth(h);
                }
            }
        }

    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }
}
