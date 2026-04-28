package org.ywzj.vehicle.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.ywzj.vehicle.client.render.item.RepairItemRenderer;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.function.Consumer;

public class RepairToolItem extends VehicleItem {

    public RepairToolItem() {
        super(new Properties().durability(100));
    }

    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private RepairItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new RepairItemRenderer();
                }
                return renderer;
            }

        });
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity entity) {
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
        if (pLevel.isClientSide() && pLivingEntity.tickCount % 4 == 0) {
            pLevel.playSound(pLivingEntity, pLivingEntity.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS,
                    0.15F,
                    1.5F);
        }
        if (pLivingEntity.tickCount % 2 == 0 && pLivingEntity.getTicksUsingItem() > 8) {
            Vec3 viewVector = pLivingEntity.getViewVector(1.0F);
            Vec3 startPos = pLivingEntity.getEyePosition();
            Vec3 endPos = startPos.add(viewVector.scale(3.0));
            var result = pLevel.clip(new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pLivingEntity));
            if (result.getType() != HitResult.Type.MISS) {
                endPos = result.getLocation();
            }
            var hitResult = ProjectileUtil.getEntityHitResult(
                    pLivingEntity, startPos, endPos,
                    pLivingEntity.getBoundingBox().expandTowards(viewVector.scale(6.0)).inflate(1.0),
                    Entity::isAlive, 50
            );
            if (hitResult != null) {
                if (hitResult.getEntity() instanceof AbstractVehicle vehicle) {
                    if (pLevel.isClientSide()) {
                        Vec3 pos = hitResult.getLocation();
                        pLevel.addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
                    } else {
                        float h = Math.min(vehicle.getHealth() + 2.0f, vehicle.getMaxHealth());
                        vehicle.setHealth(h);
                    }
                } else if (hitResult.getEntity() instanceof LivingEntity livingEntity) {
                    livingEntity.hurt(pLevel.damageSources().playerAttack((Player) pLivingEntity), 2.0F);
                    livingEntity.igniteForSeconds(3);
                }
            }
            pStack.hurtAndBreak(2, pLivingEntity, EquipmentSlot.MAINHAND);
        }

    }

    @Override
    @SuppressWarnings("removal")
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public InteractionResult interactEntity(ItemStack itemStack, Player player, Entity target, InteractionHand pHand) {
        return InteractionResult.PASS;
    }

}
