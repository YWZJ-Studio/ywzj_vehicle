package org.ywzj.vehicle.client.resource.animation;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import org.ywzj.vehicle.client.render.animation.util.AnimationPlayType;

// 状态机动作
public class ActionDefinition {
    /**
     * Action type: "play_animation", "stop_animation", "set_variable", "play_sound", "script"
     */
    @SerializedName("type")
    private String type;

    // 动画相关参数
    @SerializedName("animation")
    private String animation;

    @SerializedName("play_type")
    private AnimationPlayType playType = AnimationPlayType.PLAY_ONCE_STOP;

    @SerializedName("track")
    private String track = "main";

    @SerializedName("speed")
    private Object speed;


    // script action field
    @SerializedName("script")
    private String script;

    // set_variable fields
    @SerializedName("name")
    private String name;

    @SerializedName("value")
    private Object value;

    // play_sound fields
    @SerializedName("sound")
    private ResourceLocation sound;

    @SerializedName("volume")
    private Float volume;

    @SerializedName("pitch")
    private Float pitch;

    public String getType() {
        return type;
    }

    public String getAnimation() {
        return animation;
    }

    public String getTrack() {
        return track;
    }

    public Object getSpeed() {
        return speed;
    }

    public AnimationPlayType getPlayType() {
        return playType;
    }

    public String getScript() {
        return script;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public ResourceLocation getSound() {
        return sound;
    }

    public Float getVolume() {
        return volume;
    }

    public Float getPitch() {
        return pitch;
    }
}
