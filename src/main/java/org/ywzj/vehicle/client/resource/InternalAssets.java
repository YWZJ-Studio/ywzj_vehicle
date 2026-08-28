package org.ywzj.vehicle.client.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.HandedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.maydaymemory.mae.basic.BasicAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.client.resource.vehicle.VehicleBedrockModel;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InternalAssets extends SimplePreparableReloadListener<Void> {

    public static final ResourceLocation BASIC_BULLET_MODEL = YwzjVehicle.modLocation("entity/basic_bullet");
    public static final ResourceLocation BASIC_BULLET_TEXTURE = YwzjVehicle.modLocation("textures/entity/basic_bullet.png");
    public static final ResourceLocation GRENADE_40MM_MODEL = YwzjVehicle.modLocation("entity/grenade_40mm");
    public static final ResourceLocation GRENADE_40MM_TEXTURE = YwzjVehicle.modLocation("textures/entity/grenade_40mm.png");
    public static final ResourceLocation ROCKET_57MM_MODEL = YwzjVehicle.modLocation("entity/rocket_57mm");
    public static final ResourceLocation ROCKET_57MM_TEXTURE = YwzjVehicle.modLocation("textures/entity/rocket_57mm.png");
    public static final ResourceLocation AERIAL_BOMB_MODEL = YwzjVehicle.modLocation("entity/aerial_bomb");
    public static final ResourceLocation AERIAL_BOMB_TEXTURE = YwzjVehicle.modLocation("textures/entity/aerial_bomb.png");
    public static final ResourceLocation MISSILE_AKD10_MODEL = YwzjVehicle.modLocation("entity/missile_akd10");
    public static final ResourceLocation MISSILE_AKD10_TEXTURE = YwzjVehicle.modLocation("textures/entity/missile_akd10.png");
    public static final ResourceLocation DECOY_FLARE_TEXTURE = YwzjVehicle.modLocation("textures/entity/decoy_flare.png");
    public static final ResourceLocation DECORATION_ITEM_TEXTURE = YwzjVehicle.modLocation("textures/item/decoration_item.png");
    public static final ResourceLocation REPAIR_TOOL_MODEL = YwzjVehicle.modLocation("item/repair_tool");
    public static final ResourceLocation REPAIR_TOOL_TEXTURE = YwzjVehicle.modLocation("textures/bedrock/item/repair_tool.png");
    public static final ResourceLocation REPAIR_TOOL_ANIMATION = YwzjVehicle.modLocation("item/repair_tool.animation");
    public static final ResourceLocation REPAIR_TOOL_SLOT_TEXTURE = YwzjVehicle.modLocation("textures/item/repair_tool.png");
    public static final ResourceLocation ROCKET_MOTOR_FLAME_MODEL = YwzjVehicle.modLocation("effect/rocket_motor_flame");
    public static final ResourceLocation ROCKET_MOTOR_FLAME_TEXTURE = YwzjVehicle.modLocation("textures/effect/rocket_motor_flame.png");
    public static final ResourceLocation ROCKET_MOTOR_FLAME_ANIMATION = YwzjVehicle.modLocation("effect/rocket_motor_flame.animation");
    public static final ResourceLocation MACHINE_MAX_BLOCK_MODEL = YwzjVehicle.modLocation("block/machine_max_block");
    public static final ResourceLocation MACHINE_MAX_BLOCK_TEXTURE = YwzjVehicle.modLocation("textures/block/machine_max_block.png");
    private HandedBedrockModel repairToolModel;
    private Map<String, BedrockAnimation> repairToolAnimations;
    private VehicleBedrockModel rocketMotorFlameModel;
    private BedrockAnimation rocketMotorFlameAnimation;

    @NotNull
    @Override
    @ParametersAreNonnullByDefault
    protected Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return null;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        repairToolModel = ClientAssetsManager.INSTANCE.getModel(REPAIR_TOOL_MODEL)
                .map(modelPojo -> {
                    var model = new HandedBedrockModel(modelPojo, null);
                    model.getBone("fire").illuminated = true;
                    return model;
                })
                .orElseThrow(()-> new IllegalStateException("Failed to load repair tool model. The mod may be corrupted."));
        repairToolAnimations = ClientAssetsManager.INSTANCE.getAnimation(REPAIR_TOOL_ANIMATION)
                .map(pojo -> {
                    return BedrockAnimation.createAnimation(pojo, repairToolModel)
                            .stream()
                            .collect(Collectors.toMap(BasicAnimation::getName, anim -> anim));
                })
                .orElseThrow(()-> new IllegalStateException("Failed to load repair tool animation. The mod may be corrupted."));
        rocketMotorFlameModel = ClientAssetsManager.INSTANCE.getModel(ROCKET_MOTOR_FLAME_MODEL)
                .map(modelPojo -> {
                    var model = new VehicleBedrockModel(modelPojo, List.of());
                    model.getBone("flare").illuminated = true;
                    return model;
                })
                .orElseThrow(() -> new IllegalStateException("Failed to load rocket motor flame model."));
        rocketMotorFlameAnimation = ClientAssetsManager.INSTANCE.getAnimation(ROCKET_MOTOR_FLAME_ANIMATION)
                .map(pojo -> BedrockAnimation.createAnimation(pojo, rocketMotorFlameModel))
                .flatMap(animations -> animations.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("Failed to load rocket motor flame animation."));
    }

    @NotNull
    public HandedBedrockModel getRepairToolModel() {
        return repairToolModel;
    }

    @NotNull
    public Map<String, BedrockAnimation> getRepairToolAnimations() {
        return repairToolAnimations;
    }

    @NotNull
    public VehicleBedrockModel getRocketMotorFlameModel() {
        return rocketMotorFlameModel;
    }

    @NotNull
    public BedrockAnimation getRocketMotorFlameAnimation() {
        return rocketMotorFlameAnimation;
    }

}
