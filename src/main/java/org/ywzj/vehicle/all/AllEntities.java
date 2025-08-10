package org.ywzj.vehicle.all;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.Vehicle;
import org.ywzj.vehicle.entity.BulletEntity;
import org.ywzj.vehicle.entity.Lav150;

public class AllEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Vehicle.MOD_ID);

    public static final RegistryObject<EntityType<Lav150>> LAV150 = ENTITIES.register("lav150",
            () -> EntityType.Builder.of(Lav150::new, MobCategory.MISC).sized(4f, 2.5f)
                    .clientTrackingRange(128)
                    .build("lav150"));

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
            () -> BulletEntity.TYPE);

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

}
