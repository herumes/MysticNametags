package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mystichorizons.mysticnametags.MysticNameTagsPlugin;
import com.mystichorizons.mysticnametags.nameplate.state.NameplateDirtyReason;

import java.util.UUID;

public final class NameplateRefreshRequestService {

    public void markDirty(PlayerRef playerRef, NameplateDirtyReason reason) {
        if (playerRef == null) {
            return;
        }

        NameplateCoordinator coordinator = getCoordinator();
        if (coordinator != null) {
            coordinator.markDirty(playerRef, reason);
        }
    }

    public void markDirty(UUID playerId, NameplateDirtyReason reason) {
        if (playerId == null) {
            return;
        }

        NameplateCoordinator coordinator = getCoordinator();
        if (coordinator != null) {
            coordinator.markDirty(playerId, reason);
        }
    }

    public void remove(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        NameplateCoordinator coordinator = getCoordinator();
        if (coordinator != null) {
            coordinator.remove(playerRef);
        }
    }

    private NameplateCoordinator getCoordinator() {
        MysticNameTagsPlugin plugin = MysticNameTagsPlugin.getInstance();
        return plugin == null ? null : plugin.getNameplateCoordinator();
    }
}