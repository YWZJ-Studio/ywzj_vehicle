package org.ywzj.vehicle.stream;

import net.minecraft.world.level.ChunkPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ywzj.vehicle.all.AllConfigs;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkStreamDebug {

    public enum Category {
        SESSION,
        TICKET,
        CHUNK,
        CENTER,
        PIN,
        ENTITY,
        WAKEUP,
        CLIENT,
        CACHE,
        TRACKER
    }

    private static final Logger LOG = LogManager.getLogger("ywzj_vehicle|chunk-stream");
    private static final Map<String, String> LAST_STATE = new ConcurrentHashMap<>();
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static volatile EnumSet<Category> override;
    private static volatile EnumSet<Category> resolved = EnumSet.noneOf(Category.class);

    private ChunkStreamDebug() {
    }

    public static void setOverride(EnumSet<Category> categories) {
        override = categories;
        refresh();
        LOG.info("debug categories overridden -> {}", categories == null ? "config" : categories);
    }


    public static void refresh() {
        EnumSet<Category> current = override;
        if (current != null) {
            resolved = current;
            return;
        }
        EnumSet<Category> fromConfig = EnumSet.noneOf(Category.class);
        try {
            if (AllConfigs.server.chunkStreamDebug.get()) {
                for (String raw : AllConfigs.server.chunkStreamDebugCategories.get()) {
                    String name = raw.trim().toUpperCase(Locale.ROOT);
                    if (name.equals("ALL")) {
                        fromConfig = EnumSet.allOf(Category.class);
                        break;
                    }
                    try {
                        fromConfig.add(Category.valueOf(name));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
            fromConfig = EnumSet.noneOf(Category.class);
        }
        resolved = fromConfig;
    }

    public static EnumSet<Category> active() {
        return resolved;
    }

    public static boolean on(Category category) {
        return resolved.contains(category);
    }

    public static boolean any() {
        return !resolved.isEmpty();
    }

    public static void log(Category category, String format, Object... args) {
        if (on(category)) {
            LOG.info("[" + category + "] " + format, args);
        }
    }


    public static void report(String format, Object... args) {
        LOG.info(format, args);
    }

    public static void warn(Category category, String format, Object... args) {
        if (on(category)) {
            LOG.warn("[" + category + "] " + format, args);
        }
    }

    public static void state(Category category, String key, String state) {
        String previous = LAST_STATE.put(key, state);
        if (!state.equals(previous) && on(category)) {
            LOG.info("[{}] {}: {}", category, key, state);
        }
    }

    public static void clearState(String key) {
        LAST_STATE.remove(key);
    }

    public static int heartbeatTicks() {
        try {
            return Math.max(1, AllConfigs.server.chunkStreamDebugHeartbeat.get());
        } catch (Exception e) {
            return 40;
        }
    }

    public static Session session(UUID playerId, String playerName) {
        return SESSIONS.computeIfAbsent(playerId, id -> {
            Session session = new Session(id, playerName);
            log(Category.SESSION, "open {} ({})", playerName, shortId(id));
            return session;
        });
    }

    public static Session peek(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    public static void endSession(UUID playerId, String reason) {
        Session session = SESSIONS.remove(playerId);
        if (session != null) {
            log(Category.SESSION, "close {} ({}) reason={} | {}", session.playerName, shortId(playerId), reason, session.summary());
        }
        LAST_STATE.keySet().removeIf(key -> key.contains(shortId(playerId)));
    }

    public static List<String> status() {
        List<String> lines = new ArrayList<>();
        EnumSet<Category> categories = active();
        lines.add("categories=" + (categories.isEmpty() ? "off" : categories.toString())
                + (override != null ? " (command override)" : " (config)"));
        lines.add("heartbeat=" + heartbeatTicks() + "t radius=" + streamRadiusForStatus()
                + " budget=" + budgetForStatus() + "/tick");
        if (SESSIONS.isEmpty()) {
            lines.add("no active detached sessions");
        } else {
            for (Session session : SESSIONS.values()) {
                lines.add(session.playerName + ": " + session.summary());
            }
        }
        return lines;
    }

    private static int streamRadiusForStatus() {
        try {
            return AllConfigs.server.detachedStreamRadius.get();
        } catch (Exception e) {
            return -1;
        }
    }

    private static int budgetForStatus() {
        try {
            return AllConfigs.server.detachedMaxChunksPerTick.get();
        } catch (Exception e) {
            return -1;
        }
    }

    public static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    public static final class Session {

        public final UUID playerId;
        public final String playerName;
        public long chunksSent;
        public long chunksForgotten;
        public long chunkMisses;
        public long ticketsIssued;
        public long centerUpdates;
        public long pinsSent;
        public long pinsDropped;
        public long entityReveals;
        public long visibilityScans;
        public long bodyChunksDisplaced;
        public int liveChunks;
        public int radius;
        public String vehicle = "-";
        public ChunkPos center;
        private long sentAtLastHeartbeat;

        private Session(UUID playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
        }

        public void heartbeat() {
            log(Category.SESSION, "heartbeat {}: {} (+{} chunks since last)",
                    playerName, summary(), chunksSent - sentAtLastHeartbeat);
            sentAtLastHeartbeat = chunksSent;
        }

        public String summary() {
            return String.format(Locale.ROOT,
                    "vehicle=%s center=%s r=%d live=%d sent=%d forgot=%d miss=%d tickets=%d center-updates=%d pins=%d/%d reveals=%d scans=%d body-displaced=%d",
                    vehicle, center, radius, liveChunks, chunksSent, chunksForgotten, chunkMisses,
                    ticketsIssued, centerUpdates, pinsSent, pinsDropped, entityReveals, visibilityScans,
                    bodyChunksDisplaced);
        }

    }

}
