package com.mystichorizons.mysticnametags.integrations;

import com.hypixel.hytale.logger.HytaleLogger;
import com.zib.playtime.api.PlaytimeAPI;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * PlaytimeProvider that delegates to Zid's Playtime mod.
 *
 * Uses PlaytimeAPI#getTotalPlaytime(UUID) (milliseconds) and converts to minutes.
 */
public final class ZibPlaytimeProvider implements PlaytimeProvider {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final PlaytimeAPI api;

    public ZibPlaytimeProvider(@Nonnull PlaytimeAPI api) {
        this.api = api;
    }

    @Override
    public long getPlaytimeMinutes(@Nonnull UUID uuid) {
        try {
            long millis = api.getTotalPlaytime(uuid);
            if (millis <= 0L) {
                return 0L;
            }
            return TimeUnit.MILLISECONDS.toMinutes(millis);
        } catch (Throwable t) {
            LOGGER.at(Level.FINE)
                    .withCause(t)
                    .log("[MysticNameTags] Zid Playtime API failed for %s", uuid);
            return 0L;
        }
    }
}