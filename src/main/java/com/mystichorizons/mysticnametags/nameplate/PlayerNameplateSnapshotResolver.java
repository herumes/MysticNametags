package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mystichorizons.mysticnametags.tags.Tag;
import com.mystichorizons.mysticnametags.tags.TagManager;

import java.util.logging.Level;

public final class PlayerNameplateSnapshotResolver {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PlayerNameplateSnapshot resolve(PlayerRef playerRef) {
        if (playerRef == null) {
            return new PlayerNameplateSnapshot("", "", null);
        }

        String playerName = resolvePlayerName(playerRef);
        String rank = resolveRank(playerRef);
        Tag activeTag = resolveActiveTag(playerRef);

        return new PlayerNameplateSnapshot(rank, playerName, activeTag);
    }

    private String resolvePlayerName(PlayerRef playerRef) {
        try {
            // Replace later with your preferred live player/display-name resolution if needed.
            String value = playerRef.getName();
            return value == null ? "" : value;
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to resolve player name for snapshot.");
            return "";
        }
    }

    private String resolveRank(PlayerRef playerRef) {
        try {
            // Section 4 intentionally keeps rank resolution simple.
            // If you already have a rank/prefix source, wire it here later.
            return "";
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to resolve rank for snapshot.");
            return "";
        }
    }

    private Tag resolveActiveTag(PlayerRef playerRef) {
        try {
            if (TagManager.get() == null) {
                return null;
            }
            return TagManager.get().getEquippedTag(playerRef.getUuid());
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to resolve active tag for snapshot.");
            return null;
        }
    }
}