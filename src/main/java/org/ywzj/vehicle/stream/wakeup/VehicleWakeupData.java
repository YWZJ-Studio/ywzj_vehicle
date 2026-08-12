package org.ywzj.vehicle.stream.wakeup;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VehicleWakeupData extends SavedData {

    public static final String FILE_ID = "ywzj_vehicle_wakeup";

    private final Map<UUID, Entry> entries = new HashMap<>();

    public record Entry(UUID vehicleId, ResourceKey<Level> dimension, Vec3 position, long gameTime) {

        public ChunkPos chunk() {
            return new ChunkPos((int) Math.floor(position.x) >> 4, (int) Math.floor(position.z) >> 4);
        }

    }

    public static SavedData.Factory<VehicleWakeupData> factory() {
        return new SavedData.Factory<>(VehicleWakeupData::new, VehicleWakeupData::load, null);
    }

    public static VehicleWakeupData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    private static VehicleWakeupData load(CompoundTag tag, HolderLookup.Provider registries) {
        VehicleWakeupData data = new VehicleWakeupData();
        ListTag list = tag.getList("Vehicles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation dimensionId = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimensionId == null || !entry.hasUUID("Id")) {
                continue;
            }
            UUID id = entry.getUUID("Id");
            data.entries.put(id, new Entry(id,
                    ResourceKey.create(Registries.DIMENSION, dimensionId),
                    new Vec3(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")),
                    entry.getLong("GameTime")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : this.entries.values()) {
            CompoundTag element = new CompoundTag();
            element.putUUID("Id", entry.vehicleId());
            element.putString("Dimension", entry.dimension().location().toString());
            element.putDouble("X", entry.position().x);
            element.putDouble("Y", entry.position().y);
            element.putDouble("Z", entry.position().z);
            element.putLong("GameTime", entry.gameTime());
            list.add(element);
        }
        tag.put("Vehicles", list);
        return tag;
    }

    public void put(UUID vehicleId, ResourceKey<Level> dimension, Vec3 position, long gameTime) {
        Entry previous = this.entries.get(vehicleId);
        if (previous != null
                && previous.dimension() == dimension
                && previous.position().distanceToSqr(position) < 1.0E-4) {
            return;
        }
        this.entries.put(vehicleId, new Entry(vehicleId, dimension, position, gameTime));
        setDirty();
    }

    @Nullable
    public Entry get(UUID vehicleId) {
        return this.entries.get(vehicleId);
    }

    public void remove(UUID vehicleId) {
        if (this.entries.remove(vehicleId) != null) {
            setDirty();
        }
    }

    public Collection<Entry> all() {
        return this.entries.values();
    }

}
