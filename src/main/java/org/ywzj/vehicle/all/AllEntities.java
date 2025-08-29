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
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.weapon.BulletEntity;

public class AllEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YwzjVehicle.MOD_ID);

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
        AllVehicles.getVehicles().forEach(vehicle -> event.put(vehicle.getEntityType(),
                Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, vehicle.getHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.4D).build()));
    }

    public static void register(IEventBus eventBus) {
        AllVehicles.getVehicles().forEach(AllVehicles.Vehicle::registerEntity);
        ENTITIES.register(eventBus);
        eventBus.register(AllEntities.class);
    }

}
