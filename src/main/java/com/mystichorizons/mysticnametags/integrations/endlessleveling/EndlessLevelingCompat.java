package com.mystichorizons.mysticnametags.integrations.endlessleveling;

import com.airijko.endlessleveling.api.EndlessLevelingAPI;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * API-only compatibility bridge for EndlessLeveling.
 *
 * MysticNameTags should prefer the public EndlessLevelingAPI surface
 * rather than directly touching internal managers.
 */
public final class EndlessLevelingCompat {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private EndlessLevelingCompat() {
    }

    public static boolean isAvailable() {
        try {
            Class.forName("com.airijko.endlessleveling.api.EndlessLevelingAPI");
            EndlessLevelingAPI api = EndlessLevelingAPI.get();
            return api != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    public static EndlessLevelingAPI getApi() {
        try {
            return EndlessLevelingAPI.get();
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to access EndlessLevelingAPI.");
            return null;
        }
    }

    @Nonnull
    public static String getLevel(@Nonnull UUID uuid) {
        EndlessLevelingAPI api = getApi();
        if (api == null) {
            return "";
        }

        try {
            int level = Math.max(1, api.getPlayerLevel(uuid));
            return String.valueOf(level);
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to read EndlessLeveling level for %s", uuid);
            return "";
        }
    }

    @Nonnull
    public static String getPrestige(@Nonnull UUID uuid,
                                     boolean enabled,
                                     @Nullable String prefix) {
        if (!enabled) {
            return "";
        }

        EndlessLevelingAPI api = getApi();
        if (api == null) {
            return "";
        }

        try {
            int prestige = Math.max(0, api.getPlayerPrestigeLevel(uuid));
            if (prestige <= 0) {
                return "";
            }

            return (prefix == null ? "" : prefix) + prestige;
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to read EndlessLeveling prestige for %s", uuid);
            return "";
        }
    }

    @Nonnull
    public static String getRaceId(@Nonnull UUID uuid) {
        EndlessLevelingAPI api = getApi();
        if (api == null) {
            return "";
        }

        try {
            String race = api.getRaceId(uuid);
            return race == null ? "" : race.trim();
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to read EndlessLeveling race for %s", uuid);
            return "";
        }
    }

    @Nonnull
    public static String getPrimaryClassId(@Nonnull UUID uuid) {
        EndlessLevelingAPI api = getApi();
        if (api == null) {
            return "";
        }

        try {
            String value = api.getPrimaryClassId(uuid);
            return value == null ? "" : value.trim();
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to read EndlessLeveling primary class for %s", uuid);
            return "";
        }
    }

    @Nonnull
    public static String getSecondaryClassId(@Nonnull UUID uuid) {
        EndlessLevelingAPI api = getApi();
        if (api == null) {
            return "";
        }

        try {
            String value = api.getSecondaryClassId(uuid);
            return value == null ? "" : value.trim();
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to read EndlessLeveling secondary class for %s", uuid);
            return "";
        }
    }

    @Nonnull
    public static String buildStateKey(@Nonnull UUID uuid) {
        EndlessLevelingAPI api = getApi();
        if (api == null) {
            return "";
        }

        try {
            int level = Math.max(1, api.getPlayerLevel(uuid));
            int prestige = Math.max(0, api.getPlayerPrestigeLevel(uuid));

            String race = api.getRaceId(uuid);
            if (race == null) race = "";

            String primary = api.getPrimaryClassId(uuid);
            if (primary == null) primary = "";

            String secondary = api.getSecondaryClassId(uuid);
            if (secondary == null) secondary = "";

            return level + "|" + prestige + "|" + race + "|" + primary + "|" + secondary;
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to build EndlessLeveling state key for %s", uuid);
            return "";
        }
    }
}