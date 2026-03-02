package com.mystichorizons.mysticnametags.playtime;

import com.hypixel.hytale.logger.HytaleLogger;
import com.mystichorizons.mysticnametags.stats.PlayerStatManager;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Tracks online players and periodically increments their playtime stat.
 *
 * Backed by PlayerStatManager using key "custom.playtime_seconds".
 */
public final class PlaytimeService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Stat key used for internal playtime storage.
     *
     * Parsed by PlayerStatManager as:
     *   category = "custom"
     *   stat     = "playtime_seconds"
     */
    public static final String STAT_KEY = "custom.playtime_seconds";

    private final ScheduledExecutorService scheduler;
    private final Set<UUID> onlinePlayers = ConcurrentHashMap.newKeySet();
    private final long intervalSeconds;

    private volatile boolean started = false;

    /**
     * @param intervalSeconds how often to increment playtime, in seconds.
     *                        Each tick adds intervalSeconds to the stat.
     */
    public PlaytimeService(long intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds must be > 0");
        }
        this.intervalSeconds = intervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MysticNameTags-Playtime");
            t.setDaemon(true);
            return t;
        });
    }

    public void markOnline(@Nonnull UUID uuid) {
        onlinePlayers.add(uuid);
    }

    public void markOffline(@Nonnull UUID uuid) {
        onlinePlayers.remove(uuid);
    }

    public void start() {
        if (started) {
            LOGGER.at(Level.FINE).log("[MysticNameTags] PlaytimeService already started; skipping.");
            return;
        }
        started = true;

        scheduler.scheduleAtFixedRate(
                this::tickPlaytime,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );

        LOGGER.at(Level.INFO)
                .log("[MysticNameTags] PlaytimeService started (interval=" + intervalSeconds + "s).");
    }

    private void tickPlaytime() {
        try {
            if (onlinePlayers.isEmpty()) {
                return;
            }

            PlayerStatManager mgr = PlayerStatManager.get();
            if (mgr == null) {
                LOGGER.at(Level.FINE)
                        .log("[MysticNameTags] PlayerStatManager not initialized; skipping playtime tick.");
                return;
            }

            for (UUID uuid : onlinePlayers) {
                try {
                    mgr.addToStat(uuid, STAT_KEY, intervalSeconds);
                } catch (Throwable t) {
                    LOGGER.at(Level.WARNING)
                            .withCause(t)
                            .log("[MysticNameTags] Failed to increment playtime for " + uuid);
                }
            }
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING)
                    .withCause(t)
                    .log("[MysticNameTags] Unexpected error in PlaytimeService tick.");
        }
    }

    public void shutdown() {
        try {
            scheduler.shutdownNow();
        } catch (Throwable ignored) {
        } finally {
            onlinePlayers.clear();
            LOGGER.at(Level.INFO).log("[MysticNameTags] PlaytimeService stopped.");
        }
    }

    public long getPlaytimeSeconds(@Nonnull UUID uuid) {
        PlayerStatManager mgr = PlayerStatManager.get();
        if (mgr == null) {
            return 0L;
        }
        return mgr.getStatLong(uuid, STAT_KEY);
    }
}