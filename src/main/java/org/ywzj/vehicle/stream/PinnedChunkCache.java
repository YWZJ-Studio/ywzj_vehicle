package org.ywzj.vehicle.stream;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.ChunkPos;

public interface PinnedChunkCache {

    int MAX_PINNED_CHUNKS = 512;

    int ywzj$pinnedCount();

    LongSet ywzj$pinnedPositions();

    /**
     * The last center the server told the client to index its ring buffer around; every chunk more
     * than the storage radius away from it is rejected outright by vanilla.
     */
    ChunkPos ywzj$viewCentre();

    int ywzj$storageRadius();

}
