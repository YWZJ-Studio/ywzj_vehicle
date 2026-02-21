package org.ywzj.vehicle.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.all.AllBlockEntities;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

import static org.ywzj.vehicle.item.FigureBoxItem.ENTITY_DATA;
import static org.ywzj.vehicle.item.FigureBoxItem.ENTITY_ID;

public class FigureBoxBlockEntity extends BlockEntity {

    private Entity entity;
    private String entityId;
    private CompoundTag entityData;
    public boolean open;
    public float scale = 1f;
    public float xShift;
    public float yShift;
    public float zShift;
    public float xRot;
    public float yRot;

    public FigureBoxBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.FIGURE_BOX_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB baseBox = super.getRenderBoundingBox();
        return baseBox.inflate(scale + Math.abs(xShift),
                scale + Math.abs(yShift),
                scale + Math.abs(zShift));
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
        if (level != null && entityId != null && entityData != null) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(YwzjVehicle.resourceLocation(entityId));
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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (entity != null) {
            CompoundTag entityData = new CompoundTag();
            entity.saveWithoutId(entityData);
            tag.putString(ENTITY_ID, EntityType.getKey(entity.getType()).toString());
            tag.put(ENTITY_DATA, entityData);
        }
        tag.putBoolean("open", open);
        tag.putFloat("scale", scale);
        tag.putFloat("xShift", xShift);
        tag.putFloat("yShift", yShift);
        tag.putFloat("zShift", zShift);
        tag.putFloat("xRot", xRot);
        tag.putFloat("yRot", yRot);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ENTITY_ID) && tag.contains(ENTITY_DATA)) {
            entityId = tag.getString(ENTITY_ID);
            entityData = tag.getCompound(ENTITY_DATA);
        }
        open = tag.getBoolean("open");
        scale = tag.getFloat("scale");
        xShift = tag.getFloat("xShift");
        yShift = tag.getFloat("yShift");
        zShift = tag.getFloat("zShift");
        xRot = tag.getFloat("xRot");
        yRot = tag.getFloat("yRot");
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
