package com.mystichorizons.mysticnametags;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.mystichorizons.mysticnametags.commands.MysticNameTagsPluginCommand;
import com.mystichorizons.mysticnametags.commands.TagsAdminCommand;
import com.mystichorizons.mysticnametags.commands.TagsCommand;
import com.mystichorizons.mysticnametags.commands.TagsOwnedCommand;
import com.mystichorizons.mysticnametags.config.LanguageManager;
import com.mystichorizons.mysticnametags.config.Settings;
import com.mystichorizons.mysticnametags.integrations.IntegrationManager;
import com.mystichorizons.mysticnametags.listeners.PlayerListener;
import com.mystichorizons.mysticnametags.nameplate.LevelNameplateRefreshTask;
import com.mystichorizons.mysticnametags.nameplate.NameplateCoordinator;
import com.mystichorizons.mysticnametags.nameplate.NameplateManager;
import com.mystichorizons.mysticnametags.nameplate.QueuedNameplateRefreshTask;
import com.mystichorizons.mysticnametags.nameplate.render.GlyphNameplateRenderer;
import com.mystichorizons.mysticnametags.nameplate.render.ImageNameplateRenderer;
import com.mystichorizons.mysticnametags.nameplate.render.NameplateRendererRegistry;
import com.mystichorizons.mysticnametags.nameplate.render.VanillaTextNameplateRenderer;
import com.mystichorizons.mysticnametags.placeholders.HelpchPlaceholderHook;
import com.mystichorizons.mysticnametags.placeholders.WiFlowPlaceholderHook;
import com.mystichorizons.mysticnametags.playtime.PlaytimeService;
import com.mystichorizons.mysticnametags.stats.PlayerStatManager;
import com.mystichorizons.mysticnametags.stats.systems.BlockBreakStatSystem;
import com.mystichorizons.mysticnametags.stats.systems.BlockPlaceStatSystem;
import com.mystichorizons.mysticnametags.stats.systems.DamageStatSystem;
import com.mystichorizons.mysticnametags.stats.systems.DeathStatSystem;
import com.mystichorizons.mysticnametags.tags.TagManager;
import com.mystichorizons.mysticnametags.util.MysticLog;
import com.mystichorizons.mysticnametags.util.UpdateChecker;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * MysticNameTags - A Hytale server plugin.
 */
public class MysticNameTagsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static MysticNameTagsPlugin instance;

    private ScheduledExecutorService levelScheduler;
    private ScheduledExecutorService renderScheduler;
    private ScheduledExecutorService queuedRefreshScheduler;

    private IntegrationManager integrations;
    private UpdateChecker updateChecker;
    private PluginManifest manifest;

    private PlaytimeService playtimeService;

    private NameplateRendererRegistry nameplateRendererRegistry;
    private NameplateCoordinator nameplateCoordinator;

    public MysticNameTagsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    /**
     * Get the plugin instance.
     *
     * @return The plugin instance
     */
    public static MysticNameTagsPlugin getInstance() {
        return instance;
    }

    /**
     * Expose integrations to commands / other systems.
     */
    public IntegrationManager getIntegrations() {
        return integrations;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public NameplateRendererRegistry getNameplateRendererRegistry() {
        return nameplateRendererRegistry;
    }

    public NameplateCoordinator getNameplateCoordinator() {
        return nameplateCoordinator;
    }

    /** Shared "resolved version" helper for UI/commands. */
    public String getResolvedVersion() {
        if (manifest != null && manifest.getVersion() != null) {
            return manifest.getVersion().toString();
        }
        return "unknown";
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[MysticNameTags] Setting up...");

        this.manifest = this.getManifest();

        String version = "unknown";
        if (manifest != null && manifest.getVersion() != null) {
            version = manifest.getVersion().toString();
        }

        this.updateChecker = new UpdateChecker(version);
        this.updateChecker.checkForUpdates();

        // ------------------------------------------------------
        // Playtime service
        // ------------------------------------------------------
        this.playtimeService = new PlaytimeService(60L);

        // ------------------------------------------------------
        // Integrations
        // ------------------------------------------------------
        this.integrations = new IntegrationManager(this.playtimeService);

        // ------------------------------------------------------
        // Core config + language
        // ------------------------------------------------------
        Settings.init();
        LanguageManager.init();

        // ------------------------------------------------------
        // Nameplate renderer system
        // ------------------------------------------------------
        this.nameplateRendererRegistry = new NameplateRendererRegistry();
        this.nameplateRendererRegistry.register(new VanillaTextNameplateRenderer());
        this.nameplateRendererRegistry.register(new GlyphNameplateRenderer());
        this.nameplateRendererRegistry.register(new ImageNameplateRenderer());
        this.nameplateRendererRegistry.initializeAll();

        this.nameplateCoordinator = new NameplateCoordinator(this.nameplateRendererRegistry);

        // ------------------------------------------------------
        // Tags + ECS systems + commands + listeners
        // ------------------------------------------------------
        TagManager.init(integrations);

        registerCommands();
        registerListeners();
        registerEcsSystems();

        LOGGER.at(Level.INFO).log("[MysticNameTags] Setup complete!");
    }

    private void registerCommands() {
        try {
            getCommandRegistry().registerCommand(new MysticNameTagsPluginCommand());
            LOGGER.at(Level.INFO).log("[MysticNameTags] Registered /mntags command");

            getCommandRegistry().registerCommand(new TagsCommand());
            LOGGER.at(Level.INFO).log("[MysticNameTags] Registered /tags command");

            getCommandRegistry().registerCommand(new TagsAdminCommand());
            LOGGER.at(Level.INFO).log("[MysticNameTags] Registered /tagsadmin command");

            getCommandRegistry().registerCommand(new TagsOwnedCommand());
            LOGGER.at(Level.INFO).log("[MysticNameTags] Registered /tagsowned command");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING)
                    .withCause(e)
                    .log("[MysticNameTags] Failed to register commands");
        }
    }

    private void registerListeners() {
        EventRegistry eventBus = getEventRegistry();

        try {
            new PlayerListener(playtimeService).register(eventBus);
            LOGGER.at(Level.INFO).log("[MysticNameTags] Registered player event listeners");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING)
                    .withCause(e)
                    .log("[MysticNameTags] Failed to register listeners");
        }
    }

    private void registerEcsSystems() {
        try {
            var ecs = getEntityStoreRegistry();

            ecs.registerSystem(new BlockBreakStatSystem());
            ecs.registerSystem(new BlockPlaceStatSystem());
            ecs.registerSystem(new DamageStatSystem());
            ecs.registerSystem(new DeathStatSystem());

            LOGGER.at(Level.INFO)
                    .log("[MysticNameTags] Registered ECS stat systems (break/place/damage/death).");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING)
                    .withCause(e)
                    .log("[MysticNameTags] Failed to register ECS stat systems");
        }
    }

    @Override
    protected void start() {
        this.integrations.init();

        tryRegisterEndlessLevelingNameplates();

        MysticLog.init(this);

        if (playtimeService != null) {
            playtimeService.start();
        }

        LOGGER.at(Level.INFO).log("[MysticNameTags] Started!");
        LOGGER.at(Level.INFO).log("[MysticNameTags] Use /tags help for commands");

        try {
            new HelpchPlaceholderHook().register();
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING)
                    .log("[MysticNameTags] Failed to register at.helpch PlaceholderAPI expansion. "
                            + "Maybe not installed? Disabled helpch placeholder support.");
        }

        try {
            new WiFlowPlaceholderHook().register();
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING)
                    .log("[MysticNameTags] Failed to register WiFlowPlaceholderAPI expansion. "
                            + "Maybe not installed? Disabled Placeholder Support.");
        }

        try {
            var manager = net.cfh.vault.VaultUnlockedServicesManager.get();
            LOGGER.at(Level.INFO).log("[MysticNameTags][Debug] Startup Vault econ provider names = "
                    + manager.economyProviderNames());
            LOGGER.at(Level.INFO).log("[MysticNameTags][Debug] Startup Vault economyObj() = "
                    + manager.economyObj());
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING)
                    .log("[MysticNameTags][Debug] Error probing VaultUnlocked at startup");
        }

        try {
            boolean enabled = com.eliteessentials.api.EconomyAPI.isEnabled();
            LOGGER.at(Level.INFO).log("[MysticNameTags][Debug] Startup EconomyAPI.isEnabled() = " + enabled);
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING)
                    .log("[MysticNameTags][Debug] EconomyAPI not reachable at startup");
        }

        try {
            boolean primaryAvailable = com.economy.api.EconomyAPI.getInstance() != null;
            LOGGER.at(Level.INFO).log("[MysticNameTags][Debug] EconomySystem API available = " + primaryAvailable);
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING)
                    .log("[MysticNameTags][Debug] EconomySystem API not reachable at startup");
        }

        startLevelSchedulerIfNeeded();
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("[MysticNameTags] Shutting down...");

        try {
            stopLevelScheduler();
        } catch (Throwable ignored) {
            LOGGER.at(Level.WARNING).log("[MysticNameTags] Failed to stop level scheduler");
        }

        try {
            stopQueuedRefreshScheduler();
        } catch (Throwable ignored) {
            LOGGER.at(Level.WARNING).log("[MysticNameTags] Failed to stop queued refresh scheduler");
        }

        try {
            stopRenderScheduler();
        } catch (Throwable ignored) {
            LOGGER.at(Level.WARNING).log("[MysticNameTags] Failed to stop render scheduler");
        }

        try {
            if (playtimeService != null) {
                playtimeService.shutdown();
            }
        } catch (Throwable ignored) {
            LOGGER.at(Level.WARNING).log("[MysticNameTags] Failed to stop PlaytimeService");
        }

        try {
            if (this.nameplateRendererRegistry != null) {
                this.nameplateRendererRegistry.shutdownAll();
            }
        } catch (Throwable ignored) {
            // no-op
        }

        try {
            PlayerStatManager.shutdownGlobal();
        } catch (Throwable ignored) {
            LOGGER.at(Level.WARNING).log("[MysticNameTags] Failed to stop PlayerStatManager");
        }

        try {
            NameplateManager.get().clearAll();
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING)
                    .withCause(t)
                    .log("[MysticNameTags] Error while clearing nameplates during shutdown.");
        } finally {
            MysticLog.shutdown();
            instance = null;
        }
    }

    // ------------------------------------------------------
    // RPGLeveling availability helper
    // ------------------------------------------------------

    public static boolean isRpgLevelingAvailable() {
        try {
            if (!Settings.get().isRpgLevelingNameplatesEnabled()) {
                return false;
            }

            org.zuxaw.plugin.api.RPGLevelingAPI api = org.zuxaw.plugin.api.RPGLevelingAPI.get();
            return api != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------
    // Level scheduler control
    // ------------------------------------------------------

    private void startLevelSchedulerIfNeeded() {
        if (!Settings.get().isRpgLevelingNameplatesEnabled()) {
            LOGGER.at(Level.INFO)
                    .log("[MysticNameTags] RPGLeveling nameplates disabled in settings; not starting scheduler.");
            return;
        }

        int intervalSec = Settings.get().getRpgLevelingRefreshSeconds();

        if (levelScheduler != null && !levelScheduler.isShutdown()) {
            LOGGER.at(Level.FINE)
                    .log("[MysticNameTags] Level scheduler already running; skipping restart.");
            return;
        }

        levelScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MysticNameTags-LevelRefresher");
            t.setDaemon(true);
            return t;
        });

        levelScheduler.scheduleAtFixedRate(
                new LevelNameplateRefreshTask(),
                intervalSec,
                intervalSec,
                TimeUnit.SECONDS
        );

        LOGGER.at(Level.INFO)
                .log("[MysticNameTags] Started RPGLeveling nameplate scheduler (interval=" + intervalSec + "s).");
    }

    private void stopLevelScheduler() {
        if (levelScheduler != null) {
            try {
                levelScheduler.shutdownNow();
            } catch (Throwable ignored) {
            } finally {
                levelScheduler = null;
            }
            LOGGER.at(Level.INFO).log("[MysticNameTags] Stopped RPGLeveling nameplate scheduler.");
        }
    }

    private void startRenderSchedulerIfNeeded() {
        if (renderScheduler != null && !renderScheduler.isShutdown()) {
            return;
        }

        renderScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MysticNameTags-RenderTick");
            t.setDaemon(true);
            return t;
        });

        renderScheduler.scheduleAtFixedRate(() -> {
            try {
                if (this.nameplateCoordinator != null) {
                    this.nameplateCoordinator.tick();
                }
            } catch (Throwable t) {
                LOGGER.at(Level.WARNING).withCause(t)
                        .log("[MysticNameTags] Render scheduler tick failed.");
            }
        }, 50L, 50L, TimeUnit.MILLISECONDS);

        LOGGER.at(Level.INFO).log("[MysticNameTags] Started render scheduler.");
    }

    private void stopRenderScheduler() {
        if (renderScheduler != null) {
            try {
                renderScheduler.shutdownNow();
            } catch (Throwable ignored) {
            } finally {
                renderScheduler = null;
            }
            LOGGER.at(Level.INFO).log("[MysticNameTags] Stopped render scheduler.");
        }
    }

    private void startQueuedRefreshSchedulerIfNeeded() {
        if (queuedRefreshScheduler != null && !queuedRefreshScheduler.isShutdown()) {
            return;
        }

        queuedRefreshScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MysticNameTags-QueuedRefresh");
            t.setDaemon(true);
            return t;
        });

        queuedRefreshScheduler.scheduleAtFixedRate(
                new QueuedNameplateRefreshTask(),
                50L,
                50L,
                TimeUnit.MILLISECONDS
        );

        LOGGER.at(Level.INFO).log("[MysticNameTags] Started queued nameplate refresh scheduler.");
    }

    private void stopQueuedRefreshScheduler() {
        if (queuedRefreshScheduler != null) {
            try {
                queuedRefreshScheduler.shutdownNow();
            } catch (Throwable ignored) {
            } finally {
                queuedRefreshScheduler = null;
            }

            LOGGER.at(Level.INFO).log("[MysticNameTags] Stopped queued nameplate refresh scheduler.");
        }
    }

    // ------------------------------------------------------
    // Reload entrypoint used by /tags reload
    // ------------------------------------------------------

    /**
     * Reloads settings, integrations, tags, and restarts the
     * RPGLeveling scheduler using the latest configuration.
     *
     * Intended to be called from /tags reload.
     */
    public void reloadAll() {
        LOGGER.at(Level.INFO).log("[MysticNameTags] Reloading settings, integrations, and tags...");

        Settings.init();
        LanguageManager.get().reload();

        try {
            this.integrations.init();
            LOGGER.at(Level.INFO).log("[MysticNameTags] Integrations re-initialized after reload.");
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t)
                    .log("[MysticNameTags] Failed to re-initialize integrations during reload.");
        }

        TagManager.reload();

        stopLevelScheduler();
        startLevelSchedulerIfNeeded();

        stopQueuedRefreshScheduler();
        startQueuedRefreshSchedulerIfNeeded();

        stopRenderScheduler();
        startRenderSchedulerIfNeeded();

        if (this.nameplateCoordinator != null) {
            this.nameplateCoordinator.clearAllState();
        }

        LOGGER.at(Level.INFO).log("[MysticNameTags] Reload complete.");
    }

    private void tryRegisterEndlessLevelingNameplates() {
        if (!Settings.get().isEndlessLevelingNameplatesEnabled()) {
            LOGGER.at(Level.INFO).log("[MysticNameTags] EndlessLeveling nameplates disabled in settings.");
            return;
        }

        try {
            Class.forName("com.airijko.endlessleveling.EndlessLeveling");

            com.airijko.endlessleveling.EndlessLeveling el = com.airijko.endlessleveling.EndlessLeveling.getInstance();
            if (el == null) {
                LOGGER.at(Level.INFO).log("[MysticNameTags] EndlessLeveling detected but instance is null; skipping integration.");
                return;
            }

            var pdm = el.getPlayerDataManager();
            if (pdm == null) {
                LOGGER.at(Level.INFO).log("[MysticNameTags] EndlessLeveling detected but PlayerDataManager is null; skipping integration.");
                return;
            }

            this.getEntityStoreRegistry().registerSystem(
                    new com.mystichorizons.mysticnametags.integrations.endlessleveling.EndlessLevelingNameplateSystem(
                            pdm,
                            TagManager.get()
                    )
            );

            LOGGER.at(Level.INFO).log("[MysticNameTags] EndlessLeveling integration enabled: overriding player nameplates.");

        } catch (ClassNotFoundException ignored) {
            // Not installed
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t)
                    .log("[MysticNameTags] Failed to register EndlessLeveling integration.");
        }
    }

    public int getNameplateBatchSize() {
        try {
            return Settings.get().getRendering().getImage().getMaxBatchUpdatesPerTick();
        } catch (Throwable ignored) {
            return 10;
        }
    }
}