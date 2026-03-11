package com.mystichorizons.mysticnametags.nameplate.render.image.backend;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.mystichorizons.mysticnametags.nameplate.image.RuntimeNameplateAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public interface ImageNameplateBackend {

    @Nonnull
    ImageBackendSpawnResult spawn(@Nonnull PlayerRef owner,
                                  @Nonnull World world,
                                  @Nonnull Store<EntityStore> store,
                                  @Nonnull Ref<EntityStore> ownerRef,
                                  @Nonnull RuntimeNameplateAsset asset,
                                  boolean billboard);

    void despawn(@Nonnull World world,
                 @Nonnull Store<EntityStore> store,
                 @Nullable UUID entityUuid);

    boolean isAvailable();
}