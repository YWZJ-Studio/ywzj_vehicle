package org.ywzj.vehicle.vehicle.parenting;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

/**
 * Fills in surface properties and footstep sounds that hull air does not provide.
 * Motion is measured in the vehicle frame; a rider standing still on a moving
 * ship has not taken a step. Step detection runs with clip; emission runs on tick
 * thread only.
 */
public final class DeckEffects {

    /** Step scaling: vanilla fires every 1.67 blocks. */
    private static final double STEP_SCALE = 0.6;
    private static final double STEP_INTERVAL = 1.0;

    /** Vanilla's fall sound distance from causeFallDamage. */
    private static final float FALL_SOUND_DISTANCE = 3.0F;

    private DeckEffects() {}

    /**
     * Records deck-relative travel in the vehicle frame, called from the clip.
     */
    static void walked(Entity entity, DeckAttachment attachment, double localDx, double localDz) {
        if (localDx != 0 || localDz != 0) {
            attachment.addWalk(Math.sqrt(localDx * localDx + localDz * localDz) * STEP_SCALE);
        }
        // Still the pre-landing value: vanilla clears fallDistance in checkFallDamage, which runs
        // after collide has returned, and this is inside collide.
        if (entity.fallDistance > FALL_SOUND_DISTANCE) {
            attachment.queueFallSound();
        }
    }

    /**
     * Plays sounds through entity.playSound, respecting client/server split;
     * Player broadcasts except to self, mobs broadcast to all.
     */
    static void emit(Entity entity, AbstractVehicle vehicle, DeckAttachment attachment) {
        // Approximates vanilla's MovementEmission gate, which is protected. Items, boats and
        // projectiles do not have feet; everything that walks is a LivingEntity.
        if (!(entity instanceof LivingEntity) || entity.isSilent()) {
            attachment.clearPendingSounds();
            return;
        }
        SoundType surface = vehicle.deckSoundType();
        if (attachment.takeFallSound()) {
            entity.playSound(surface.getFallSound(),
                    surface.getVolume() * 0.5F, surface.getPitch() * 0.75F);
        }
        if (attachment.takeStep(STEP_INTERVAL)) {
            entity.playSound(surface.getStepSound(), surface.getVolume() * 0.15F, surface.getPitch());
            // No-op on the client, so this is the server's copy only; vanilla emits it there too.
            // Restores the vibration a hull was swallowing.
            entity.gameEvent(GameEvent.STEP);
        }
    }

}
