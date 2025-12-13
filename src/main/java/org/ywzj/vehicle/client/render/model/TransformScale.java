package org.ywzj.vehicle.client.render.model;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class TransformScale {
    @SerializedName("thirdperson")
    @Nullable
    private Vector3f thirdPerson;
    @SerializedName("ground")
    @Nullable
    private Vector3f ground;
    @SerializedName("fixed")
    @Nullable
    private Vector3f fixed;
    @SerializedName("firstperson")
    @Nullable
    private Vector3f firstPerson;

    @Nullable
    public Vector3f getThirdPerson() {
        return thirdPerson;
    }

    @Nullable
    public Vector3f getGround() {
        return ground;
    }

    @Nullable
    public Vector3f getFixed() {
        return fixed;
    }

    @Nullable
    public Vector3f getFirstPerson() {
        return firstPerson;
    }

    @Nullable
    public Vector3f fromTransformType(ItemDisplayContext transformType) {
        switch (transformType) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                return firstPerson;
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                return thirdPerson;
            }
            case GROUND -> {
                return ground;
            }
            case FIXED -> {
                return fixed;
            }
        }
        return null;
    }
}
