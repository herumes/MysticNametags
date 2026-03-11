package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

public final class OnlinePlayerWorldResolver {

    private OnlinePlayerWorldResolver() {
    }

    @Nullable
    public static World resolve(UUID playerUuid) {
        for (World world : Universe.get().getWorlds().values()) {
            for (PlayerRef ref : world.getPlayerRefs()) {
                if (ref.getUuid().equals(playerUuid)) {
                    return world;
                }
            }
        }
        return null;
    }
}