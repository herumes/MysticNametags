package com.mystichorizons.mysticnametags.nameplate.state;

import com.mystichorizons.mysticnametags.nameplate.image.SpawnedNameplateState;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;
import com.mystichorizons.mysticnametags.nameplate.render.NameplateRenderMode;

import javax.annotation.Nullable;

public final class PlayerNameplateState {

    @Nullable
    private final SpawnedNameplateState spawnedImageState;
    @Nullable
    private final ResolvedNameplate resolvedNameplate;
    @Nullable
    private final NameplateRenderMode activeMode;

    public PlayerNameplateState(@Nullable SpawnedNameplateState spawnedImageState,
                                @Nullable ResolvedNameplate resolvedNameplate,
                                @Nullable NameplateRenderMode activeMode) {
        this.spawnedImageState = spawnedImageState;
        this.resolvedNameplate = resolvedNameplate;
        this.activeMode = activeMode;
    }

    @Nullable
    public SpawnedNameplateState getSpawnedImageState() {
        return spawnedImageState;
    }

    @Nullable
    public ResolvedNameplate getResolvedNameplate() {
        return resolvedNameplate;
    }

    @Nullable
    public NameplateRenderMode getActiveMode() {
        return activeMode;
    }

    public PlayerNameplateState withImageState(@Nullable SpawnedNameplateState state,
                                               @Nullable ResolvedNameplate resolved,
                                               @Nullable NameplateRenderMode mode) {
        return new PlayerNameplateState(state, resolved, mode);
    }
}