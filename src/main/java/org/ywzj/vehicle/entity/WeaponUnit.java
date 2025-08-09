package org.ywzj.vehicle.entity;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunFire;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.AmmoSpeedModifier;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.CycleTaskHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Objects;
import java.util.Optional;

public class WeaponUnit {
    private final AbstractVehicle vehicle;
    private final ItemStack taczWeapon;
    private Player operator;
    public float aimXRot;
    public float aimYRot;
    public float xRot;
    public float yRot;
    public float xRotO;
    public float yRotO;
    public float xRotSpeed;
    public  float yRotSpeed;
    public float maxXRot;

    public WeaponUnit(AbstractVehicle vehicle, ItemStack taczWeapon) {
        this.vehicle = vehicle;
        this.taczWeapon = taczWeapon;
    }

    public ItemStack getTaczWeapon() {
        return taczWeapon;
    }

    public void setOperator(Player operator) {
        this.operator = operator;
        if (!vehicle.level().isClientSide) {
            IGunOperator.fromLivingEntity(operator).draw(() -> taczWeapon);
        }
    }

    public void shoot(Vec3 ammoSpawnPosition, boolean tracer, Vec3 targetPos) {
        if (operator == null) {
            return;
        }
        if (!(taczWeapon.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }
        ResourceLocation gunId = getGunId(taczWeapon);
        ResourceLocation gunDisplayId = gunItem.getGunDisplayId(taczWeapon);
        IGun iGun = IGun.getIGunOrNull(taczWeapon);
        if (iGun == null) {
            return;
        }
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return;
        }
        CommonGunIndex gunIndex = gunIndexOptional.get();
        BulletData bulletData = gunIndex.getBulletData();
        GunData gunData = gunIndex.getGunData();
        ResourceLocation ammoId = gunData.getAmmoId();
        FireMode fireMode = iGun.getFireMode(taczWeapon);
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(operator).getCacheProperty();
        if (cacheProperty == null) {
            return;
        }

        // 消音器影响
        Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
        final int soundDistance = silence.first();
        final boolean useSilenceSound = silence.right();

        // 子弹飞行速度
        float speed = cacheProperty.<Float>getCache(AmmoSpeedModifier.ID);
        float finalSpeed = Mth.clamp(speed / 20, 0, Float.MAX_VALUE);
        // 弹丸数量
        int bulletAmount = Math.max(bulletData.getBulletAmount(), 1);
        // 连发数量
        int cycles = fireMode == FireMode.BURST ? gunData.getBurstData().getCount() : 1;
        // 连发间隔
        long period = fireMode == FireMode.BURST ? gunData.getBurstShootInterval() : 1;

        CycleTaskHelper.addCycleTask(() -> {
            // 如果射击者死亡，取消射击
            if (operator.isDeadOrDying()) {
                return false;
            }
            // 触发击发事件
            boolean fire = !MinecraftForge.EVENT_BUS.post(new GunFireEvent(operator, taczWeapon, LogicalSide.SERVER));
            if (fire) {
                NetworkHandler.sendToTrackingEntity(new ServerMessageGunFire(operator.getId(), taczWeapon), operator);
                // 生成子弹
                Level world = operator.level();
                for (int i = 0; i < bulletAmount; i++) {
                    doSpawnBulletEntity(ammoSpawnPosition, world, operator, taczWeapon, xRot, yRot, finalSpeed, 0, ammoId, gunId, tracer, gunData, bulletData, targetPos);
                }
                // 播放枪声
                if (soundDistance > 0) {
                    String soundId = useSilenceSound ? SoundManager.SILENCE_3P_SOUND : SoundManager.SHOOT_3P_SOUND;
                    SoundManager.sendSoundToNearby(vehicle, soundDistance, gunId, gunDisplayId, soundId, 0.8f, 0.9f + operator.getRandom().nextFloat() * 0.125f);
                }
            }
            return true;
        }, period, cycles);
    }

    public void tick() {
        if (vehicle.level().isClientSide()) {
            this.xRotO = this.xRot;
            this.yRotO = this.yRot;
        } else {
            float xDiff = Mth.wrapDegrees(this.aimXRot - this.xRot);
            float yDiff = Mth.wrapDegrees(this.aimYRot - this.yRot);

            if (Math.abs(xDiff) > xRotSpeed) {
                this.xRot += Math.signum(xDiff) * xRotSpeed;
            } else {
                this.xRot = this.aimXRot;
            }
            this.xRot = Math.max(Math.min(this.xRot, maxXRot), -maxXRot);

            if (Math.abs(yDiff) > yRotSpeed) {
                this.yRot += Math.signum(yDiff) * yRotSpeed;
            } else {
                this.yRot = this.aimYRot;
            }
        }
    }

    private static ResourceLocation getGunId(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        if (nbt.contains("GunId", Tag.TAG_STRING)) {
            ResourceLocation gunId = ResourceLocation.tryParse(nbt.getString("GunId"));
            return Objects.requireNonNullElse(gunId, DefaultAssets.EMPTY_GUN_ID);
        }
        return DefaultAssets.EMPTY_GUN_ID;
    }

    protected static void doSpawnBulletEntity(Vec3 ammoSpawnPosition, Level world, LivingEntity shooter, ItemStack gunItem, float pitch, float yaw, float speed, float inaccuracy, ResourceLocation ammoId, ResourceLocation gunId, boolean tracer, GunData gunData, BulletData bulletData, Vec3 targetPos) {
        EntityKineticBullet bullet = new EntityKineticBullet(world, shooter, gunItem, ammoId, gunId, tracer, gunData, bulletData);
        if (ammoSpawnPosition != null) {
            bullet.setPos(ammoSpawnPosition);
        }
        if (targetPos != null) {
            // 考虑重力进行弹道计算
            double d = bullet.position().distanceTo(targetPos);
            Vec3 v = new Vec3(targetPos.x - bullet.getX(), targetPos.y - bullet.getY(), targetPos.z - bullet.getZ()).normalize().scale(speed);
            double t = d / v.length();
            double dv = 0.5 * bulletData.getGravity() * t;
            v = v.add(0, dv, 0).normalize();
            bullet.shoot(v.x, v.y, v.z, speed, inaccuracy);
        } else {
            bullet.shootFromRotation(bullet, pitch, yaw, 0.0F, speed, inaccuracy);
        }
        world.addFreshEntity(bullet);
    }

}
