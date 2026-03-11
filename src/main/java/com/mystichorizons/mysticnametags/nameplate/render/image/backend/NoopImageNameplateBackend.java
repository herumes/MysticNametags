package com.mystichorizons.mysticnametags.nameplate.render.image.backend;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.nameplate.image.RuntimeNameplateAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class NoopImageNameplateBackend implements ImageNameplateBackend {

    @Nonnull
    @Override
    public ImageBackendSpawnResult spawn(@Nonnull PlayerRef owner,
                                         @Nonnull World world,
                                         @Nonnull Store<EntityStore> store,
                                         @Nonnull Ref<EntityStore> ownerRef,
                                         @Nonnull RuntimeNameplateAsset asset,
                                         boolean billboard) {
        return ImageBackendSpawnResult.failure("Image backend unavailable");
    }

    @Override
    public void despawn(@Nonnull World world,
                        @Nonnull Store<EntityStore> store,
                        @Nullable UUID entityUuid) {
        // no-op
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}