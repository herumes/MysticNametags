package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.mystichorizons.mysticnametags.MysticNameTagsPlugin;

import java.util.UUID;
import java.util.logging.Level;

public final class QueuedNameplateRefreshTask implements Runnable {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final PlayerNameplateSnapshotResolver snapshotResolver = new PlayerNameplateSnapshotResolver();

    @Override
    public void run() {
        MysticNameTagsPlugin plugin = MysticNameTagsPlugin.getInstance();
        if (plugin == null) {
            return;
        }

        NameplateCoordinator coordinator = plugin.getNameplateCoordinator();
        if (coordinator == null) {
            return;
        }

        int maxPerRun = Math.max(1,
                plugin.getNameplateBatchSize());

        coordinator.getUpdateScheduler().drain(maxPerRun, playerId -> {
            PlayerRef playerRef = findPlayer(playerId);
            if (playerRef == null) {
                coordinator.getStateStore().remove(playerId);
                return false;
            }

            PlayerNameplateSnapshot snapshot = snapshotResolver.resolve(playerRef);
            coordinator.refreshQueued(playerRef, snapshot);
            return true;
        });
    }

    private PlayerRef findPlayer(UUID playerId) {
        try {
            MysticNameTagsPlugin plugin = MysticNameTagsPlugin.getInstance();
            if (plugin == null) {
                return null;
            }

            Universe universe = plugin.getServer().getUniverse();
            if (universe == null) {
                return null;
            }

            return universe.getPlayer(playerId);
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to resolve online player for queued refresh.");
            return null;
        }
    }
}