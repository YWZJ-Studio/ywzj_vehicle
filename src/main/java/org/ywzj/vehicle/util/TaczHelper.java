package org.ywzj.vehicle.util;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class TaczHelper {

    public static void shoot(Vec3 ammoSpawnPosition, ItemStack itemStack, Supplier<Float> pitch, Supplier<Float> yaw, boolean tracer, LivingEntity shooter, Vec3 targetPos) {
        if (!(itemStack.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }
        ResourceLocation gunId = getGunId(itemStack);
        ResourceLocation gunDisplayId = gunItem.getGunDisplayId(itemStack);
        IGun iGun = IGun.getIGunOrNull(itemStack);
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
        FireMode fireMode = iGun.getFireMode(itemStack);
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
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
            if (shooter.isDeadOrDying()) {
                return false;
            }
            // 触发击发事件
            boolean fire = !MinecraftForge.EVENT_BUS.post(new GunFireEvent(shooter, itemStack, LogicalSide.SERVER));
            if (fire) {
                NetworkHandler.sendToTrackingEntity(new ServerMessageGunFire(shooter.getId(), itemStack), shooter);
                // 生成子弹
                Level world = shooter.level();
                for (int i = 0; i < bulletAmount; i++) {
                    doSpawnBulletEntity(ammoSpawnPosition, world, shooter, itemStack, pitch.get(), yaw.get(), finalSpeed, 0, ammoId, gunId, tracer, gunData, bulletData, targetPos);
                }
                // 播放枪声
                if (soundDistance > 0) {
                    String soundId = useSilenceSound ? SoundManager.SILENCE_3P_SOUND : SoundManager.SHOOT_3P_SOUND;
                    SoundManager.sendSoundToNearby(shooter, soundDistance, gunId, gunDisplayId, soundId, 0.8f, 0.9f + shooter.getRandom().nextFloat() * 0.125f);
                }
            }
            return true;
        }, period, cycles);
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
