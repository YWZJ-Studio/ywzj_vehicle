package org.ywzj.vehicle.mixin.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.ywzj.vehicle.stream.ChunkStreamDebug;
import org.ywzj.vehicle.stream.PinnedChunkCache;
import org.ywzj.vehicle.stream.client.SodiumChunkProbe;

import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin implements PinnedChunkCache {

    @Unique
    private final Long2ObjectLinkedOpenHashMap<LevelChunk> ywzj$pinnedChunks = new Long2ObjectLinkedOpenHashMap<>();

    @Unique
    private int ywzj$centreX;

    @Unique
    private int ywzj$centreZ;

    @Unique
    private int ywzj$radius = -1;

    @Shadow
    @Final
    ClientLevel level;

    @Shadow
    @Final
    private LevelChunk emptyChunk;

    @Override
    public int ywzj$pinnedCount() {
        return this.ywzj$pinnedChunks.size();
    }

    @Override
    public LongSet ywzj$pinnedPositions() {
        return new LongOpenHashSet(this.ywzj$pinnedChunks.keySet());
    }

    @Override
    public ChunkPos ywzj$viewCentre() {
        return new ChunkPos(this.ywzj$centreX, this.ywzj$centreZ);
    }

    @Override
    public int ywzj$storageRadius() {
        return this.ywzj$radius;
    }

    /**
     * Dropping a pinned copy has to run the same unload path vanilla uses, because renderers track
     * chunk presence by observing ClientLevel#unload; discarding it silently leaves them convinced
     * the chunk is still there and they never rebuild it when it comes back. ClientLevel#unload keys
     * off the chunk position though, so it must be skipped once the vanilla array owns that position
     * again. Otherwise, dropping a stale copy unloads the live chunk sharing its coordinates.
     */
    @Unique
    private void ywzj$release(long key, LevelChunk released, String reason) {
        int x = ChunkPos.getX(key);
        int z = ChunkPos.getZ(key);
        LevelChunk live = ((ClientChunkCache) (Object) this).getChunk(x, z, ChunkStatus.FULL, false);
        if (live == null) {
            NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(released));
            this.level.unload(released);
        }
        ChunkStreamDebug.log(ChunkStreamDebug.Category.CACHE, "rescue drop {} {} ({}, {} held)",
                x, z, live == null ? reason : "superseded by the live copy", this.ywzj$pinnedChunks.size());
    }

    @Unique
    private void ywzj$trim() {
        while (this.ywzj$pinnedChunks.size() > MAX_PINNED_CHUNKS) {
            long oldest = this.ywzj$pinnedChunks.firstLongKey();
            LevelChunk evicted = this.ywzj$pinnedChunks.remove(oldest);
            if (evicted != null) {
                ywzj$release(oldest, evicted, "rescue cache full");
            }
        }
    }

    @Inject(method = "updateViewCenter(II)V", at = @At("HEAD"))
    private void ywzj$trackCentre(int x, int z, CallbackInfo ci) {
        if (x != this.ywzj$centreX || z != this.ywzj$centreZ) {
            ChunkStreamDebug.log(ChunkStreamDebug.Category.CACHE, "view centre {} {} -> {} {}",
                    this.ywzj$centreX, this.ywzj$centreZ, x, z);
            this.ywzj$centreX = x;
            this.ywzj$centreZ = z;
        }
    }

    /**
     * Rebuilding the storage array keeps only chunks that are in range of the new radius
     * around the unchanged centre and silently discardsw the rest without unloading them, so
     * renderers are never told those chunks went away.
     */
    @Inject(method = "updateViewRadius(I)V", at = @At("HEAD"))
    private void ywzj$trackRadius(int viewDistance, CallbackInfo ci) {
        int updated = Math.max(2, viewDistance) + 3;
        if (updated != this.ywzj$radius) {
            ChunkStreamDebug.log(ChunkStreamDebug.Category.CACHE,
                    "storage radius {} -> {} (view distance {}), rebuilding around centre {} {}, {} chunk(s) held",
                    this.ywzj$radius, updated, viewDistance, this.ywzj$centreX, this.ywzj$centreZ,
                    ((ClientChunkCache) (Object) this).getLoadedChunksCount());
            this.ywzj$radius = updated;
        }
    }

    @Inject(method = "drop(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("HEAD"))
    private void ywzj$trackDrop(ChunkPos pos, CallbackInfo ci) {
        ChunkStreamDebug.log(ChunkStreamDebug.Category.CACHE, "forget packet for {} {}", pos.x, pos.z);
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("RETURN"), cancellable = true)
    private void ywzj$pinnedGetChunk(int x, int z, ChunkStatus status, boolean load,
                                     CallbackInfoReturnable<LevelChunk> cir) {
        if (this.ywzj$pinnedChunks.isEmpty()) {
            return;
        }
        long key = ChunkPos.asLong(x, z);
        LevelChunk current = cir.getReturnValue();
        if (current != null && current != this.emptyChunk) {
            LevelChunk stale = this.ywzj$pinnedChunks.remove(key);
            if (stale != null && stale != current) {
                ywzj$release(key, stale, "superseded");
            }
            return;
        }
        LevelChunk pinned = this.ywzj$pinnedChunks.get(key);
        if (pinned != null) {
            cir.setReturnValue(pinned);
        }
    }

    /**
     * Vanilla drops any chunk that lands outside the ring buffer's window around the cache centre and
     * never asks for it again, so the server's copy is simply lost. Detached streaming parks that
     * centre on the vehicle, which puts every chunk sent around the operator's body outside the
     * window.
     */
    @Inject(method = "replaceWithPacketData(IILnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/nbt/CompoundTag;Ljava/util/function/Consumer;)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("RETURN"), cancellable = true)
    private void ywzj$pinnedReplace(int x, int z, FriendlyByteBuf buffer, CompoundTag tag,
                                    Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
                                    CallbackInfoReturnable<LevelChunk> cir) {
        if (cir.getReturnValue() != null) {
            ChunkStreamDebug.log(ChunkStreamDebug.Category.CACHE,
                    "accepted {} {} into slot (centre {} {}, radius {})",
                    x, z, this.ywzj$centreX, this.ywzj$centreZ, this.ywzj$radius);
            return;
        }
        ChunkPos pos = new ChunkPos(x, z);
        LevelChunk chunk = new LevelChunk(this.level, pos);
        chunk.replaceWithPacketData(buffer, tag, consumer);
        this.ywzj$pinnedChunks.put(ChunkPos.asLong(x, z), chunk);
        this.level.onChunkLoaded(pos);
        NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false));
        SodiumChunkProbe.markLoaded(this.level, x, z);
        cir.setReturnValue(chunk);
        ywzj$trim();
        ChunkStreamDebug.log(ChunkStreamDebug.Category.CACHE,
                "rescued {} {} out of window (centre {} {}, radius {}, distance {}, {} held)",
                x, z, this.ywzj$centreX, this.ywzj$centreZ, this.ywzj$radius,
                Math.max(Math.abs(x - this.ywzj$centreX), Math.abs(z - this.ywzj$centreZ)),
                this.ywzj$pinnedChunks.size());
    }

}
