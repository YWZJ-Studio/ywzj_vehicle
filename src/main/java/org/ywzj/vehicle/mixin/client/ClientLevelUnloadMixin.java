package org.ywzj.vehicle.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.ywzj.vehicle.stream.ChunkStreamDebug;

/**
 * Every sodium-family renderer removes a chunk from its render graph here, keyed by position, no
 * matter who called it, including vanilla's own silent ring-buffer eviction in
 * {@code ClientChunkCache$Storage#replace}. This is the only place that removal can be observed, so
 * the caller is recorded alongside it.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelUnloadMixin {

    @Unique
    private static String ywzj$caller() {
        return StackWalker.getInstance().walk(frames -> frames
                .skip(2)
                .filter(frame -> !frame.getClassName().equals(ClientLevel.class.getName()))
                .findFirst()
                .map(frame -> frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1)
                        + "." + frame.getMethodName())
                .orElse("?"));
    }

    @Inject(method = "unload(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("HEAD"))
    private void ywzj$trackUnload(LevelChunk chunk, CallbackInfo ci) {
        if (!ChunkStreamDebug.on(ChunkStreamDebug.Category.CACHE)) {
            return;
        }
        ChunkPos pos = chunk.getPos();
        ChunkStreamDebug.log(ChunkStreamDebug.Category.CACHE, "unload {} {} from {}",
                pos.x, pos.z, ywzj$caller());
    }

    @Inject(method = "onChunkLoaded(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("HEAD"))
    private void ywzj$trackLoad(ChunkPos pos, CallbackInfo ci) {
        if (!ChunkStreamDebug.on(ChunkStreamDebug.Category.CACHE)) {
            return;
        }
        ChunkStreamDebug.log(ChunkStreamDebug.Category.CACHE, "load {} {} from {}",
                pos.x, pos.z, ywzj$caller());
    }

}
