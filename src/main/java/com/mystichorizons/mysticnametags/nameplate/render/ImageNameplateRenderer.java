package com.mystichorizons.mysticnametags.nameplate.render;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.config.Settings;
import com.mystichorizons.mysticnametags.nameplate.NameplateTextResolver;
import com.mystichorizons.mysticnametags.nameplate.image.NameplateAssetIds;
import com.mystichorizons.mysticnametags.nameplate.image.NameplateHashing;
import com.mystichorizons.mysticnametags.nameplate.image.RenderedNameplateImage;
import com.mystichorizons.mysticnametags.nameplate.image.RuntimeNameplateAsset;
import com.mystichorizons.mysticnametags.nameplate.image.SpawnedNameplateState;
import com.mystichorizons.mysticnametags.nameplate.model.NameplateAppearance;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;
import com.mystichorizons.mysticnametags.nameplate.render.image.ImageNameplateAnchorManager;
import com.mystichorizons.mysticnametags.nameplate.render.image.ImageNameplatePayloadFactory;
import com.mystichorizons.mysticnametags.nameplate.render.image.ImageNameplateRenderPayload;
import com.mystichorizons.mysticnametags.nameplate.render.image.ImageNameplateTracker;
import com.mystichorizons.mysticnametags.nameplate.render.image.ImageNameplateVisualManager;
import com.mystichorizons.mysticnametags.nameplate.render.image.backend.HytaleImageNameplateBackend;
import com.mystichorizons.mysticnametags.nameplate.render.image.backend.ImageBackendSpawnResult;
import com.mystichorizons.mysticnametags.nameplate.render.image.backend.ImageNameplateBackend;
import com.mystichorizons.mysticnametags.nameplate.state.PlayerNameplateState;
import com.mystichorizons.mysticnametags.nameplate.state.PlayerNameplateStateStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class ImageNameplateRenderer implements NameplateRenderer {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ImageNameplatePayloadFactory payloadFactory;
    private final NameplateAssetIds assetIds;
    private final ImageNameplateBackend backend;
    private final PlayerNameplateStateStore stateStore;
    private final ImageNameplateTracker tracker;
    private final ImageNameplateVisualManager visualManager;
    private final ImageNameplateAnchorManager anchorManager;

    public ImageNameplateRenderer() {
        this.payloadFactory = new ImageNameplatePayloadFactory();
        this.assetIds = new NameplateAssetIds();
        this.backend = new HytaleImageNameplateBackend();
        this.stateStore = PlayerNameplateStateStore.get();
        this.tracker = ImageNameplateTracker.get();
        this.visualManager = new ImageNameplateVisualManager();
        this.anchorManager = new ImageNameplateAnchorManager();
    }

    @Nonnull
    @Override
    public String getId() {
        return "image";
    }

    @Override
    public boolean supports(@Nonnull ResolvedNameplate resolvedNameplate) {
        if (!Settings.get().isNameplatesEnabled()) {
            return false;
        }

        if (!Settings.get().isImageNameplatesEnabled()) {
            return false;
        }

        return backend.isAvailable() && resolvedNameplate.getAppearance().isImageMode();
    }

    @Override
    public void initialize() {
        LOGGER.at(Level.INFO).log("[MysticNameTags] ImageNameplateRenderer initialized");
    }

    @Override
    public void shutdown() {
        try {
            for (UUID uuid : tracker.getTrackedPlayerIds()) {
                forget(uuid);
            }
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Error during ImageNameplateRenderer shutdown");
        }
    }

    @Override
    public void render(@Nonnull PlayerRef playerRef,
                       @Nonnull World world,
                       @Nonnull ResolvedNameplate resolvedNameplate) {

        UUID playerUuid = playerRef.getUuid();

        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> ownerRef = playerRef.getReference();

            if (ownerRef == null || !ownerRef.isValid()) {
                forget(playerUuid);
                return;
            }

            ImageNameplateRenderPayload payload = payloadFactory.create(playerRef, resolvedNameplate);
            if (payload == null) {
                forget(playerUuid);
                return;
            }

            RenderedNameplateImage rendered = payload.getRenderedImage();
            if (rendered == null) {
                forget(playerUuid);
                return;
            }

            RuntimeNameplateAsset runtimeAsset = payload.getRuntimeAsset();
            if (runtimeAsset == null) {
                forget(playerUuid);
                return;
            }

            boolean billboard = resolvedNameplate.getAppearance().isBillboard();

            PlayerNameplateState previousState = stateStore.get(playerUuid);
            SpawnedNameplateState previousSpawn = previousState != null ? previousState.getSpawnedImageState() : null;

            boolean needsRespawn = shouldRespawn(previousSpawn, runtimeAsset, billboard);

            UUID entityUuid;
            if (needsRespawn) {
                if (previousSpawn != null && previousSpawn.getEntityUuid() != null) {
                    backend.despawn(world, store, previousSpawn.getEntityUuid());
                }

                ImageBackendSpawnResult result = backend.spawn(
                        playerRef,
                        world,
                        store,
                        ownerRef,
                        runtimeAsset,
                        billboard
                );

                if (!result.isSuccess() || result.getEntityUuid() == null) {
                    LOGGER.at(Level.WARNING)
                            .log("[MysticNameTags] Failed to spawn image nameplate for %s: %s",
                                    playerRef.getUsername(),
                                    result.getFailureReason());
                    forget(playerUuid);
                    return;
                }

                entityUuid = result.getEntityUuid();
            } else {
                entityUuid = previousSpawn.getEntityUuid();
            }

            SpawnedNameplateState newSpawnState = new SpawnedNameplateState(
                    entityUuid,
                    runtimeAsset.getKey(),
                    runtimeAsset.getModelAssetId(),
                    runtimeAsset.getBillboardModelAssetId(),
                    rendered.getWidth(),
                    rendered.getHeight(),
                    billboard
            );

            stateStore.upsert(
                    playerUuid,
                    newSpawnState,
                    resolvedNameplate,
                    NameplateRenderMode.IMAGE
            );

            tracker.track(playerUuid, entityUuid, world.getWorldConfig().getUuid());

            visualManager.applyVisualState(
                    playerRef,
                    world,
                    store,
                    ownerRef,
                    entityUuid,
                    resolvedNameplate
            );

            anchorManager.updateAnchor(
                    playerRef,
                    world,
                    store,
                    ownerRef,
                    entityUuid,
                    resolvedNameplate
            );

        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t)
                    .log("[MysticNameTags] ImageNameplateRenderer render failure for %s", playerRef.getUsername());
            forget(playerUuid);
        }
    }

    @Override
    public void clear(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        forget(playerRef.getUuid());
    }

    @Override
    public void forget(@Nonnull UUID playerUuid) {
        PlayerNameplateState state = stateStore.get(playerUuid);
        if (state != null) {
            SpawnedNameplateState spawned = state.getSpawnedImageState();
            if (spawned != null && spawned.getEntityUuid() != null) {
                World world = tracker.resolveWorld(playerUuid);
                if (world != null) {
                    try {
                        Store<EntityStore> store = world.getEntityStore().getStore();
                        backend.despawn(world, store, spawned.getEntityUuid());
                    } catch (Throwable t) {
                        LOGGER.at(Level.FINE).withCause(t)
                                .log("[MysticNameTags] Failed to despawn forgotten image nameplate for %s", playerUuid);
                    }
                }
            }
        }

        tracker.untrack(playerUuid);
        stateStore.clearImageState(playerUuid);
    }

    private boolean shouldRespawn(@Nullable SpawnedNameplateState previous,
                                  @Nonnull RuntimeNameplateAsset runtimeAsset,
                                  boolean billboard) {
        if (previous == null) {
            return true;
        }

        if (previous.getEntityUuid() == null) {
            return true;
        }

        if (previous.isBillboard() != billboard) {
            return true;
        }

        if (!Objects.equals(previous.getAssetKey(), runtimeAsset.getKey())) {
            return true;
        }

        if (billboard) {
            return !Objects.equals(previous.getBillboardModelAssetId(), runtimeAsset.getBillboardModelAssetId());
        }

        return !Objects.equals(previous.getModelAssetId(), runtimeAsset.getModelAssetId());
    }
}