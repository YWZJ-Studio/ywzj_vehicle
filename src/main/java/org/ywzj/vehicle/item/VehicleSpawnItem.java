package org.ywzj.vehicle.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.render.item.VehicleSpawnItemRenderer;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;
import org.ywzj.vehicle.util.VectorUtil;

import java.util.Optional;
import java.util.function.Consumer;

import static org.ywzj.vehicle.api.entity.ICustomVehicle.TAG_VEHICLE_ID;

public class VehicleSpawnItem extends Item {

    public VehicleSpawnItem(Properties pProperties) {
        super(pProperties);
    }

    public ItemStack createInstance(ResourceLocation vehicleId) {
        ItemStack itemStack = new ItemStack(this);
        CompoundTag tag = itemStack.getOrCreateTag();
        tag.putString(TAG_VEHICLE_ID, vehicleId.toString());
        itemStack.setHoverName(Component.translatable(vehicleId.getNamespace() + "." + vehicleId.getPath()));
        itemStack.setTag(tag);
        return itemStack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private VehicleSpawnItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new VehicleSpawnItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }

        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(itemStack, true);
        }
        HitResult blockHitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (blockHitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemStack);
        }
        EntityHitResult entityHitResult = VectorUtil.hitEntity(player, player.getEyePosition(), blockHitResult.getLocation());
        if (entityHitResult != null) {
            return InteractionResultHolder.pass(itemStack);
        }
        CompoundTag tag = itemStack.getTag();
        if (tag == null || !tag.contains(TAG_VEHICLE_ID)) {
            return InteractionResultHolder.fail(itemStack);
        }
        ResourceLocation vehicleId = YwzjVehicle.resourceLocation(tag.getString(TAG_VEHICLE_ID));
        Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId);
        if (vehicleDataOptional.isPresent()) {
            Entity vehicle = vehicleDataOptional.get().construct(level, blockHitResult.getLocation(), 0, player.getYRot());
            if (level.addFreshEntity(vehicle)) {
                itemStack.shrink(1);
                return InteractionResultHolder.sidedSuccess(itemStack, false);
            }
        }
        return InteractionResultHolder.fail(itemStack);
    }

}
