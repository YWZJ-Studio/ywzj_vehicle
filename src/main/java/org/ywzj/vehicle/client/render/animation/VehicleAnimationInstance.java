package org.ywzj.vehicle.client.render.animation;

import com.maydaymemory.mae.basic.DummyPose;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.api.animation.IAnimationInstance;
import org.ywzj.vehicle.client.render.animation.context.BaseAnimationContext;
import org.ywzj.vehicle.client.render.animation.context.EntityContext;
import org.ywzj.vehicle.client.render.animation.controller.AnimationController;

import java.util.Map;

public class VehicleAnimationInstance<T extends BaseAnimationContext> implements IAnimationInstance<T> {

    private final Map<String, AnimationStateMachine<T>> stateMachines;
    private final AnimationController<T> controller;
    private final T context;
    private boolean isError = false;

    public VehicleAnimationInstance(AnimationController<T> controller, @NotNull T context) {
        this.controller = controller;
        this.context = context;
        this.stateMachines = controller.initialize(context);
    }

    public AnimationStateMachine<T> getStateMachine(String name) {
        return stateMachines.get(name);
    }

    @NotNull
    public T getContext() {
        return context;
    }

    public void tick() {
        for (var value : stateMachines.values()) {
            value.tick();
        }
        if (context != null) {
            context.tick();
            // 此帧结束，清除事件
            context.clearEvents();
        }
    }

    @NotNull
    public Pose getCurrentPose() {
        if (isError) {
            return DummyPose.INSTANCE;
        }
        try {
            Pose pose = controller.getPoseGraph().evaluate(this);
            return pose != null ? pose : DummyPose.INSTANCE;
        } catch (Exception e) {
            isError = true;
            YwzjVehicle.LOGGER.error("Error evaluating animation pose", e);
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                var msg = Component.literal("Error evaluating animation pose, check logs for detail")
                        .withStyle(ChatFormatting.RED)
                        .withStyle((s)->s.withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(e.getMessage()))
                        ));
                if (this.getContext() instanceof EntityContext<?> entityContext && entityContext.getEntity() != null) {
                    msg.append(" (").append(entityContext.getEntity().toString()).append(")");
                }
                mc.player.sendSystemMessage(msg);
            }
            return DummyPose.INSTANCE;
        }
    }

}
