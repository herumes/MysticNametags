package com.mystichorizons.mysticnametags.nameplate.image;

import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.common.util.java.ManifestUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.universe.Universe;
import com.mystichorizons.mysticnametags.MysticNameTagsPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

/**
 * Runtime image asset pipeline for rendered nameplates.
 *
 * Responsibilities:
 * - turn BufferedImage -> PNG bytes
 * - register texture as CommonAsset
 * - generate blockymodel json
 * - generate/load ModelAsset
 * - optionally send assets to online players
 * - optionally write an asset pack zip for persistence / restart resilience
 *
 * This is intentionally the runtime bridge between:
 *
 *   RenderedNameplateImage
 *       -> RuntimeNameplateAsset
 *       -> Hytale model asset
 *       -> entity spawn backend
 */
public final class NameplateRuntimeAssets {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String PACK_NAME = "MysticNameTagsNameplates";
    private static final String ZIP_NAME = "MysticNameTagsNameplates.zip";

    private static final String ASSET_PREFIX = "MysticNameTags_Nameplate_";
    private static final String TEXTURE_ROOT = "Characters/MysticNameTags";
    private static final String MODEL_ROOT = "Characters/MysticNameTags";

    private final MysticNameTagsPlugin plugin;
    private final Path pluginDataFolder;
    private final Path runtimeFolder;
    private final Path modsFolder;
    private final Path zipPath;

    /**
     * Cache by hash so identical rendered nameplates reuse identical assets.
     */
    private final Map<String, RuntimeNameplateAsset> runtimeCache = new ConcurrentHashMap<>();

    public NameplateRuntimeAssets(@Nonnull MysticNameTagsPlugin plugin) {
        this.plugin = plugin;
        this.pluginDataFolder = plugin.getDataDirectory().toPath();
        this.runtimeFolder = pluginDataFolder.resolve("runtime-nameplates");

        Path parent = pluginDataFolder.getParent();
        this.modsFolder = parent != null ? parent : pluginDataFolder;
        this.zipPath = modsFolder.resolve(ZIP_NAME);
    }

    public void initialize() {
        try {
            Files.createDirectories(runtimeFolder);
            LOGGER.at(Level.INFO).log("[MysticNameTags] NameplateRuntimeAssets initialized.");
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e)
                    .log("[MysticNameTags] Failed to initialize runtime nameplate asset folder.");
        }
    }

    /**
     * Get or create runtime assets for a rendered image.
     */
    @Nonnull
    public RuntimeNameplateAsset getOrCreate(@Nonnull RenderedNameplateImage rendered) {
        final String hash = rendered.getContentHash();

        RuntimeNameplateAsset cached = runtimeCache.get(hash);
        if (cached != null && isModelAssetLoaded(cached.getModelAssetId())) {
            return cached;
        }

        synchronized (hash.intern()) {
            cached = runtimeCache.get(hash);
            if (cached != null && isModelAssetLoaded(cached.getModelAssetId())) {
                return cached;
            }

            RuntimeNameplateAsset created = bake(rendered);
            runtimeCache.put(hash, created);
            return created;
        }
    }

    /**
     * Bake one rendered image into runtime assets and load them immediately.
     */
    @Nonnull
    private RuntimeNameplateAsset bake(@Nonnull RenderedNameplateImage rendered) {
        String hash = rendered.getContentHash();
        BufferedImage image = rendered.getImage();

        int width = image.getWidth();
        int height = image.getHeight();

        NameplateAssetIds ids = NameplateAssetIds.fromHash(hash, ASSET_PREFIX);

        try {
            byte[] pngBytes = toPngBytes(image);

            String textureName = TEXTURE_ROOT + "/" + ids.textureFileName();
            String blockyModelName = MODEL_ROOT + "/" + ids.blockyModelFileName();
            String billboardBlockyModelName = MODEL_ROOT + "/" + ids.billboardBlockyModelFileName();

            String texturePath = textureName;
            String blockyModelPath = blockyModelName;
            String billboardBlockyModelPath = billboardBlockyModelName;

            String blockyJson = createBlockyModelJson(width, height, false);
            String billboardBlockyJson = createBlockyModelJson(width, height, true);

            CommonAssetModule commonAssetModule = CommonAssetModule.get();
            if (commonAssetModule == null) {
                LOGGER.at(Level.WARNING)
                        .log("[MysticNameTags] CommonAssetModule unavailable. Runtime nameplate assets may not load.");
            } else {
                registerCommonAsset(commonAssetModule, textureName, pngBytes);
                registerCommonAsset(commonAssetModule, blockyModelName, blockyJson.getBytes(StandardCharsets.UTF_8));
                registerCommonAsset(commonAssetModule, billboardBlockyModelName, billboardBlockyJson.getBytes(StandardCharsets.UTF_8));
            }

            ModelAsset regularModel = createModelAsset(
                    ids.modelAssetId(),
                    texturePath,
                    blockyModelPath,
                    width,
                    height
            );

            ModelAsset billboardModel = createModelAsset(
                    ids.billboardModelAssetId(),
                    texturePath,
                    billboardBlockyModelPath,
                    width,
                    height
            );

            loadModelAssets(regularModel, billboardModel);

            if (commonAssetModule != null && Universe.get().getPlayerCount() > 0) {
                commonAssetModule.sendAssets(
                        java.util.List.of(
                                new RuntimeNameplateCommonAsset(textureName, pngBytes),
                                new RuntimeNameplateCommonAsset(blockyModelName, blockyJson.getBytes(StandardCharsets.UTF_8)),
                                new RuntimeNameplateCommonAsset(billboardBlockyModelName, billboardBlockyJson.getBytes(StandardCharsets.UTF_8))
                        ),
                        true
                );
            }

            persistZipEntry(ids, pngBytes, blockyJson, billboardBlockyJson, width, height);

            RuntimeNameplateAsset asset = new RuntimeNameplateAsset(
                    hash,
                    ids.modelAssetId(),
                    ids.billboardModelAssetId(),
                    texturePath,
                    width,
                    height,
                    isModelAssetLoaded(ids.modelAssetId()),
                    isModelAssetLoaded(ids.billboardModelAssetId())
            );

            LOGGER.at(Level.INFO).log(
                    "[MysticNameTags] Baked runtime nameplate asset hash=" + hash +
                            " model=" + ids.modelAssetId() +
                            " size=" + width + "x" + height
            );

            return asset;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e)
                    .log("[MysticNameTags] Failed to bake runtime nameplate asset for hash=" + hash);

            return new RuntimeNameplateAsset(
                    hash,
                    ids.modelAssetId(),
                    ids.billboardModelAssetId(),
                    TEXTURE_ROOT + "/" + ids.textureFileName(),
                    width,
                    height,
                    false,
                    false
            );
        }
    }

    private void registerCommonAsset(@Nonnull CommonAssetModule module,
                                     @Nonnull String name,
                                     @Nonnull byte[] bytes) {
        module.addCommonAsset(PACK_NAME, new RuntimeNameplateCommonAsset(name, bytes), false);
    }

    private void loadModelAssets(@Nonnull ModelAsset regularModel,
                                 @Nonnull ModelAsset billboardModel) {
        try {
            AssetStore<String, ModelAsset, DefaultAssetMap<String, ModelAsset>> modelAssetStore =
                    ModelAsset.getAssetStore();

            modelAssetStore.loadAssets(PACK_NAME, java.util.List.of(regularModel, billboardModel));
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e)
                    .log("[MysticNameTags] Failed loading runtime ModelAssets.");
        }
    }

    @Nonnull
    private static byte[] toPngBytes(@Nonnull BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Character/prop blockymodel using one textured quad.
     * Billboard version uses lod=billboard.
     */
    @Nonnull
    private static String createBlockyModelJson(int width, int height, boolean billboard) {
        String lodValue = billboard ? "billboard" : "auto";

        return String.format(Locale.US,
                "{\n" +
                        "  \"nodes\": [\n" +
                        "    {\n" +
                        "      \"id\": \"1\",\n" +
                        "      \"name\": \"Front\",\n" +
                        "      \"position\": {\"x\": 0, \"y\": 0, \"z\": 0},\n" +
                        "      \"orientation\": {\"x\": 0, \"y\": 0, \"z\": 0, \"w\": 1},\n" +
                        "      \"shape\": {\n" +
                        "        \"type\": \"quad\",\n" +
                        "        \"offset\": {\"x\": 0, \"y\": 0, \"z\": 0.001},\n" +
                        "        \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1},\n" +
                        "        \"settings\": {\n" +
                        "          \"size\": {\"x\": %d, \"y\": %d},\n" +
                        "          \"normal\": \"+Z\"\n" +
                        "        },\n" +
                        "        \"visible\": true,\n" +
                        "        \"doubleSided\": false,\n" +
                        "        \"shadingMode\": \"flat\",\n" +
                        "        \"unwrapMode\": \"custom\",\n" +
                        "        \"textureLayout\": {\n" +
                        "          \"front\": {\n" +
                        "            \"offset\": {\"x\": 0, \"y\": 0},\n" +
                        "            \"mirror\": {\"x\": false, \"y\": false},\n" +
                        "            \"angle\": 0\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": \"2\",\n" +
                        "      \"name\": \"Back\",\n" +
                        "      \"position\": {\"x\": 0, \"y\": 0, \"z\": 0},\n" +
                        "      \"orientation\": {\"x\": 0, \"y\": 0, \"z\": 0, \"w\": 1},\n" +
                        "      \"shape\": {\n" +
                        "        \"type\": \"quad\",\n" +
                        "        \"offset\": {\"x\": 0, \"y\": 0, \"z\": -0.001},\n" +
                        "        \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1},\n" +
                        "        \"settings\": {\n" +
                        "          \"size\": {\"x\": %d, \"y\": %d},\n" +
                        "          \"normal\": \"-Z\"\n" +
                        "        },\n" +
                        "        \"visible\": true,\n" +
                        "        \"doubleSided\": false,\n" +
                        "        \"shadingMode\": \"flat\",\n" +
                        "        \"unwrapMode\": \"custom\",\n" +
                        "        \"textureLayout\": {\n" +
                        "          \"front\": {\n" +
                        "            \"offset\": {\"x\": 0, \"y\": 0},\n" +
                        "            \"mirror\": {\"x\": false, \"y\": false},\n" +
                        "            \"angle\": 0\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"format\": \"character\",\n" +
                        "  \"lod\": \"%s\"\n" +
                        "}\n",
                width, height, width, height, lodValue
        );
    }

    @Nonnull
    private static ModelAsset createModelAsset(@Nonnull String modelAssetId,
                                               @Nonnull String texturePath,
                                               @Nonnull String blockyModelPath,
                                               int width,
                                               int height) throws Exception {

        double aspectRatio = (double) width / (double) height;
        double normalizedWidth;
        double normalizedHeight;

        if (width >= height) {
            normalizedWidth = 1.0D;
            normalizedHeight = 1.0D / aspectRatio;
        } else {
            normalizedHeight = 1.0D;
            normalizedWidth = aspectRatio;
        }

        double halfWidth = normalizedWidth / 2.0D;

        ModelAsset modelAsset = new ModelAsset();

        setField(modelAsset, "id", modelAssetId);
        setField(modelAsset, "model", blockyModelPath);
        setField(modelAsset, "texture", texturePath);
        setField(modelAsset, "minScale", 0.01F);
        setField(modelAsset, "maxScale", 100.0F);
        setField(modelAsset, "boundingBox", new Box(
                -halfWidth, 0.0D, -0.01D,
                halfWidth, normalizedHeight, 0.01D
        ));

        return modelAsset;
    }

    private static void setField(@Nonnull Object target,
                                 @Nonnull String fieldName,
                                 @Nullable Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private boolean isModelAssetLoaded(@Nonnull String modelAssetId) {
        try {
            return ModelAsset.getAssetMap().getAsset(modelAssetId) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Writes/updates a restart-safe zip in the mods folder.
     * For now we rewrite the full zip from current cache on each new bake.
     * That is acceptable for a first production version.
     */
    private synchronized void persistZipEntry(@Nonnull NameplateAssetIds ids,
                                              @Nonnull byte[] pngBytes,
                                              @Nonnull String blockyJson,
                                              @Nonnull String billboardBlockyJson,
                                              int width,
                                              int height) {
        try {
            Files.createDirectories(modsFolder);

            Map<String, byte[]> entries = new LinkedHashMap<>();

            for (RuntimeNameplateAsset existing : runtimeCache.values()) {
                // Existing cache entries are not reconstructed from disk here.
                // This version writes only newly baked entries unless process lifetime kept cache alive.
            }

            entries.put("Common/" + TEXTURE_ROOT + "/" + ids.textureFileName(), pngBytes);
            entries.put("Common/" + MODEL_ROOT + "/" + ids.blockyModelFileName(), blockyJson.getBytes(StandardCharsets.UTF_8));
            entries.put("Common/" + MODEL_ROOT + "/" + ids.billboardBlockyModelFileName(), billboardBlockyJson.getBytes(StandardCharsets.UTF_8));
            entries.put("Server/Models/" + ids.modelAssetId() + ".json",
                    createModelAssetJson(ids.modelAssetId(), ids.textureFileName(), ids.blockyModelFileName(), width, height).getBytes(StandardCharsets.UTF_8));
            entries.put("Server/Models/" + ids.billboardModelAssetId() + ".json",
                    createModelAssetJson(ids.billboardModelAssetId(), ids.textureFileName(), ids.billboardBlockyModelFileName(), width, height).getBytes(StandardCharsets.UTF_8));

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    zos.putNextEntry(new ZipEntry(entry.getKey()));
                    zos.write(entry.getValue());
                    zos.closeEntry();
                }

                zos.putNextEntry(new ZipEntry("manifest.json"));
                zos.write(createManifestJson().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e)
                    .log("[MysticNameTags] Failed writing runtime nameplate zip.");
        }
    }

    @Nonnull
    private static String createModelAssetJson(@Nonnull String modelAssetId,
                                               @Nonnull String textureFileName,
                                               @Nonnull String blockyModelFileName,
                                               int width,
                                               int height) {
        double aspectRatio = (double) width / (double) height;
        double normalizedWidth;
        double normalizedHeight;

        if (width >= height) {
            normalizedWidth = 1.0D;
            normalizedHeight = 1.0D / aspectRatio;
        } else {
            normalizedHeight = 1.0D;
            normalizedWidth = aspectRatio;
        }

        double halfWidth = normalizedWidth / 2.0D;

        return String.format(Locale.US,
                "{\n" +
                        "  \"Model\": \"%s/%s\",\n" +
                        "  \"Texture\": \"%s/%s\",\n" +
                        "  \"MinScale\": 0.01,\n" +
                        "  \"MaxScale\": 100.0,\n" +
                        "  \"EyeHeight\": 0.0,\n" +
                        "  \"CrouchOffset\": 0.0,\n" +
                        "  \"HitBox\": {\n" +
                        "    \"Min\": { \"X\": %.4f, \"Y\": 0.0, \"Z\": -0.01 },\n" +
                        "    \"Max\": { \"X\": %.4f, \"Y\": %.4f, \"Z\": 0.01 }\n" +
                        "  }\n" +
                        "}\n",
                MODEL_ROOT, blockyModelFileName,
                TEXTURE_ROOT, textureFileName,
                -halfWidth, halfWidth, normalizedHeight
        );
    }

    @Nonnull
    private static String createManifestJson() {
        String serverVersion = ManifestUtil.getVersion();
        if (serverVersion == null) {
            serverVersion = "*";
        }

        return String.format(
                "{\n" +
                        "  \"Name\": \"%s\",\n" +
                        "  \"Group\": \"com.mystichorizons.mysticnametags\",\n" +
                        "  \"Version\": \"1.0.0\",\n" +
                        "  \"Description\": \"Runtime-generated MysticNameTags image assets\",\n" +
                        "  \"ServerVersion\": \"%s\",\n" +
                        "  \"IncludesAssetPack\": true\n" +
                        "}\n",
                PACK_NAME,
                serverVersion
        );
    }

    public void clearCache() {
        runtimeCache.clear();
    }

    @Nonnull
    public Map<String, RuntimeNameplateAsset> getRuntimeCache() {
        return java.util.Collections.unmodifiableMap(runtimeCache);
    }

    public boolean hasZipOnDisk() {
        return Files.exists(zipPath, LinkOption.NOFOLLOW_LINKS);
    }

    @Nonnull
    public Path getZipPath() {
        return zipPath;
    }

    /**
     * Simple CommonAsset implementation for runtime bytes.
     */
    private static final class RuntimeNameplateCommonAsset extends CommonAsset {
        private final byte[] data;

        private RuntimeNameplateCommonAsset(@Nonnull String name, @Nonnull byte[] data) {
            super(name, data);
            this.data = data;
        }

        @Nonnull
        @Override
        protected java.util.concurrent.CompletableFuture<byte[]> getBlob0() {
            return java.util.concurrent.CompletableFuture.completedFuture(data);
        }
    }
}