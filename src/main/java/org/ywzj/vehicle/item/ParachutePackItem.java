package org.ywzj.vehicle.item;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.GeoArmorRendererV2;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.resource.BedrockModelResources;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.entity.misc.ParagliderCanopy;
import org.ywzj.vehicle.resource.ParachuteModels;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = YwzjVehicle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ParachutePackItem extends ArmorItem {

    private static final String OPEN_TAG = "ParachutePackOpen";
    private static final String CANOPY_ID_TAG = "ParachutePackCanopyId";
    private static final double MIN_OPEN_DESCENT_SPEED = 0.5;
    private static final double OPEN_VERTICAL_SPEED = -0.05;
    private static final double GLIDE_RATIO = 1.5;
    private static final double MAX_HORIZONTAL_SPEED = 0.6;
    private static final double INERTIA = 0.1;

    public ParachutePackItem(Type type, Properties properties) {
        super(ParachutePackArmorMaterial.INSTANCE, type, properties);
    }

    public static boolean canOpen(LivingEntity entity) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        return stack.is(AllItems.PARACHUTE_PACK.get())
                && stack.getDamageValue() == 0
                && !isOpen(stack)
                && !entity.onGround()
                && !entity.isInWater()
                && !entity.isPassenger()
                && entity.getDeltaMovement().y <= -MIN_OPEN_DESCENT_SPEED;
    }

    public static void open(ServerPlayer player) {
        if (!canOpen(player)) {
            return;
        }
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(OPEN_TAG, true);
        stack.setDamageValue(1);
        player.startFallFlying();
        player.fallDistance = 0;
        ParagliderCanopy canopy = new ParagliderCanopy(AllEntities.PARAGLIDER_CANOPY.get(), player.level());
        canopy.equip(player);
        tag.putInt(CANOPY_ID_TAG, canopy.getId());
        player.level().playSound(null, player, AllSounds.PARACHUTE_OPEN.get(), SoundSource.PLAYERS, 3F, 1F);
    }

    public static boolean isOpen(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(OPEN_TAG);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return isOpen(stack);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        return isOpen(stack);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        return 0;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ItemTags.WOOL);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        boolean equipped = player.getItemBySlot(EquipmentSlot.CHEST) == stack;
        boolean open = isOpen(stack);
        if (!equipped || player.onGround() || player.isInWater() || player.isDeadOrDying()) {
            if (open) {
                close(stack, player, equipped && (player.onGround() || player.isInWater()));
            }
            return;
        }
        if (!open) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 movement = player.getDeltaMovement();
        Vec3 horizontalLook = new Vec3(look.x, 0, look.z);
        if (horizontalLook.lengthSqr() > 1.0E-6) {
            horizontalLook = horizontalLook.normalize();
        }
        double descentSpeed = Math.max(0, -movement.y);
        double horizontalSpeed = Mth.clamp(descentSpeed * GLIDE_RATIO, 0, MAX_HORIZONTAL_SPEED);
        double verticalSpeed = movement.y < 0 ? OPEN_VERTICAL_SPEED : movement.y;
        Vec3 target = horizontalLook.scale(horizontalSpeed).add(0, verticalSpeed, 0);
        player.setDeltaMovement(movement.scale(1 - INERTIA).add(target.scale(INERTIA)));
        player.fallDistance = 0;
        player.hurtMarked = true;
    }

    private static void close(ItemStack stack, ServerPlayer player, boolean landed) {
        CompoundTag tag = stack.getOrCreateTag();
        Entity canopy = player.level().getEntity(tag.getInt(CANOPY_ID_TAG));
        if (canopy instanceof ParagliderCanopy paragliderCanopy) {
            paragliderCanopy.fallThenDiscard();
        }
        tag.remove(OPEN_TAG);
        tag.remove(CANOPY_ID_TAG);
        if (landed) {
            player.fallDistance = 0;
            player.level().playSound(null, player, AllSounds.PARACHUTE_DOWN.get(), SoundSource.PLAYERS, 3F, 1F);
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack parachutePack = event.getLeft();
        if (!parachutePack.is(AllItems.PARACHUTE_PACK.get()) || parachutePack.getDamageValue() == 0 || !event.getRight().is(ItemTags.WOOL)) {
            return;
        }
        ItemStack repaired = parachutePack.copy();
        repaired.setDamageValue(0);
        if (repaired.hasTag()) {
            repaired.getTag().remove(OPEN_TAG);
            repaired.getTag().remove(CANOPY_ID_TAG);
        }
        event.setOutput(repaired);
        event.setMaterialCost(1);
        event.setCost(1);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private GeoArmorRendererV2 renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                                    EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (renderer == null) {
                    TreeBedrockModel model = BedrockModelResources.getInstance().getTreeModel(ParachuteModels.PARACHUTE_PACK);
                    renderer = new GeoArmorRendererV2(model, EquipmentSlot.CHEST, ParachuteModels.PARACHUTE_PACK_TEXTURE);
                }
                renderer.preparePose(livingEntity, itemStack, equipmentSlot, original);
                return renderer;
            }

        });
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return ParachuteModels.PARACHUTE_PACK_TEXTURE.toString();
    }

}

enum ParachutePackArmorMaterial implements ArmorMaterial {

    INSTANCE;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 1;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public @NotNull SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public @NotNull Ingredient getRepairIngredient() {
        return Ingredient.of(ItemTags.WOOL);
    }

    @Override
    public @NotNull String getName() {
        return "ywzj_vehicle:parachute_pack";
    }

    @Override
    public float getToughness() {
        return 0;
    }

    @Override
    public float getKnockbackResistance() {
        return 0;
    }

}
