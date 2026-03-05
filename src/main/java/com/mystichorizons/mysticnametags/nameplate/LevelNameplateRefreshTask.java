package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.MysticNameTagsPlugin;
import com.mystichorizons.mysticnametags.config.Settings;
import com.mystichorizons.mysticnametags.tags.TagManager;
import com.mystichorizons.mysticnametags.util.ColorFormatter;
import org.zuxaw.plugin.api.RPGLevelingAPI;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

public class LevelNameplateRefreshTask implements Runnable {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void run() {
        // Config + availability guard
        if (!Settings.get().isRpgLevelingNameplatesEnabled()) return;
        if (!MysticNameTagsPlugin.isRpgLevelingAvailable()) return;

        Universe universe = Universe.get();
        if (universe == null) return;

        RPGLevelingAPI api = RPGLevelingAPI.get();
        if (api == null) return;

        TagManager tagManager = TagManager.get();
        if (tagManager == null) return;

        for (World world : universe.getWorlds().values()) {
            if (world == null || !world.isAlive()) continue;
            world.execute(() -> refreshWorld(world, tagManager, api));
        }
    }

    private void refreshWorld(@Nonnull World world,
                              @Nonnull TagManager tagManager,
                              @Nonnull RPGLevelingAPI api) {

        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();

        for (PlayerRef playerRef : world.getPlayerRefs()) {
            if (playerRef == null) continue;

            UUID uuid = playerRef.getUuid();
            if (uuid == null) continue;

            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null || !entityRef.isValid()) continue;

            String baseName = playerRef.getUsername();
            if (baseName == null) baseName = "Player";

            int level = 1;
            try {
                RPGLevelingAPI.PlayerLevelInfo info = api.getPlayerLevelInfo(playerRef, store);
                if (info != null && info.getLevel() > 0) {
                    level = info.getLevel();
                }
            } catch (Throwable t) {
                LOGGER.at(Level.WARNING).log(
                        "[MysticNameTags] Failed to fetch RPG level for " + baseName + " (" + uuid + "), defaulting to 1: " + t
                );
            }

            // Build "rank + name + tag" (colored string), then append level (colored style),
            // and also produce a plain fallback for the vanilla Nameplate component.
            String resolvedColored = tagManager.getColoredFullNameplate(playerRef);

            // You can style this however you want; glyph renderer understands <#RRGGBB> and </>.
            String coloredWithLevel = resolvedColored + " <#AAAAAA>[Lvl. " + level + "]</>";

            // Vanilla nameplate must be plain text
            String plainFallback = ColorFormatter.stripFormatting(coloredWithLevel).trim();

            if (Settings.get().isExperimentalGlyphNameplatesEnabled()) {
                // Keep vanilla simple/consistent (optional)
                NameplateManager.get().apply(uuid, store, entityRef, plainFallback);

                // Render colored glyph line above player
                GlyphNameplateManager.get().apply(uuid, world, store, entityRef, coloredWithLevel);
            } else {
                // Normal: vanilla nameplate only
                NameplateManager.get().apply(uuid, store, entityRef, plainFallback);
            }
        }
    }
}