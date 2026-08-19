package org.ywzj.vehicle.all;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllConfigs {

    public static CommonConfig common;
    public static ServerConfig server;
    public static List<String> figureBoxCaptureBlacklist = new ArrayList<>();
    public static List<String> serverBroadcastEntityWhitelist = new ArrayList<>();

    public static void register(ModLoadingContext context, IEventBus modEventBus) {
        Pair<CommonConfig, ModConfigSpec> specPairCommon = new ModConfigSpec.Builder().configure(CommonConfig::new);
        common = specPairCommon.getLeft();
        context.getActiveContainer().registerConfig(ModConfig.Type.COMMON, specPairCommon.getRight());
        Pair<ServerConfig, ModConfigSpec> specPairServer = new ModConfigSpec.Builder().configure(ServerConfig::new);
        server = specPairServer.getLeft();
        context.getActiveContainer().registerConfig(ModConfig.Type.SERVER, specPairServer.getRight());
        modEventBus.addListener((ModConfigEvent.Loading event) -> Cached.refresh());
        modEventBus.addListener((ModConfigEvent.Reloading event) -> Cached.refresh());
        loadExternal();
    }

    /**
     * Cached config values for physics to read every vehicle tick.
     */
    public static final class Cached {

        public static volatile boolean invertedCollisionQuery = true;
        public static volatile boolean canDestroyBlock = true;
        public static volatile double ramBreakMomentum = 0.125;
        public static volatile boolean selfRighting = true;
        public static volatile boolean tumbling = false;
        public static volatile double rightingEffortPerPlayer = 30.0;
        public static volatile boolean deckParenting = true;
        public static volatile boolean carrierDecks = true;
        public static volatile boolean deckHarness = true;
        public static volatile boolean asyncVehiclePhysics = true;
        public static volatile boolean restingVehicleSleep = true;
        public static volatile int physicsThreads = 0;

        static void refresh() {
            if (common == null) {
                return;
            }
            invertedCollisionQuery = common.invertedCollisionQuery.get();
            canDestroyBlock = common.canDestroyBlock.get();
            ramBreakMomentum = common.ramBreakMomentum.get();
            selfRighting = common.selfRighting.get();
            tumbling = common.tumbling.get();
            rightingEffortPerPlayer = common.rightingEffortPerPlayer.get();
            deckParenting = common.deckParenting.get();
            carrierDecks = common.carrierDecks.get();
            deckHarness = common.deckHarness.get();
            asyncVehiclePhysics = common.asyncVehiclePhysics.get();
            restingVehicleSleep = common.restingVehicleSleep.get();
            physicsThreads = common.physicsThreads.get();
        }

    }

    public static void loadExternal() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("limitless_vehicle");
        Path figureBoxCaptureBlacklistPath = configDir.resolve("figure_box_capture_blacklist.txt");
        Path serverBroadcastEntityWhitelistPath = configDir.resolve("server_broadcast_entity_whitelist.txt");
        try {
            if (Files.notExists(configDir)) Files.createDirectories(configDir);
            if (Files.notExists(figureBoxCaptureBlacklistPath)) {
                List<String> defaultLines = Arrays.asList(
                        "minecraft:ender_dragon",
                        "corpse:corpse",
                        "twilightforest:naga"
                );
                Files.write(figureBoxCaptureBlacklistPath, defaultLines);
            }
            if (Files.notExists(serverBroadcastEntityWhitelistPath)) {
                List<String> defaultLines = Arrays.asList(
                        "superbwarfare:.*"
                );
                Files.write(serverBroadcastEntityWhitelistPath, defaultLines);
            }
            figureBoxCaptureBlacklist.clear();
            figureBoxCaptureBlacklist = Files.readAllLines(figureBoxCaptureBlacklistPath);
            figureBoxCaptureBlacklist.removeIf(line -> line.startsWith("#") || line.trim().isEmpty());
            serverBroadcastEntityWhitelist.clear();
            serverBroadcastEntityWhitelist = Files.readAllLines(serverBroadcastEntityWhitelistPath);
            serverBroadcastEntityWhitelist.removeIf(line -> line.startsWith("#") || line.trim().isEmpty());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class CommonConfig {

        public final ModConfigSpec.ConfigValue<Boolean> allowMeleeDamageVehicle;
        public final ModConfigSpec.ConfigValue<Boolean> canDestroyBlock;
        public final ModConfigSpec.ConfigValue<Double> ramBreakMomentum;
        public final ModConfigSpec.ConfigValue<Boolean> explosionDropBlock;
        public final ModConfigSpec.ConfigValue<Double> vehicleExplosionHurtPassengerDamage;
        public final ModConfigSpec.ConfigValue<Double> cameraShakeMultiplier;
        public final ModConfigSpec.ConfigValue<Boolean> selfRighting;
        public final ModConfigSpec.ConfigValue<Boolean> tumbling;
        public final ModConfigSpec.ConfigValue<Double> rightingEffortPerPlayer;
        public final ModConfigSpec.ConfigValue<Boolean> invertedCollisionQuery;
        public final ModConfigSpec.ConfigValue<Boolean> arcadeFlightModel;
        public final ModConfigSpec.ConfigValue<Boolean> deckParenting;
        public final ModConfigSpec.ConfigValue<Boolean> carrierDecks;
        public final ModConfigSpec.ConfigValue<Boolean> deckHarness;
        public final ModConfigSpec.ConfigValue<Boolean> asyncVehiclePhysics;
        public final ModConfigSpec.ConfigValue<Boolean> restingVehicleSleep;
        public final ModConfigSpec.ConfigValue<Integer> physicsThreads;
        public final ModConfigSpec.ConfigValue<Boolean> infiniteFuel;
        public final ModConfigSpec.ConfigValue<List<? extends String>> fuelNameWhiteList;
        public final ModConfigSpec.ConfigValue<Boolean> hitIndicator;
        public final ModConfigSpec.ConfigValue<Boolean> checkTeamOnEnterVehicle;
        public final ModConfigSpec.ConfigValue<Boolean> figureBoxOnlyCaptureVehicle;
        public final ModConfigSpec.ConfigValue<Boolean> overload;
        public final ModConfigSpec.ConfigValue<Double> overloadCapacityMultiplier;
        public final ModConfigSpec.ConfigValue<Integer> aerobaticSmokeR;
        public final ModConfigSpec.ConfigValue<Integer> aerobaticSmokeG;
        public final ModConfigSpec.ConfigValue<Integer> aerobaticSmokeB;

        public CommonConfig(ModConfigSpec.Builder builder) {
            allowMeleeDamageVehicle = builder.comment("近战是否能伤害载具")
                    .define("allowMeleeDamageVehicle", false);
            canDestroyBlock = builder.comment("载具是否能破坏方块")
                    .define("canDestroyBlock", true);
            ramBreakMomentum = builder.comment("撞击破坏方块所需的动量（每点方块硬度），数值越小越容易撞碎")
                    .defineInRange("ramBreakMomentum", 0.125, 0.0, Double.MAX_VALUE);
            explosionDropBlock = builder.comment("爆炸是否掉落方块")
                    .define("explosionDropBlock", true);
            vehicleExplosionHurtPassengerDamage = builder.comment("载具爆炸对乘客造成的伤害值")
                    .defineInRange("vehicleExplosionHurtPassengerDamage", 512.0, 0.0, Double.MAX_VALUE);
            cameraShakeMultiplier = builder.comment("镜头晃动强度倍率")
                    .defineInRange("cameraShakeMultiplier", 1.0, 0.0, 10.0);
            selfRighting = builder.comment("倾角过大时是否自动回正")
                    .define("selfRighting", true);
            tumbling = builder.comment("载具可自由翻滚（关闭自动回正等保护，改用真实角速度）")
                    .define("tumbling", false);
            rightingEffortPerPlayer = builder.comment("每名潜行玩家能推翻的车体量（质量×主碰撞体积），需要 ceil(质量×体积/此值) 人同时推；0为禁用手动翻正")
                    .defineInRange("rightingEffortPerPlayer", 30.0, 0.0, Double.MAX_VALUE);
            invertedCollisionQuery = builder.comment("碰撞检测使用反向查询（按接触面积计算，而非车体表面采样）")
                    .define("invertedCollisionQuery", true);
            arcadeFlightModel = builder.comment("飞行器使用街机风格气动模型（贴合机头方向的阻力）")
                    .define("arcadeFlightModel", true);
            deckParenting = builder.comment("实体站上载具时使用父子坐标系承载（关闭则回退为推出修正）")
                    .define("deckParenting", true);
            carrierDecks = builder.comment("载具可停靠在其他载具声明的甲板上（模型中名为 deck 的骨骼）")
                    .define("carrierDecks", true);
            deckHarness = builder.comment("停放在甲板上的载具进入休眠，位姿直接由母舰推导（纯性能优化）")
                    .define("deckHarness", true);
            asyncVehiclePhysics = builder.comment("载具物理在工作线程上求解（结果按刻内顺序确定性应用）")
                    .define("asyncVehiclePhysics", true);
            restingVehicleSleep = builder.comment("静止且无人的载具降低物理求解频率（纯性能优化）")
                    .define("restingVehicleSleep", true);
            physicsThreads = builder.comment("物理求解线程数，0为自动（约为CPU核心数的一半）")
                    .defineInRange("physicsThreads", 0, 0, 64);
            infiniteFuel = builder.comment("无需燃油仍可运作")
                    .define("infiniteFuel", false);
            fuelNameWhiteList = builder.comment("允许视作燃油的液体")
                    .defineList("fuelNameWhiteList", Arrays.asList("fuel", "gas", "lava"), obj -> obj instanceof String);
            hitIndicator = builder.comment("开启命中提示")
                    .define("hitIndicator", true);
            checkTeamOnEnterVehicle = builder.comment("载具乘客是否需为同队")
                    .define("checkTeamOnEnterVehicle", true);
            figureBoxOnlyCaptureVehicle = builder.comment("手办盒是否只能收纳载具")
                    .define("figureBoxOnlyCaptureVehicle", false);
            overload = builder.comment("是否启用过载机制")
                    .define("overload", true);
            overloadCapacityMultiplier = builder.comment("过载耐受倍率")
                    .defineInRange("overloadCapacityMultiplier", 1.0, 0.1, 100.0);
            aerobaticSmokeR = builder.comment("特技飞行烟雾颜色 - 红")
                    .defineInRange("aerobaticSmokeR", 255, 0, 255);
            aerobaticSmokeG = builder.comment("特技飞行烟雾颜色 - 绿")
                    .defineInRange("aerobaticSmokeG", 0, 0, 255);
            aerobaticSmokeB = builder.comment("特技飞行烟雾颜色 - 蓝")
                    .defineInRange("aerobaticSmokeB", 0, 0, 255);
        }

    }

    public static class ServerConfig {

        public final ModConfigSpec.ConfigValue<Double> showVehicleInfoDistance;
        public final ModConfigSpec.ConfigValue<Integer> serverBroadcastEntitiesInterval;
        public final ModConfigSpec.ConfigValue<Integer> detachedStreamRadius;
        public final ModConfigSpec.ConfigValue<Integer> detachedMaxChunksPerTick;
        public final ModConfigSpec.ConfigValue<Integer> detachedBodyPinRadius;
        public final ModConfigSpec.ConfigValue<Boolean> detachedSuppressBodyStream;
        public final ModConfigSpec.ConfigValue<Integer> detachedBodyViewDistance;
        public final ModConfigSpec.ConfigValue<Integer> detachedBodyTicketRadius;
        public final ModConfigSpec.ConfigValue<Integer> vehicleWakeupTimeout;
        public final ModConfigSpec.ConfigValue<Integer> vehicleWakeupTicketRadius;
        public final ModConfigSpec.ConfigValue<Boolean> chunkStreamDebug;
        public final ModConfigSpec.ConfigValue<List<? extends String>> chunkStreamDebugCategories;
        public final ModConfigSpec.ConfigValue<Integer> chunkStreamDebugHeartbeat;

        public ServerConfig(ModConfigSpec.Builder builder) {
            showVehicleInfoDistance = builder.comment("允许看向载具时展示信息的最大距离（单位：block）")
                    .defineInRange("showVehicleInfoDistance", 512.0, 0.0, 1024.0);
            serverBroadcastEntitiesInterval = builder.comment("服务端向玩家广播其所在世界超视距实体的时间间隔（单位：tick）")
                    .defineInRange("serverBroadcastEntitiesInterval", 5, 1, 72000);
            builder.push("detachedBody");
            detachedStreamRadius = builder.comment("Chunk radius streamed around a remotely operated vehicle, clamped to the server view distance.")
                    .defineInRange("streamRadius", 12, 2, 32);
            detachedMaxChunksPerTick = builder.comment("Maximum chunks pushed to one operator per tick.",
                            "Taking control over already generated terrain would otherwise dump the whole view square at once and stall input for a few seconds.")
                    .defineInRange("maxChunksPerTick", 16, 1, 1024);
            detachedBodyPinRadius = builder.comment("Chunk radius kept alive around the operator's real body while they operate remotely.")
                    .defineInRange("bodyPinRadius", 1, 0, 8);
            detachedSuppressBodyStream = builder.comment("While operating remotely, stop streaming entities around the operator's real body, which they cannot see anyway.",
                            "Entities the body rides or is attached to are never hidden, and the surroundings are restored the instant control ends.")
                    .define("suppressBodyStream", true);
            detachedBodyViewDistance = builder.comment("View distance applied to the operator's real body while they operate remotely.",
                            "The body only needs enough of a view to stay simulated and collidable, so shrinking it stops the server re-sending terrain the operator cannot see. Below 2 keeps the operator's normal view distance.")
                    .defineInRange("bodyViewDistance", 2, -1, 32);
            detachedBodyTicketRadius = builder.comment("Chunk radius kept loaded around the operator's real body in place of their normal player ticket.",
                            "Releasing that ticket stops the server ticking a full simulation distance of chunks nobody is watching, at the cost of freezing mobs, redstone and farms around the body until control ends. Negative keeps the normal player ticket.")
                    .defineInRange("bodyTicketRadius", 2, -1, 8);
            vehicleWakeupTimeout = builder.comment("How long to wait for a vehicle in an unloaded chunk to load when connecting to it, in ticks.")
                    .defineInRange("wakeupTimeout", 200, 20, 6000);
            vehicleWakeupTicketRadius = builder.comment("Chunk ticket radius applied when waking a sleeping vehicle.")
                    .defineInRange("wakeupTicketRadius", 2, 1, 8);
            builder.pop();
            builder.push("chunkStreamDebug");
            chunkStreamDebug = builder.comment("Log chunk streaming diagnostics for remotely operated vehicles.")
                    .define("enabled", false);
            chunkStreamDebugCategories = builder.comment("Categories to log: ALL, SESSION, TICKET, CHUNK, CENTER, PIN, ENTITY, WAKEUP, CLIENT.")
                    .defineList("categories", List.of("ALL"), obj -> obj instanceof String);
            chunkStreamDebugHeartbeat = builder.comment("Interval between debug heartbeat lines, in ticks.")
                    .defineInRange("heartbeatTicks", 40, 1, 12000);
            builder.pop();
        }

    }

}
