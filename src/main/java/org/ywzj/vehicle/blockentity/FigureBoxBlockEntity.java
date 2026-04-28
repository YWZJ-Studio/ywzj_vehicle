package org.ywzj.vehicle.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import static org.ywzj.vehicle.item.FigureBoxItem.ENTITY_DATA;
import static org.ywzj.vehicle.item.FigureBoxItem.ENTITY_TYPE;

public class FigureBoxBlockEntity extends BlockEntity {

    private Entity entity;
    private String entityType;
    private CompoundTag entityData;
    public boolean open;
    public float scale = 1f;
    public float xShift;
    public float yShift;
    public float zShift;
    public float xRot;
    public float yRot;
    public float zRot;

    public FigureBoxBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.FIGURE_BOX_BLOCK_ENTITY.get(), pos, state);
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
        if (level != null) {
            if (level.isClientSide()) {
                if (entity instanceof AbstractVehicle vehicle) {
                    vehicle.initDisplayData();
                }
            } else {
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    private void updateEntity() {
        if (level != null && entityType != null && entityData != null) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(YwzjVehicle.resourceLocation(entityType));
            if (type != null) {
                Entity entity = type.create(level);
                entity.load(entityData);
                setEntity(entity);
                if (entity instanceof AbstractVehicle vehicle) {
                    vehicle.initData();
                }
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (entity != null) {
            CompoundTag entityData = new CompoundTag();
            entity.saveWithoutId(entityData);
            tag.putString(ENTITY_TYPE, EntityType.getKey(entity.getType()).toString());
            tag.put(ENTITY_DATA, entityData);
        }
        tag.putBoolean("open", open);
        tag.putFloat("scale", scale);
        tag.putFloat("xShift", xShift);
        tag.putFloat("yShift", yShift);
        tag.putFloat("zShift", zShift);
        tag.putFloat("xRot", xRot);
        tag.putFloat("yRot", yRot);
        tag.putFloat("zRot", zRot);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(ENTITY_TYPE) && tag.contains(ENTITY_DATA)) {
            entityType = tag.getString(ENTITY_TYPE);
            entityData = tag.getCompound(ENTITY_DATA);
        }
        open = tag.getBoolean("open");
        scale = tag.getFloat("scale");
        xShift = tag.getFloat("xShift");
        yShift = tag.getFloat("yShift");
        zShift = tag.getFloat("zShift");
        xRot = tag.getFloat("xRot");
        yRot = tag.getFloat("yRot");
        zRot = tag.getFloat("zRot");
        updateEntity();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateEntity();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

}
