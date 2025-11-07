package org.ywzj.vehicle.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.all.AllBlockEntities;

public class FigureBoxBlockEntity extends BlockEntity {

    private Entity entity;
    private String entityId;
    private CompoundTag entityData;

    public FigureBoxBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.FIGURE_BOX_BLOCK_ENTITY.get(), pos, state);
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void updateEntity() {
        if (level != null && entityId != null && entityData != null) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entityId));
            if (type != null) {
                Entity entity = type.create(level);
                entity.load(entityData);
                this.entity = entity;
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (entity != null) {
            CompoundTag entityData = new CompoundTag();
            entity.saveWithoutId(entityData);
            tag.putString("entityId", EntityType.getKey(entity.getType()).toString());
            tag.put("entityData", entityData);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("entityId") && tag.contains("entityData")) {
            entityId = tag.getString("entityId");
            entityData = tag.getCompound("entityData");
        }
        updateEntity();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateEntity();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        handleUpdateTag(packet.getTag());
    }

}
