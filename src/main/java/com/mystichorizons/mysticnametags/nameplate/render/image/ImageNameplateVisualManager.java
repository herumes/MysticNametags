package com.mystichorizons.mysticnametags.nameplate.render.image;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class ImageNameplateVisualManager {

    // TODO: EVERYTHING NEEDS TO BE FINISHED LOL
    public void applyVisualState(@Nonnull PlayerRef playerRef,
                                 @Nonnull World world,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull Ref<EntityStore> ownerRef,
                                 @Nonnull UUID entityUuid,
                                 @Nonnull ResolvedNameplate resolvedNameplate) {
        // Reserved for future:
        // - visibility tweaks
        // - scale adjustments
        // - stateful visual rules
    }
}