package org.ywzj.vehicle.stream.client;

import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.ywzj.vehicle.stream.ChunkStreamDebug;

import java.lang.reflect.Method;

/**
 * Sodium keeps its own view of which chunks exist and builds the render graph from that, not from
 * {@link net.minecraft.client.multiplayer.ClientChunkCache}. Removal is hooked on
 * {@code ClientLevel#unload} globally, but addition is hooked on the {@code ClientLevel#onChunkLoaded}
 * call site <em>inside</em> {@code ClientChunkCache#replaceWithPacketData}; a chunk stored by any
 * other path is therefore invisible to it forever while still being unloadable. This reaches the
 * tracker directly so chunks kept outside vanilla's ring buffer can be announced and audited.
 */
@OnlyIn(Dist.CLIENT)
public final class SodiumChunkProbe {

    private static final String[] HOLDERS = {
            "net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder",
            "me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder",
            "org.embeddedt.embeddium.client.render.chunk.map.ChunkTrackerHolder",
            "org.embeddedt.embeddium.render.chunk.map.ChunkTrackerHolder"
    };

    private static final int FLAG_HAS_BLOCK_DATA = 1;

    private static boolean resolved;
    private static Method holderGet;
    private static Method readyChunks;
    private static Method statusAdded;
    private static String flavour = "absent";

    private SodiumChunkProbe() {
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        for (String name : HOLDERS) {
            try {
                Class<?> holder = Class.forName(name, false, SodiumChunkProbe.class.getClassLoader());
                Method get = holder.getMethod("get", ClientLevel.class);
                Class<?> tracker = get.getReturnType();
                readyChunks = tracker.getMethod("getReadyChunks");
                statusAdded = tracker.getMethod("onChunkStatusAdded", int.class, int.class, int.class);
                holderGet = get;
                flavour = name;
                break;
            } catch (Throwable ignored) {
                holderGet = null;
                readyChunks = null;
                statusAdded = null;
            }
        }
        ChunkStreamDebug.log(ChunkStreamDebug.Category.TRACKER, "sodium-family chunk tracker: {}", flavour);
    }

    public static boolean available() {
        resolve();
        return holderGet != null;
    }

    public static String flavour() {
        resolve();
        return flavour;
    }

    private static Object tracker(ClientLevel level) {
        resolve();
        if (holderGet == null || level == null) {
            return null;
        }
        try {
            return holderGet.invoke(null, level);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * @return the chunks the renderer currently considers complete, or null when no sodium-family
     * renderer is installed. A chunk absent here is never drawn regardless of what the chunk cache
     * holds.
     */
    public static LongOpenHashSet readySet(ClientLevel level) {
        Object tracker = tracker(level);
        if (tracker == null) {
            return null;
        }
        try {
            Object ready = readyChunks.invoke(tracker);
            return ready instanceof LongCollection collection ? new LongOpenHashSet(collection) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void markLoaded(ClientLevel level, int x, int z) {
        Object tracker = tracker(level);
        if (tracker == null) {
            return;
        }
        try {
            statusAdded.invoke(tracker, x, z, FLAG_HAS_BLOCK_DATA);
            ChunkStreamDebug.log(ChunkStreamDebug.Category.TRACKER,
                    "announced chunk {} {} to the renderer", x, z);
        } catch (Throwable t) {
            ChunkStreamDebug.warn(ChunkStreamDebug.Category.TRACKER,
                    "could not announce chunk {} {}: {}", x, z, t);
        }
    }

    public static boolean isReady(LongOpenHashSet ready, int x, int z) {
        return ready != null && ready.contains(ChunkPos.asLong(x, z));
    }

}
