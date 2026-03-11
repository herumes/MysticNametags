package com.mystichorizons.mysticnametags.nameplate.render.image;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ImageNameplateTracker {

    private static final ImageNameplateTracker INSTANCE = new ImageNameplateTracker();

    private final ConcurrentMap<UUID, UUID> playerToEntity = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> playerToWorld = new ConcurrentHashMap<>();

    private ImageNameplateTracker() {
    }

    @Nonnull
    public static ImageNameplateTracker get() {
        return INSTANCE;
    }

    public void track(@Nonnull UUID playerUuid,
                      @Nonnull UUID entityUuid,
                      @Nonnull UUID worldUuid) {
        playerToEntity.put(playerUuid, entityUuid);
        playerToWorld.put(playerUuid, worldUuid);
    }

    public void untrack(@Nonnull UUID playerUuid) {
        playerToEntity.remove(playerUuid);
        playerToWorld.remove(playerUuid);
    }

    @Nullable
    public UUID getTrackedEntity(@Nonnull UUID playerUuid) {
        return playerToEntity.get(playerUuid);
    }

    @Nullable
    public World resolveWorld(@Nonnull UUID playerUuid) {
        UUID worldUuid = playerToWorld.get(playerUuid);
        if (worldUuid == null) {
            return null;
        }

        for (World world : Universe.get().getWorlds().values()) {
            if (world.getWorldConfig().getUuid().equals(worldUuid)) {
                return world;
            }
        }

        return null;
    }

    @Nonnull
    public Set<UUID> getTrackedPlayerIds() {
        return playerToEntity.keySet();
    }
}