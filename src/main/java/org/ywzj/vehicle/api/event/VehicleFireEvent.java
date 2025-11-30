package org.ywzj.vehicle.api.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import org.jetbrains.annotations.Nullable;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.vehicle.weapon.AbstractVehicleWeapon;

/**
 * 载具开火事件
 */
public abstract class VehicleFireEvent extends VehicleEvent {

    private final AbstractVehicleWeapon<?> weapon;
    @Nullable
    private final LivingEntity operator;

    private VehicleFireEvent(AbstractVehicle vehicle, AbstractVehicleWeapon<?> weapon, @Nullable LivingEntity operator) {
        super(vehicle);
        this.weapon = weapon;
        this.operator = operator;
    }

    /**
     * 在客户端侧，当玩家按下开火按钮，尚未通知服务器前触发（仅操作者的客户端）<br/>
     * 在服务端侧，当载具武器准备开火前触发<br />
     * 取消该事件可阻止载具开火
     */
    @Cancelable
    public static class Pre extends VehicleFireEvent {
        public Pre(AbstractVehicle vehicle, AbstractVehicleWeapon<?> weapon, @Nullable LivingEntity operator) {
            super(vehicle, weapon, operator);
        }
    }

    /**
     * 在客户端侧，服务端逻辑执行完毕，且客户端收到回调通知时触发<br/>
     * 在服务端侧，当载具武器完成开火后触发
     */
    public static class Post extends VehicleFireEvent {
        public Post(AbstractVehicle vehicle, AbstractVehicleWeapon<?> weapon, @Nullable LivingEntity operator) {
            super(vehicle, weapon, operator);
        }
    }

    public AbstractVehicleWeapon<?> getWeapon() {
        return weapon;
    }

    public boolean isClientSide() {
        return this.getVehicle().level().isClientSide();
    }

    @Nullable
    public LivingEntity getOperator() {
        return operator;
    }

}
