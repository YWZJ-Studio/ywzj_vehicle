package org.ywzj.vehicle.all;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
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

    public static void register(ModLoadingContext context) {
        Pair<CommonConfig, ModConfigSpec> specPairCommon = new ModConfigSpec.Builder().configure(CommonConfig::new);
        common = specPairCommon.getLeft();
        context.getActiveContainer().registerConfig(ModConfig.Type.COMMON, specPairCommon.getRight());
        Pair<ServerConfig, ModConfigSpec> specPairServer = new ModConfigSpec.Builder().configure(ServerConfig::new);
        server = specPairServer.getLeft();
        context.getActiveContainer().registerConfig(ModConfig.Type.SERVER, specPairServer.getRight());
        loadExternal();
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
        public final ModConfigSpec.ConfigValue<Boolean> explosionDropBlock;
        public final ModConfigSpec.ConfigValue<Double> vehicleExplosionHurtPassengerDamage;
        public final ModConfigSpec.ConfigValue<Boolean> selfRighting;
        public final ModConfigSpec.ConfigValue<Boolean> infiniteFuel;
        public final ModConfigSpec.ConfigValue<List<? extends String>> fuelNameWhiteList;
        public final ModConfigSpec.ConfigValue<Boolean> hitIndicator;
        public final ModConfigSpec.ConfigValue<Boolean> checkTeamOnEnterVehicle;
        public final ModConfigSpec.ConfigValue<Boolean> figureBoxOnlyCaptureVehicle;

        public CommonConfig(ModConfigSpec.Builder builder) {
            allowMeleeDamageVehicle = builder.comment("近战是否能伤害载具")
                    .define("allowMeleeDamageVehicle", false);
            canDestroyBlock = builder.comment("载具是否能破坏方块")
                    .define("canDestroyBlock", true);
            explosionDropBlock = builder.comment("爆炸是否掉落方块")
                    .define("explosionDropBlock", true);
            vehicleExplosionHurtPassengerDamage = builder.comment("载具爆炸对乘客造成的伤害值")
                    .defineInRange("vehicleExplosionHurtPassengerDamage", 512.0, 0.0, Double.MAX_VALUE);
            selfRighting = builder.comment("倾角过大时是否自动回正")
                    .define("selfRighting", true);
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
        }

    }

    public static class ServerConfig {

        public final ModConfigSpec.ConfigValue<Double> showVehicleInfoDistance;
        public final ModConfigSpec.ConfigValue<Integer> serverBroadcastEntitiesInterval;

        public ServerConfig(ModConfigSpec.Builder builder) {
            showVehicleInfoDistance = builder.comment("允许看向载具时展示信息的最大距离（单位：block）")
                    .defineInRange("showVehicleInfoDistance", 512.0, 0.0, 1024.0);
            serverBroadcastEntitiesInterval = builder.comment("服务端向玩家广播其所在世界超视距实体的时间间隔（单位：tick）")
                    .defineInRange("serverBroadcastEntitiesInterval", 5, 1, 72000);
        }

    }

}
