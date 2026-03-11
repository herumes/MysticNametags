package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.mystichorizons.mysticnametags.nameplate.model.NameplateAppearance;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;
import com.mystichorizons.mysticnametags.nameplate.render.GlyphNameplateRenderer;
import com.mystichorizons.mysticnametags.nameplate.render.ImageNameplateRenderer;
import com.mystichorizons.mysticnametags.nameplate.render.NameplateRenderMode;
import com.mystichorizons.mysticnametags.nameplate.render.NameplateRenderer;
import com.mystichorizons.mysticnametags.nameplate.render.NameplateRendererRegistry;
import com.mystichorizons.mysticnametags.nameplate.render.VanillaTextNameplateRenderer;
import com.mystichorizons.mysticnametags.nameplate.state.NameplateDirtyReason;
import com.mystichorizons.mysticnametags.nameplate.state.PlayerNameplateState;
import com.mystichorizons.mysticnametags.nameplate.state.PlayerNameplateStateStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

public final class NameplateCoordinator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final NameplateRendererRegistry rendererRegistry;
    private final PlayerNameplateStateStore stateStore;

    public NameplateCoordinator() {
        this.rendererRegistry = new NameplateRendererRegistry();
        this.stateStore = PlayerNameplateStateStore.get();

        rendererRegistry.register(NameplateRenderMode.VANILLA_TEXT, new VanillaTextNameplateRenderer());
        rendererRegistry.register(NameplateRenderMode.IMAGE, new ImageNameplateRenderer());
        rendererRegistry.register(NameplateRenderMode.GLYPH, new GlyphNameplateRenderer());
    }

    public void initialize() {
        rendererRegistry.initializeAll();
        LOGGER.at(Level.INFO).log("[MysticNameTags] NameplateCoordinator initialized");
    }

    public void shutdown() {
        rendererRegistry.shutdownAll();
    }

    public void refreshImmediately(@Nonnull PlayerRef playerRef,
                                   @Nonnull ResolvedNameplate resolvedNameplate,
                                   @Nonnull NameplateDirtyReason reason) {

        World world = OnlinePlayerWorldResolver.resolve(playerRef.getUuid());
        if (world == null) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        NameplateRenderMode newMode = determineMode(resolvedNameplate);
        PlayerNameplateState existing = stateStore.get(playerUuid);
        NameplateRenderMode oldMode = existing != null ? existing.getActiveMode() : null;

        if (oldMode != null && oldMode != newMode) {
            clearRenderer(oldMode, playerRef, world);
        }

        NameplateRenderer renderer = rendererRegistry.get(newMode);
        if (renderer == null) {
            LOGGER.at(Level.WARNING)
                    .log("[MysticNameTags] No renderer registered for mode %s", newMode);
            return;
        }

        try {
            renderer.render(playerRef, world, resolvedNameplate);

            PlayerNameplateState refreshed = stateStore.get(playerUuid);
            if (refreshed == null || refreshed.getActiveMode() != newMode) {
                stateStore.upsert(
                        playerUuid,
                        refreshed != null ? refreshed.getSpawnedImageState() : null,
                        resolvedNameplate,
                        newMode
                );
            }

        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t)
                    .log("[MysticNameTags] Failed to refresh nameplate for %s (%s)",
                            playerRef.getUsername(),
                            reason.name());
        }
    }

    public void clear(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID uuid = playerRef.getUuid();
        PlayerNameplateState state = stateStore.get(uuid);

        if (state != null && state.getActiveMode() != null) {
            clearRenderer(state.getActiveMode(), playerRef, world);
        }

        stateStore.remove(uuid);
    }

    private void clearRenderer(@Nonnull NameplateRenderMode mode,
                               @Nonnull PlayerRef playerRef,
                               @Nonnull World world) {
        NameplateRenderer renderer = rendererRegistry.get(mode);
        if (renderer == null) {
            return;
        }

        try {
            renderer.clear(playerRef, world);
            renderer.forget(playerRef.getUuid());
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to clear renderer %s for %s",
                            mode.name(),
                            playerRef.getUsername());
        }
    }

    @Nonnull
    private NameplateRenderMode determineMode(@Nonnull ResolvedNameplate resolvedNameplate) {
        NameplateAppearance appearance = resolvedNameplate.getAppearance();

        if (appearance.isImageMode()) {
            return NameplateRenderMode.IMAGE;
        }

        if (appearance.isGlyphMode()) {
            return NameplateRenderMode.GLYPH;
        }

        return NameplateRenderMode.VANILLA_TEXT;
    }
}