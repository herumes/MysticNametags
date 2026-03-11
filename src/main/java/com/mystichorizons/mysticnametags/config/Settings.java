package com.mystichorizons.mysticnametags.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import com.mystichorizons.mysticnametags.MysticNameTagsPlugin;
import com.mystichorizons.mysticnametags.config.render.RenderingSettings;
import com.mystichorizons.mysticnametags.util.ColorFormatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class Settings {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static Settings INSTANCE;

    // ---------------------------------------------------------------------
    // Core Settings
    // ---------------------------------------------------------------------

    private String nameplateFormat = "{rank} {name} {tag}";
    private boolean stripExtraSpaces = true;
    private String language = "en_US";

    /**
     * Delay in seconds before a player can equip a different tag again.
     * 0 = no cooldown.
     */
    private int tagDelaysecs = 20;

    // ---------------------------------------------------------------------
    // Storage backend (FILE / SQLITE / MYSQL)
    // ---------------------------------------------------------------------

    private String storageBackend = "FILE";
    private String sqliteFile = "playerdata.db";

    private String mysqlHost = "localhost";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "mysticnametags";
    private String mysqlUser = "root";
    private String mysqlPassword = "password";

    // ---------------------------------------------------------------------
    // Playtime
    // ---------------------------------------------------------------------

    private String playtimeProvider = "AUTO"; // AUTO, INTERNAL, ZIB_PLAYTIME, NONE

    // ---------------------------------------------------------------------
    // Nameplate toggles
    // ---------------------------------------------------------------------

    /** Master toggle for MysticNameTags nameplates. */
    private boolean nameplatesEnabled = true;

    /** If true, use a default tag when player has no equipped tag. */
    private boolean defaultTagEnabled = false;

    /** Tag id from tags.json to use as default. */
    private String defaultTagId = "mystic";

    // ---------------------------------------------------------------------
    // EndlessLeveling integration
    // ---------------------------------------------------------------------

    private boolean endlessLevelingNameplatesEnabled = false;
    private boolean endlessRaceDisplay = false;
    private boolean endlessPrestigeDisplay = false;
    private String endlessPrestigePrefix = "P";

    // ---------------------------------------------------------------------
    // Placeholder toggles
    // ---------------------------------------------------------------------

    /**
     * If true, MysticNameTags will run WiFlowPlaceholderAPI on the built nameplate text.
     * If auto-detect is enabled, this value may be overwritten by detection.
     */
    private boolean wiFlowPlaceholdersEnabled = false;

    /**
     * If true, MysticNameTags will run at.helpch.placeholderapi on the built nameplate text.
     * If auto-detect is enabled, this value may be overwritten by detection.
     */
    private boolean helpchPlaceholderApiEnabled = false;

    /**
     * If true, plugin will auto-detect WiFlowPlaceholderAPI presence and toggle wiFlowPlaceholdersEnabled.
     * If false, wiFlowPlaceholdersEnabled is treated as an explicit admin override.
     */
    private boolean wiFlowPlaceholdersAutoDetect = true;

    /**
     * If true, plugin will auto-detect at.helpch PlaceholderAPI presence and toggle helpchPlaceholderApiEnabled.
     * If false, helpchPlaceholderApiEnabled is treated as an explicit admin override.
     */
    private boolean helpchPlaceholderApiAutoDetect = true;

    // ---------------------------------------------------------------------
    // Economy / permission / RPG flags
    // ---------------------------------------------------------------------

    private boolean economySystemEnabled = true;
    private boolean useCoinSystem = false;
    private boolean usePhysicalCoinEconomy = false;
    private boolean fullPermissionGate = false;
    private boolean permissionGate = false;

    private boolean rpgLevelingNameplatesEnabled = false;
    private int rpgLevelingRefreshSeconds = 30;

    // ---------------------------------------------------------------------
    // Commands / features
    // ---------------------------------------------------------------------

    /**
     * If true, enables the owned tags command/UI.
     * Nullable for backward compatibility.
     */
    private Boolean ownedTagsCommandEnabled = Boolean.TRUE;

    // ---------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------

    private RenderingSettings rendering = new RenderingSettings();

    // ---------------------------------------------------------------------
    // Non-serialized state
    // ---------------------------------------------------------------------

    private transient boolean dirty = false;

    public static void init() {
        INSTANCE = new Settings();
        INSTANCE.load();
    }

    public static Settings get() {
        return INSTANCE;
    }

    private File getFile() {
        File data = MysticNameTagsPlugin.getInstance().getDataDirectory().toFile();
        return new File(data, "settings.json");
    }

    private void load() {
        File file = getFile();

        if (!file.exists()) {
            applyAutoPlaceholderDetection(true);
            dirty = true;
            saveIfDirty();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Settings loaded = GSON.fromJson(reader, Settings.class);
            if (loaded != null) {
                // -----------------------------------------------------------------
                // Core
                // -----------------------------------------------------------------
                this.nameplateFormat = nonBlankOr(loaded.nameplateFormat, this.nameplateFormat);
                this.stripExtraSpaces = loaded.stripExtraSpaces;
                this.language = nonBlankOr(loaded.language, this.language);
                this.tagDelaysecs = Math.max(0, loaded.tagDelaysecs);

                // -----------------------------------------------------------------
                // Storage
                // -----------------------------------------------------------------
                this.storageBackend = nonBlankOr(loaded.storageBackend, this.storageBackend);
                this.sqliteFile = nonBlankOr(loaded.sqliteFile, this.sqliteFile);

                this.mysqlHost = nonBlankOr(loaded.mysqlHost, this.mysqlHost);
                this.mysqlPort = (loaded.mysqlPort <= 0 ? this.mysqlPort : loaded.mysqlPort);
                this.mysqlDatabase = nonBlankOr(loaded.mysqlDatabase, this.mysqlDatabase);
                this.mysqlUser = nonBlankOr(loaded.mysqlUser, this.mysqlUser);
                this.mysqlPassword = (loaded.mysqlPassword == null ? this.mysqlPassword : loaded.mysqlPassword);

                // -----------------------------------------------------------------
                // Playtime
                // -----------------------------------------------------------------
                this.playtimeProvider = nonBlankOr(loaded.playtimeProvider, this.playtimeProvider);

                // -----------------------------------------------------------------
                // Nameplates
                // -----------------------------------------------------------------
                this.nameplatesEnabled = loaded.nameplatesEnabled;
                this.defaultTagEnabled = loaded.defaultTagEnabled;
                this.defaultTagId = nonBlankOr(loaded.defaultTagId, this.defaultTagId);

                // -----------------------------------------------------------------
                // EndlessLeveling
                // -----------------------------------------------------------------
                this.endlessLevelingNameplatesEnabled = loaded.endlessLevelingNameplatesEnabled;
                this.endlessRaceDisplay = loaded.endlessRaceDisplay;
                this.endlessPrestigeDisplay = loaded.endlessPrestigeDisplay;
                this.endlessPrestigePrefix = nonBlankOr(loaded.endlessPrestigePrefix, this.endlessPrestigePrefix);

                // -----------------------------------------------------------------
                // Placeholder toggles
                // -----------------------------------------------------------------
                this.wiFlowPlaceholdersEnabled = loaded.wiFlowPlaceholdersEnabled;
                this.helpchPlaceholderApiEnabled = loaded.helpchPlaceholderApiEnabled;

                this.wiFlowPlaceholdersAutoDetect = loaded.wiFlowPlaceholdersAutoDetect;
                this.helpchPlaceholderApiAutoDetect = loaded.helpchPlaceholderApiAutoDetect;

                // For older configs where these keys do not exist, default them to true.
                if (!hasKeyInJson(file, "wiFlowPlaceholdersAutoDetect")) {
                    this.wiFlowPlaceholdersAutoDetect = true;
                    dirty = true;
                }
                if (!hasKeyInJson(file, "helpchPlaceholderApiAutoDetect")) {
                    this.helpchPlaceholderApiAutoDetect = true;
                    dirty = true;
                }

                // -----------------------------------------------------------------
                // Economy / permissions / RPG
                // -----------------------------------------------------------------
                this.economySystemEnabled = loaded.economySystemEnabled;
                this.useCoinSystem = loaded.useCoinSystem;
                this.usePhysicalCoinEconomy = loaded.usePhysicalCoinEconomy;
                this.fullPermissionGate = loaded.fullPermissionGate;
                this.permissionGate = loaded.permissionGate;

                this.rpgLevelingNameplatesEnabled = loaded.rpgLevelingNameplatesEnabled;
                this.rpgLevelingRefreshSeconds = loaded.rpgLevelingRefreshSeconds;

                // -----------------------------------------------------------------
                // Commands
                // -----------------------------------------------------------------
                this.ownedTagsCommandEnabled = loaded.ownedTagsCommandEnabled;

                // -----------------------------------------------------------------
                // Rendering
                // -----------------------------------------------------------------
                if (loaded.rendering != null) {
                    this.rendering = loaded.rendering;
                } else {
                    this.rendering = new RenderingSettings();
                    dirty = true;
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e)
                    .log("[MysticNameTags] Failed to load settings.json, using defaults.");
            dirty = true;
        }

        applyAutoPlaceholderDetection(false);
        normalizeAndClamp();
        saveToDisk();
        dirty = false;
    }

    private void normalizeAndClamp() {
        String before;

        // ---------------------------------------------------------------------
        // Normalize strings
        // ---------------------------------------------------------------------

        before = this.storageBackend;
        this.storageBackend = getStorageBackendRaw();
        if (!safeEquals(before, this.storageBackend)) dirty = true;

        before = this.sqliteFile;
        this.sqliteFile = getSqliteFile();
        if (!safeEquals(before, this.sqliteFile)) dirty = true;

        before = this.mysqlHost;
        this.mysqlHost = getMysqlHost();
        if (!safeEquals(before, this.mysqlHost)) dirty = true;

        int oldPort = this.mysqlPort;
        this.mysqlPort = getMysqlPort();
        if (oldPort != this.mysqlPort) dirty = true;

        before = this.mysqlDatabase;
        this.mysqlDatabase = getMysqlDatabase();
        if (!safeEquals(before, this.mysqlDatabase)) dirty = true;

        before = this.defaultTagId;
        this.defaultTagId = (this.defaultTagId == null ? "mystic" : this.defaultTagId.trim());
        if (!safeEquals(before, this.defaultTagId)) dirty = true;

        before = this.endlessPrestigePrefix;
        this.endlessPrestigePrefix = nonBlankOr(this.endlessPrestigePrefix, "P");
        if (!safeEquals(before, this.endlessPrestigePrefix)) dirty = true;

        // ---------------------------------------------------------------------
        // Clamp primitives
        // ---------------------------------------------------------------------

        int oldDelay = this.tagDelaysecs;
        this.tagDelaysecs = Math.max(0, this.tagDelaysecs);
        if (oldDelay != this.tagDelaysecs) dirty = true;

        int oldRpg = this.rpgLevelingRefreshSeconds;
        this.rpgLevelingRefreshSeconds = Math.max(5, this.rpgLevelingRefreshSeconds);
        if (oldRpg != this.rpgLevelingRefreshSeconds) dirty = true;

        // ---------------------------------------------------------------------
        // Rendering
        // ---------------------------------------------------------------------

        if (this.rendering == null) {
            this.rendering = new RenderingSettings();
            dirty = true;
        }

        this.rendering.getGlyph().setMaxChars(this.rendering.getGlyph().getMaxChars());
        this.rendering.getGlyph().setUpdateTicks(this.rendering.getGlyph().getUpdateTicks());
        this.rendering.getGlyph().setRenderDistance(this.rendering.getGlyph().getRenderDistance());
        this.rendering.getGlyph().setMaxEntitiesPerPlayer(this.rendering.getGlyph().getMaxEntitiesPerPlayer());

        this.rendering.getImage().setMinTicksBetweenUpdates(this.rendering.getImage().getMinTicksBetweenUpdates());
        this.rendering.getImage().setPlaceholderRefreshTicks(this.rendering.getImage().getPlaceholderRefreshTicks());
        this.rendering.getImage().setMovementRefreshTicks(this.rendering.getImage().getMovementRefreshTicks());
        this.rendering.getImage().setMaxBatchUpdatesPerTick(this.rendering.getImage().getMaxBatchUpdatesPerTick());
        this.rendering.getImage().setRenderDistance(this.rendering.getImage().getRenderDistance());
        this.rendering.getImage().setVerticalOffset(this.rendering.getImage().getVerticalOffset());
        this.rendering.getImage().setMaxCachedLayouts(this.rendering.getImage().getMaxCachedLayouts());

        this.rendering.getImage().getLayout().setMaxLines(this.rendering.getImage().getLayout().getMaxLines());
        this.rendering.getImage().getLayout().setPaddingX(this.rendering.getImage().getLayout().getPaddingX());
        this.rendering.getImage().getLayout().setPaddingY(this.rendering.getImage().getLayout().getPaddingY());
        this.rendering.getImage().getLayout().setLineSpacing(this.rendering.getImage().getLayout().getLineSpacing());
        this.rendering.getImage().getLayout().setMinWidth(this.rendering.getImage().getLayout().getMinWidth());
        this.rendering.getImage().getLayout().setMaxWidth(this.rendering.getImage().getLayout().getMaxWidth());
        this.rendering.getImage().getLayout().setIconSize(this.rendering.getImage().getLayout().getIconSize());
    }

    private void saveIfDirty() {
        if (!dirty) {
            return;
        }
        saveToDisk();
        dirty = false;
    }

    /**
     * Writes the current in-memory settings to settings.json with ordered sections
     * and human-readable pseudo-comment keys.
     */
    private void saveToDisk() {
        File file = getFile();
        try (FileWriter writer = new FileWriter(file)) {
            JsonObject root = GSON.toJsonTree(this).getAsJsonObject();
            JsonObject out = new JsonObject();

            Set<String> copied = new HashSet<>();

            Consumer<String> copy = (key) -> {
                if (root.has(key)) {
                    out.add(key, root.get(key));
                    copied.add(key);
                }
            };

            out.addProperty("_", "MysticNameTags settings.json");
            out.addProperty("__info_1", "Edit this file, then reload or restart the plugin.");
            out.addProperty("__info_2", "Keys starting with underscores are informational only and are ignored by the plugin.");

            // ------------------------------------------------------------------
            // Core
            // ------------------------------------------------------------------
            out.addProperty("__core", "=== Core Settings ===");
            out.addProperty("__core_1", "nameplateFormat supports: {rank}, {name}, {tag}");
            out.addProperty("__core_2", "stripExtraSpaces condenses repeated spaces.");
            out.addProperty("__core_3", "language is the locale bundle, for example en_US.");
            out.addProperty("__core_4", "tagDelaysecs is the cooldown before equipping a different tag again.");
            copy.accept("nameplateFormat");
            copy.accept("stripExtraSpaces");
            copy.accept("language");
            copy.accept("tagDelaysecs");

            // ------------------------------------------------------------------
            // Storage
            // ------------------------------------------------------------------
            out.addProperty("__storage", "=== Storage Settings ===");
            out.addProperty("__storage_1", "storageBackend controls player/tag ownership storage.");
            out.addProperty("__storage_2", "Valid values: FILE, SQLITE, MYSQL.");
            out.addProperty("__storage_3", "sqliteFile is relative to the plugin data folder.");
            out.addProperty("__storage_4", "MySQL settings are only used when storageBackend is MYSQL.");
            copy.accept("storageBackend");
            copy.accept("sqliteFile");
            copy.accept("mysqlHost");
            copy.accept("mysqlPort");
            copy.accept("mysqlDatabase");
            copy.accept("mysqlUser");
            copy.accept("mysqlPassword");

            // ------------------------------------------------------------------
            // Nameplates
            // ------------------------------------------------------------------
            out.addProperty("__nameplates", "=== Nameplate Settings ===");
            out.addProperty("__nameplates_1", "nameplatesEnabled is the master toggle for MysticNameTags nameplates.");
            out.addProperty("__nameplates_2", "defaultTagEnabled applies defaultTagId when no tag is equipped.");
            out.addProperty("__nameplates_3", "defaultTagId must match a valid id from tags.json.");
            copy.accept("nameplatesEnabled");
            copy.accept("defaultTagEnabled");
            copy.accept("defaultTagId");

            // ------------------------------------------------------------------
            // EndlessLeveling
            // ------------------------------------------------------------------
            out.addProperty("__endless", "=== EndlessLeveling Integration ===");
            out.addProperty("__endless_1", "These settings control optional EndlessLeveling display features.");
            out.addProperty("__endless_2", "endlessPrestigePrefix is used before the prestige number, for example P3.");
            copy.accept("endlessLevelingNameplatesEnabled");
            copy.accept("endlessRaceDisplay");
            copy.accept("endlessPrestigeDisplay");
            copy.accept("endlessPrestigePrefix");

            // ------------------------------------------------------------------
            // Placeholders
            // ------------------------------------------------------------------
            out.addProperty("__placeholders", "=== Placeholder Settings ===");
            out.addProperty("__placeholders_1", "Auto-detect flags allow the plugin to enable or disable support automatically.");
            out.addProperty("__placeholders_2", "If auto-detect is false, the enabled toggle acts as a manual override.");
            copy.accept("wiFlowPlaceholdersAutoDetect");
            copy.accept("wiFlowPlaceholdersEnabled");
            copy.accept("helpchPlaceholderApiAutoDetect");
            copy.accept("helpchPlaceholderApiEnabled");

            // ------------------------------------------------------------------
            // Economy / permissions
            // ------------------------------------------------------------------
            out.addProperty("__economy", "=== Economy And Permission Settings ===");
            out.addProperty("__economy_1", "economySystemEnabled controls paid tag support.");
            out.addProperty("__economy_2", "fullPermissionGate fully gates tags by permission.");
            out.addProperty("__economy_3", "permissionGate keeps tags visible but requires permission to unlock or equip.");
            copy.accept("economySystemEnabled");
            copy.accept("useCoinSystem");
            copy.accept("usePhysicalCoinEconomy");
            copy.accept("fullPermissionGate");
            copy.accept("permissionGate");

            // ------------------------------------------------------------------
            // RPGLeveling
            // ------------------------------------------------------------------
            out.addProperty("__rpg", "=== RPGLeveling Integration ===");
            out.addProperty("__rpg_1", "rpgLevelingRefreshSeconds controls how often RPGLeveling nameplates refresh.");
            copy.accept("rpgLevelingNameplatesEnabled");
            copy.accept("rpgLevelingRefreshSeconds");

            // ------------------------------------------------------------------
            // Playtime + commands
            // ------------------------------------------------------------------
            out.addProperty("__playtime", "=== Playtime And Commands ===");
            out.addProperty("__playtime_1", "playtimeProvider valid values: AUTO, INTERNAL, ZIB_PLAYTIME, NONE.");
            out.addProperty("__playtime_2", "ownedTagsCommandEnabled controls the owned tags command/UI.");
            copy.accept("playtimeProvider");
            copy.accept("ownedTagsCommandEnabled");

            // ------------------------------------------------------------------
            // Rendering
            // ------------------------------------------------------------------
            out.addProperty("__rendering", "=== Rendering Settings ===");
            out.addProperty("__rendering_1", "rendering.mode controls the active renderer.");
            out.addProperty("__rendering_2", "Valid values: VANILLA_TEXT, GLYPH, IMAGE.");
            out.addProperty("__rendering_3", "rendering.glyph contains glyph renderer settings.");
            out.addProperty("__rendering_4", "rendering.image contains image-nameplate renderer settings.");
            copy.accept("rendering");

            // ------------------------------------------------------------------
            // Preserve unknown / future fields
            // ------------------------------------------------------------------
            JsonObject other = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("_")) continue;
                if (copied.contains(key)) continue;
                other.add(key, entry.getValue());
            }

            if (!other.entrySet().isEmpty()) {
                out.addProperty("__other", "=== Other / Future Settings ===");
                for (Map.Entry<String, JsonElement> e : other.entrySet()) {
                    out.add(e.getKey(), e.getValue());
                }
            }

            GSON.toJson(out, writer);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e)
                    .log("[MysticNameTags] Failed to save settings.json");
        }
    }

    // ---------------------------------------------------------------------
    // Auto-detection for placeholder backends
    // ---------------------------------------------------------------------

    private void applyAutoPlaceholderDetection(boolean firstRun) {
        boolean wiFlowPresent = classPresent("com.wiflow.placeholderapi.WiFlowPlaceholderAPI");
        boolean helpchPresent = classPresent("at.helpch.placeholderapi.PlaceholderAPI");

        if (firstRun || this.wiFlowPlaceholdersAutoDetect) {
            if (this.wiFlowPlaceholdersEnabled != wiFlowPresent) {
                this.wiFlowPlaceholdersEnabled = wiFlowPresent;
                dirty = true;
            }
        }

        if (firstRun || this.helpchPlaceholderApiAutoDetect) {
            if (this.helpchPlaceholderApiEnabled != helpchPresent) {
                this.helpchPlaceholderApiEnabled = helpchPresent;
                dirty = true;
            }
        }

        LOGGER.at(Level.INFO).log("[MysticNameTags] WiFlowPlaceholderAPI present=" + wiFlowPresent
                + " | enabled=" + wiFlowPlaceholdersEnabled
                + " | autoDetect=" + wiFlowPlaceholdersAutoDetect);

        LOGGER.at(Level.INFO).log("[MysticNameTags] at.helpch PlaceholderAPI present=" + helpchPresent
                + " | enabled=" + helpchPlaceholderApiEnabled
                + " | autoDetect=" + helpchPlaceholderApiAutoDetect);
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Detect if a JSON root key exists without fully modeling it.
     * Used to treat missing new fields as "default true" behavior.
     */
    private static boolean hasKeyInJson(File file, String key) {
        try (FileReader r = new FileReader(file)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            return obj != null && obj.has(key);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean safeEquals(@Nullable Object a, @Nullable Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    private static String nonBlankOr(@Nullable String value, @Nonnull String fallback) {
        if (value == null) return fallback;
        String t = value.trim();
        return t.isEmpty() ? fallback : t;
    }

    // ---------------------------------------------------------------------
    // Nameplate formatting
    // ---------------------------------------------------------------------

    public String formatNameplateRaw(String rank, String name, String tag) {
        String result = nameplateFormat
                .replace("{rank}", rank == null ? "" : rank)
                .replace("{name}", name == null ? "" : name)
                .replace("{tag}", tag == null ? "" : tag);

        if (stripExtraSpaces) {
            result = result.replaceAll("\\s+", " ").trim();
        }

        return result;
    }

    public String formatNameplate(String rank, String name, String tag) {
        String raw = formatNameplateRaw(rank, name, tag);
        return ColorFormatter.colorize(raw);
    }

    // ---------------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------------

    public String getStorageBackendRaw() {
        return (storageBackend == null || storageBackend.isBlank())
                ? "FILE"
                : storageBackend.trim().toUpperCase();
    }

    public String getSqliteFile() {
        return (sqliteFile == null || sqliteFile.isBlank())
                ? "playerdata.db"
                : sqliteFile.trim();
    }

    public String getMysqlHost() {
        return mysqlHost == null ? "localhost" : mysqlHost.trim();
    }

    public int getMysqlPort() {
        return mysqlPort <= 0 ? 3306 : mysqlPort;
    }

    public String getMysqlDatabase() {
        return (mysqlDatabase == null || mysqlDatabase.isBlank())
                ? "mysticnametags"
                : mysqlDatabase.trim();
    }

    public String getMysqlUser() {
        return mysqlUser == null ? "root" : mysqlUser;
    }

    public String getMysqlPassword() {
        return mysqlPassword == null ? "" : mysqlPassword;
    }

    public boolean isEconomySystemEnabled() {
        return economySystemEnabled;
    }

    public boolean isUseCoinSystem() {
        return useCoinSystem;
    }

    public boolean isUsePhysicalCoinEconomy() {
        return usePhysicalCoinEconomy;
    }

    public boolean isFullPermissionGateEnabled() {
        return fullPermissionGate;
    }

    public boolean isPermissionGateEnabled() {
        return permissionGate;
    }

    public boolean isRpgLevelingNameplatesEnabled() {
        return rpgLevelingNameplatesEnabled;
    }

    public int getRpgLevelingRefreshSeconds() {
        return Math.max(5, rpgLevelingRefreshSeconds);
    }

    public boolean isWiFlowPlaceholdersEnabled() {
        return wiFlowPlaceholdersEnabled;
    }

    public boolean isHelpchPlaceholderApiEnabled() {
        return helpchPlaceholderApiEnabled;
    }

    public boolean isNameplatesEnabled() {
        return nameplatesEnabled;
    }

    public boolean isDefaultTagEnabled() {
        return defaultTagEnabled;
    }

    public String getDefaultTagId() {
        return defaultTagId == null ? "mystic" : defaultTagId.trim();
    }

    public boolean isEndlessLevelingNameplatesEnabled() {
        return endlessLevelingNameplatesEnabled;
    }

    public boolean isEndlessRaceDisplayEnabled() {
        return endlessRaceDisplay;
    }

    public boolean isEndlessPrestigeDisplayEnabled() {
        return endlessPrestigeDisplay;
    }

    public String getEndlessPrestigePrefix() {
        return (endlessPrestigePrefix == null || endlessPrestigePrefix.isBlank())
                ? "P"
                : endlessPrestigePrefix.trim();
    }

    public String getLanguage() {
        return (language == null || language.trim().isEmpty())
                ? "en_US"
                : language.trim();
    }

    public boolean isOwnedTagsCommandEnabled() {
        return ownedTagsCommandEnabled == null || ownedTagsCommandEnabled;
    }

    public String getPlaytimeProviderMode() {
        String value = playtimeProvider;
        if (value == null || value.trim().isEmpty()) {
            return "AUTO";
        }
        return value.trim().toUpperCase();
    }

    public int getTagEquipDelaySeconds() {
        return Math.max(0, tagDelaysecs);
    }

    public RenderingSettings getRendering() {
        if (rendering == null) {
            rendering = new RenderingSettings();
        }
        return rendering;
    }
}