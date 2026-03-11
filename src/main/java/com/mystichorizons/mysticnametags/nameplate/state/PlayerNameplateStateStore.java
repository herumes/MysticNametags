package com.mystichorizons.mysticnametags.nameplate.state;

import com.mystichorizons.mysticnametags.nameplate.image.SpawnedNameplateState;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;
import com.mystichorizons.mysticnametags.nameplate.render.NameplateRenderMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerNameplateStateStore {

    private static final PlayerNameplateStateStore INSTANCE = new PlayerNameplateStateStore();

    private final ConcurrentMap<UUID, PlayerNameplateState> states = new ConcurrentHashMap<>();

    private PlayerNameplateStateStore() {
    }

    @Nonnull
    public static PlayerNameplateStateStore get() {
        return INSTANCE;
    }

    @Nullable
    public PlayerNameplateState get(@Nonnull UUID uuid) {
        return states.get(uuid);
    }

    public void upsert(@Nonnull UUID uuid,
                       @Nullable SpawnedNameplateState imageState,
                       @Nullable ResolvedNameplate resolvedNameplate,
                       @Nullable NameplateRenderMode mode) {
        states.put(uuid, new PlayerNameplateState(imageState, resolvedNameplate, mode));
    }

    public void clearImageState(@Nonnull UUID uuid) {
        states.computeIfPresent(uuid, (ignored, existing) ->
                new PlayerNameplateState(null, existing.getResolvedNameplate(), existing.getActiveMode()));
    }

    public void remove(@Nonnull UUID uuid) {
        states.remove(uuid);
    }
}