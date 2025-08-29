package org.ywzj.vehicle.all;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import org.ywzj.vehicle.YwzjVehicle;
import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;
import org.ywzj.vehicle.entity.vehicle.Lav150;
import org.ywzj.vehicle.entity.vehicle.Motorcycle;
import org.ywzj.vehicle.entity.vehicle.Ztl11;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AllVehicles {

    private final static ConcurrentHashMap<String, Vehicle> ALL_VEHICLES = new ConcurrentHashMap<>();

    public static final Vehicle LAV150 = registerVehicle("lav150", Lav150.class);
    public static final Vehicle ZTL11 = registerVehicle("ztl11", Ztl11.class);
    public static final Vehicle MOTORCYCLE = registerVehicle("motorcycle", Motorcycle.class);

    public static void register() {}

    public static Vehicle registerVehicle(String name, Class<? extends AbstractVehicle> entityClass) {
        Vehicle vehicle = new Vehicle(name, entityClass);
        ALL_VEHICLES.put(name, vehicle);
        YwzjVehicle.LOGGER.info("Vehicle {} registered", name);
        return vehicle;
    }

    public static List<Vehicle> getVehicles() {
        return new ArrayList<>(ALL_VEHICLES.values());
    }

    public static class Vehicle {

        private final String name;
        private final Class<? extends AbstractVehicle> entityClass;
        private RegistryObject<EntityType<? extends AbstractVehicle>> entityTypeRegistryObject;
        private final ResourceLocation visualBedrockModel;
        private final ResourceLocation visualBedrockTexture;
        private final ResourceLocation structureBedrockModel;

        public Vehicle(String name, Class<? extends AbstractVehicle> entityClass) {
            this.name = name;

            //todo: 读取Data
//            this.health = 100f;

            this.entityClass = entityClass;
            this.visualBedrockModel = YwzjVehicle.modLoc("bedrock/entity/" + name);
            this.visualBedrockTexture = YwzjVehicle.modLoc("textures/entity/" + name +".png");
            this.structureBedrockModel = YwzjVehicle.modLoc("bedrock/entity/" + name + "_structure");
        }

        public String getName() {
            return name;
        }

        //todo: 读取Data
        public Float getHealth() {
            return 100f;
        }

        public ResourceLocation getVisualBedrockModel() {
            return visualBedrockModel;
        }

        public ResourceLocation getVisualBedrockTexture() {
            return visualBedrockTexture;
        }

        public ResourceLocation getStructureBedrockModel() {
            return structureBedrockModel;
        }

        public RegistryObject<EntityType<? extends AbstractVehicle>> registerEntity() {
            Constructor<? extends AbstractVehicle> constructor;
            try {
                constructor = entityClass.getConstructor(EntityType.class, Level.class);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
            entityTypeRegistryObject = AllEntities.ENTITIES.register(name, () -> EntityType.Builder
                    .<AbstractVehicle>of((type, level) -> {
                        try {
                            return constructor.newInstance(type, level);
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    }, MobCategory.MISC)
                    .sized(1f, 1f)
                    .clientTrackingRange(16)
                    .build(name));
            return entityTypeRegistryObject;
        }

        public EntityType getEntityType() {
            return entityTypeRegistryObject.get();
        }

    }

}
