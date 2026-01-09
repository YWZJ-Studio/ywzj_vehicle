package org.ywzj.vehicle.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.YwzjVehicle;

public class VehicleSpawnItem extends Item {

    public VehicleSpawnItem(Properties pProperties) {
        super(pProperties);
    }

    public ItemStack getInstance(String vehicleEntityId) {
        ItemStack itemStack = new ItemStack(this);
        CompoundTag tag = itemStack.getOrCreateTag();
        tag.putString("entityId", vehicleEntityId);
        EntityType<?> vehicleType = ForgeRegistries.ENTITY_TYPES.getValue(YwzjVehicle.resourceLocation(vehicleEntityId));
        if (vehicleType != null) {
            itemStack.setHoverName(vehicleType.getDescription());
        }
        itemStack.setTag(tag);
        return itemStack;
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
        BlockPos pos = context.getClickedPos().above();
        ItemStack itemStack = player.getItemInHand(context.getHand());
        CompoundTag tag = itemStack.getTag();
        String entityId = tag.getString("entityId");
        EntityType<?> vehicleType = ForgeRegistries.ENTITY_TYPES.getValue(YwzjVehicle.resourceLocation(entityId));
        if (vehicleType != null) {
            Entity vehicle = vehicleType.create(level);
            if (vehicle == null) {
                return InteractionResult.PASS;
            }
            vehicle.setPos(pos.getX(), pos.getY(), pos.getZ());
            vehicle.setYRot(player.getYRot());
            level.addFreshEntity(vehicle);
            itemStack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

}
