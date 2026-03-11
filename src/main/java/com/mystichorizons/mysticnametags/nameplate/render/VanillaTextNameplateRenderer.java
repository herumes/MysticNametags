package com.mystichorizons.mysticnametags.nameplate.render;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.nameplate.NameplateManager;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class VanillaTextNameplateRenderer implements NameplateRenderer {

    @Nonnull
    @Override
    public String getId() {
        return "vanilla_text";
    }

    @Override
    public boolean supports(@Nonnull ResolvedNameplate resolvedNameplate) {
        return true;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void render(@Nonnull PlayerRef playerRef,
                       @Nonnull World world,
                       @Nonnull ResolvedNameplate resolvedNameplate) {

        Store<EntityStore> store = world.getEntityStore().getStore();
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        NameplateManager.get().apply(
                playerRef.getUuid(),
                store,
                ref,
                resolvedNameplate.getPlainText()
        );
    }

    @Override
    public void clear(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        NameplateManager.get().restore(
                playerRef.getUuid(),
                store,
                ref,
                playerRef.getUsername()
        );
    }

    @Override
    public void forget(@Nonnull UUID playerUuid) {
        NameplateManager.get().forget(playerUuid);
    }
}