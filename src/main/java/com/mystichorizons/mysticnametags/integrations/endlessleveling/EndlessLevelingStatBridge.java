package com.mystichorizons.mysticnametags.integrations.endlessleveling;

import com.airijko.endlessleveling.api.EndlessLevelingAPI;
import com.airijko.endlessleveling.enums.SkillAttributeType;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Adapts EndlessLeveling stats into the MysticNameTags StatProvider world.
 *
 * Supported keys:
 *
 *   - "endlessleveling.level"             -> player level
 *   - "endlessleveling.xp"                -> current XP (rounded)
 *   - "endlessleveling.prestige"          -> prestige level
 *   - "endlessleveling.skill.<ATTR>"      -> SkillAttributeType level
 *
 * Returns null if EndlessLeveling is unavailable or key is unknown.
 */
public final class EndlessLevelingStatBridge {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PREFIX = "endlessleveling.";

    private EndlessLevelingStatBridge() {
    }

    @Nullable
    public static Integer getStatValue(@Nonnull UUID uuid, @Nonnull String key) {
        String trimmed = key.trim();
        if (!trimmed.startsWith(PREFIX)) {
            return null;
        }

        String tail = trimmed.substring(PREFIX.length());
        EndlessLevelingAPI api = EndlessLevelingAPI.get();
        if (api == null) {
            return null;
        }

        try {
            // -------- Basic stats --------

            if (tail.equalsIgnoreCase("level")) {
                int level = api.getPlayerLevel(uuid);
                return (level <= 0) ? null : level;
            }

            if (tail.equalsIgnoreCase("xp")) {
                double xp = api.getPlayerXp(uuid);
                if (xp <= 0.0D) {
                    return null;
                }
                if (xp > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                return (int) Math.round(xp);
            }

            if (tail.equalsIgnoreCase("prestige")) {
                int prestige = api.getPlayerPrestigeLevel(uuid);
                return (prestige < 0) ? null : prestige;
            }

            // -------- Skill attribute levels --------
            // Format: endlessleveling.skill.<SkillAttributeType>

            String lowerTail = tail.toLowerCase(Locale.ROOT);
            if (lowerTail.startsWith("skill.")) {
                String rawAttr = tail.substring("skill.".length());
                if (rawAttr.isBlank()) {
                    return null;
                }

                try {
                    SkillAttributeType type = SkillAttributeType.valueOf(
                            rawAttr.toUpperCase(Locale.ROOT)
                    );
                    int level = api.getSkillAttributeLevel(uuid, type);
                    return (level <= 0) ? null : level;
                } catch (IllegalArgumentException ex) {
                    LOGGER.at(Level.FINE)
                            .withCause(ex)
                            .log("[MysticNameTags] Unknown EndlessLeveling skill attribute: %s", rawAttr);
                    return null;
                }
            }

            return null;
        } catch (Throwable t) {
            LOGGER.at(Level.FINE)
                    .withCause(t)
                    .log("[MysticNameTags] Failed to read EndlessLeveling stat '%s' for %s", tail, uuid);
            return null;
        }
    }
}