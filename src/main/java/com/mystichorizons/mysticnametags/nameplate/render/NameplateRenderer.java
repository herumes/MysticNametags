package com.mystichorizons.mysticnametags.nameplate.render;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;

import javax.annotation.Nonnull;
import java.util.UUID;

public interface NameplateRenderer {

    @Nonnull
    String getId();

    boolean supports(@Nonnull ResolvedNameplate resolvedNameplate);

    void initialize();

    void shutdown();

    void render(@Nonnull PlayerRef playerRef,
                @Nonnull World world,
                @Nonnull ResolvedNameplate resolvedNameplate);

    void clear(@Nonnull PlayerRef playerRef,
               @Nonnull World world);

    void forget(@Nonnull UUID playerUuid);
}