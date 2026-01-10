package org.ywzj.vehicle.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.render.item.VehicleSpawnItemRenderer;
import org.ywzj.vehicle.custom.CommonAssetsManager;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import java.util.function.Consumer;

public class VehicleSpawnItem extends Item {

    public VehicleSpawnItem(Properties pProperties) {
        super(pProperties);
    }

    public ItemStack createInstance(ResourceLocation customId) {
        ItemStack itemStack = new ItemStack(this);
        CompoundTag tag = itemStack.getOrCreateTag();
        tag.putString("customId", customId.toString());
        itemStack.setHoverName(Component.translatable(customId.getNamespace() + "." + customId.getPath()));
        itemStack.setTag(tag);
        return itemStack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new VehicleSpawnItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
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
        CompoundTag tag = itemStack.getTag();
        ResourceLocation customId = YwzjVehicle.resourceLocation(tag.getString("customId"));
        if (ForgeRegistries.ENTITY_TYPES.containsKey(customId)) {
            EntityType<?> vehicleType = ForgeRegistries.ENTITY_TYPES.getValue(customId);
            Entity vehicle = vehicleType.create(level);
            if (vehicle == null) {
                return InteractionResult.PASS;
            }
            vehicle.setYRot(player.getYRot());
            vehicle.setPos(position);
            level.addFreshEntity(vehicle);
        } else {
            CommonAssetsManager.vehicleDataManager().getVehicleData(customId).ifPresent(data -> {
                AbstractVehicle vehicle = data.summon(customId, level, position, 0, player.getYRot());
                vehicle.setYRot(player.getYRot());
            });
        }
        itemStack.shrink(1);
        return InteractionResult.SUCCESS;
    }

}
