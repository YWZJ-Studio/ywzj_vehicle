package org.ywzj.vehicle.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.render.item.VehicleSpawnItemRenderer;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.custom.vehicle.BaseVehicleData;

import java.util.Optional;
import java.util.function.Consumer;

import static org.ywzj.vehicle.api.entity.ICustomVehicle.TAG_VEHICLE_ID;

public class VehicleSpawnItem extends Item {

    public VehicleSpawnItem(Properties pProperties) {
        super(pProperties);
    }

    public ItemStack createInstance(ResourceLocation vehicleId) {
        ItemStack itemStack = new ItemStack(this);
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TAG_VEHICLE_ID, vehicleId.toString());
        itemStack.set(DataComponents.ITEM_NAME, Component.translatable(vehicleId.getNamespace() + "." + vehicleId.getPath()));
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return itemStack;
    }

    @Override
    @SuppressWarnings("removal")
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
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos blockPos = context.getClickedPos().above();
        Vec3 position = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        ItemStack itemStack = player.getItemInHand(context.getHand());
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ResourceLocation vehicleId = YwzjVehicle.resourceLocation(tag.getString(TAG_VEHICLE_ID));
        Optional<BaseVehicleData> vehicleDataOptional = CommonAssetsManager.vehicleDataManager().getVehicleData(vehicleId);
        if (vehicleDataOptional.isPresent()) {
            Entity vehicle = vehicleDataOptional.get().construct(level, position, 0, player.getYRot());
            level.addFreshEntity(vehicle);
            itemStack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

}
