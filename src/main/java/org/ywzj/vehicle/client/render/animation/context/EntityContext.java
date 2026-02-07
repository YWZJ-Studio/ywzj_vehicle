package org.ywzj.vehicle.client.render.animation.context;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.Keyframe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

import java.util.Map;

public class EntityContext<T extends Entity> extends BaseAnimationContext {

    private final T entity;

    public EntityContext(T entity, Map<String, BedrockAnimation> animations) {
        super(animations);
        this.entity = entity;
        this.getAnimationRunnerHolder().setSoundProcessor(this::processSounds);
    }

    public void processSounds(Iterable<Keyframe<ResourceLocation>> sounds) {
        for (Keyframe<ResourceLocation> keyframe : sounds) {
            ResourceLocation soundLocation = keyframe.getValue();
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundLocation);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), soundEvent, entity.getSoundSource(), 1.0F, 1.0F);
        }
    }

    public T getEntity() {
        return entity;
    }

}
