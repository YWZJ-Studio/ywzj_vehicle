package org.ywzj.vehicle.vehicle.collision;

import org.jetbrains.annotations.Nullable;

/**
 * Where a collision query resolves its section snapshots from, live or frozen for one
 * vehicle's tick. A missing section reads as empty.
 */
public interface SectionSource {

    @Nullable
    SectionCollision section(long key);

}
