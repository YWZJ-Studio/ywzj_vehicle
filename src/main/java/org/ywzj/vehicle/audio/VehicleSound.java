package org.ywzj.vehicle.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VehicleSound extends SimpleSoundInstance implements TickableSoundInstance {

    private final Integer fadeTicks;
    private final Integer entityId;
    private Entity entity;
    private final Vec3 offset;
    private final double scale;
    private boolean isPlaying;
    private boolean isFadeOut;
    private boolean fadeIn;
    private boolean fadeOut;
    private Integer fadeInTick = 0;

    public VehicleSound(SoundEvent event, Vec3 offset, float volume, float distance, float pitch, boolean loop, int fadeTicks, boolean fadeIn, boolean fadeOut, int entityId) {
        super(event, SoundSource.PLAYERS, volume, pitch, SoundInstance.createUnseededRandom(), 0, 0, 0);
        this.entityId = entityId;
        this.offset = offset;
        updateRelativePos();
        this.volume = fadeIn ? 0.0001f : volume;
        this.fadeTicks = fadeTicks;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.scale = 1 / distance;
        this.looping = loop;
        this.isPlaying = true;
        this.isFadeOut = false;
    }

    public VehicleSound(SoundEvent event, float volume, float distance, float pitch, boolean loop, int fadeTicks, boolean fadeIn, boolean fadeOut, int entityId) {
        this(event, Vec3.ZERO, volume, distance, pitch, loop, fadeTicks, fadeIn, fadeOut, entityId);
    }

    public void play() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().getSoundManager().play(this));
    }

    public void stop() {
        if (!looping || !fadeOut) {
            isPlaying = false;
        }
        isFadeOut = true;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    @Override
    public boolean isStopped() {
        return !isPlaying;
    }

    @Override
    public void tick() {
        updateRelativePos();
        if (isFadeOut) {
            volume = volume * (float) Math.pow(0.001f, 1 / (double) fadeTicks);
            if (volume <= 0.001f) {
                isPlaying = false;
            }
        } else if (fadeIn) {
            volume = (float) Math.max(0.0001f, Math.log(fadeInTick + 1) / Math.log(fadeTicks + 1) * 1f);
            fadeInTick += 1;
            if (volume == 1f) {
                fadeIn = false;
            }
        }
    }

    private void updateRelativePos() {
        Entity cameraEntity = Minecraft.getInstance().cameraEntity;
        if (cameraEntity == null) {
            return;
        }
        if (entity == null) {
            entity = cameraEntity.level().getEntity(entityId);
            return;
        }
        if (entity.isRemoved()) {
            stop();
            return;
        }
        Vec3 simulatedPos;
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        simulatedPos = calRelativePos(entity.position().add(offset), cameraPos, entity.equals(cameraEntity.getVehicle()));
        this.x = simulatedPos.x;
        this.y = simulatedPos.y;
        this.z = simulatedPos.z;
    }

    public Vec3 calRelativePos(Vec3 soundPos, Vec3 targetPos, boolean cameraEntityOnVehicle) {
        if (cameraEntityOnVehicle) {
            soundPos = targetPos.add(soundPos.subtract(targetPos).normalize().scale(1 / scale * 8));
        }
        return targetPos.add(soundPos.subtract(targetPos).scale(scale));
    }

}
