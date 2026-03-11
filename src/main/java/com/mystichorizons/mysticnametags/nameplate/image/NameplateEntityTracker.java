package com.mystichorizons.mysticnametags.nameplate.image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for currently spawned image nameplates.
 *
 * One active spawned image plate per player.
 */
public final class NameplateEntityTracker {

    private final Map<UUID, SpawnedNameplateState> byPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerByEntity = new ConcurrentHashMap<>();

    public void put(@Nonnull SpawnedNameplateState state) {
        SpawnedNameplateState previous = byPlayer.put(state.getPlayerUuid(), state);
        if (previous != null) {
            playerByEntity.remove(previous.getEntityUuid());
        }
        playerByEntity.put(state.getEntityUuid(), state.getPlayerUuid());
    }

    @Nullable
    public SpawnedNameplateState getByPlayer(@Nonnull UUID playerUuid) {
        return byPlayer.get(playerUuid);
    }

    @Nullable
    public SpawnedNameplateState removeByPlayer(@Nonnull UUID playerUuid) {
        SpawnedNameplateState removed = byPlayer.remove(playerUuid);
        if (removed != null) {
            playerByEntity.remove(removed.getEntityUuid());
        }
        return removed;
    }

    @Nullable
    public UUID getPlayerByEntity(@Nonnull UUID entityUuid) {
        return playerByEntity.get(entityUuid);
    }

    public boolean hasPlayer(@Nonnull UUID playerUuid) {
        return byPlayer.containsKey(playerUuid);
    }

    public void clear() {
        byPlayer.clear();
        playerByEntity.clear();
    }

    public int size() {
        return byPlayer.size();
    }
}