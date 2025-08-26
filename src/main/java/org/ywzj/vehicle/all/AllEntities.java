package org.ywzj.vehicle.all;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.Vehicle;
import org.ywzj.vehicle.entity.vehicle.Lav150;
import org.ywzj.vehicle.entity.vehicle.Ztl11;
import org.ywzj.vehicle.entity.weapon.BulletEntity;

public class AllEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Vehicle.MOD_ID);

    public static final RegistryObject<EntityType<Lav150>> LAV150 = ENTITIES.register("lav150",
            () -> EntityType.Builder.of(Lav150::new, MobCategory.MISC).sized(1.5f, 2.5f)
                    .clientTrackingRange(16)
                    .build("lav150"));

    public static final RegistryObject<EntityType<Ztl11>> ZTL11 = ENTITIES.register("ztl11",
            () -> EntityType.Builder.of(Ztl11::new, MobCategory.MISC).sized(2.5f, 2.5f)
                    .clientTrackingRange(128)
                    .build("ztl11"));

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .sized(0.0625F, 0.0625F)
                    .clientTrackingRange(5)
                    .updateInterval(5)
                    .setShouldReceiveVelocityUpdates(false)
                    .setCustomClientFactory(BulletEntity::new)
                    .build("bullet"));

    @SubscribeEvent
    public static void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
        event.put(AllEntities.LAV150.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).add(Attributes.MOVEMENT_SPEED, 0.4D).build());
        event.put(AllEntities.ZTL11.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).add(Attributes.MOVEMENT_SPEED, 0.4D).build());
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        eventBus.register(AllEntities.class);
    }

}
