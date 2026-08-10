package org.ywzj.vehicle.item;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.GeoArmorRendererV2;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.resource.BedrockModelResources;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllEntities;
import org.ywzj.vehicle.all.AllItems;
import org.ywzj.vehicle.all.AllSounds;
import org.ywzj.vehicle.entity.misc.ParagliderCanopy;
import org.ywzj.vehicle.resource.ParachuteModels;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber
public class ParachutePackItem extends ArmorItem {

    private static final Holder<ArmorMaterial> MATERIAL = Holder.direct(new ArmorMaterial(
            Map.of(),
            0,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(ItemTags.WOOL),
            List.of(new ArmorMaterial.Layer(YwzjVehicle.modLocation("parachute_pack"))),
            0,
            0
    ));
    private static final String OPEN_TAG = "ParachutePackOpen";
    private static final String CANOPY_ID_TAG = "ParachutePackCanopyId";
    private static final double MIN_OPEN_DESCENT_SPEED = 0.5;
    private static final double OPEN_VERTICAL_SPEED = -0.05;
    private static final double GLIDE_RATIO = 1.5;
    private static final double MAX_HORIZONTAL_SPEED = 0.6;
    private static final double INERTIA = 0.1;

    public ParachutePackItem(Type type, Properties properties) {
        super(MATERIAL, type, properties);
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
        CompoundTag tag = getCustomData(stack);
        tag.putBoolean(OPEN_TAG, true);
        stack.setDamageValue(1);
        player.startFallFlying();
        player.fallDistance = 0;
        ParagliderCanopy canopy = new ParagliderCanopy(AllEntities.PARAGLIDER_CANOPY.get(), player.level());
        canopy.equip(player);
        tag.putInt(CANOPY_ID_TAG, canopy.getId());
        setCustomData(stack, tag);
        player.level().playSound(null, player, AllSounds.PARACHUTE_OPEN.get(), SoundSource.PLAYERS, 3F, 1F);
    }

    public static boolean isOpen(ItemStack stack) {
        return getCustomData(stack).getBoolean(OPEN_TAG);
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
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
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
        CompoundTag tag = getCustomData(stack);
        Entity canopy = player.level().getEntity(tag.getInt(CANOPY_ID_TAG));
        if (canopy instanceof ParagliderCanopy paragliderCanopy) {
            paragliderCanopy.fallThenDiscard();
        }
        tag.remove(OPEN_TAG);
        tag.remove(CANOPY_ID_TAG);
        setCustomData(stack, tag);
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
        CompoundTag tag = getCustomData(repaired);
        tag.remove(OPEN_TAG);
        tag.remove(CANOPY_ID_TAG);
        setCustomData(repaired, tag);
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
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                                      ArmorMaterial.Layer layer, boolean innerModel) {
        return ParachuteModels.PARACHUTE_PACK_TEXTURE;
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void setCustomData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

}
