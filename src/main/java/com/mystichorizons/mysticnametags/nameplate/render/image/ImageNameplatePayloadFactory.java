package com.mystichorizons.mysticnametags.nameplate.render.image;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mystichorizons.mysticnametags.nameplate.image.NameplateAssetIds;
import com.mystichorizons.mysticnametags.nameplate.image.NameplateHashing;
import com.mystichorizons.mysticnametags.nameplate.image.RenderedNameplateImage;
import com.mystichorizons.mysticnametags.nameplate.image.RuntimeNameplateAsset;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.util.logging.Level;

public final class ImageNameplatePayloadFactory {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final com.mystichorizons.mysticnametags.nameplate.image.NameplateRuntimeAssets runtimeAssets =
            new com.mystichorizons.mysticnametags.nameplate.image.NameplateRuntimeAssets();

    private final com.mystichorizons.mysticnametags.nameplate.image.NameplateImageRenderer imageRenderer =
            new com.mystichorizons.mysticnametags.nameplate.image.NameplateImageRenderer();

    private final NameplateAssetIds assetIds = new NameplateAssetIds();

    @Nullable
    public ImageNameplateRenderPayload create(@Nonnull PlayerRef playerRef,
                                              @Nonnull ResolvedNameplate resolvedNameplate) {
        try {
            BufferedImage image = imageRenderer.render(resolvedNameplate);
            if (image == null) {
                return null;
            }

            String hash = NameplateHashing.sha256(resolvedNameplate.cacheKey());
            String key = assetIds.runtimeKey(hash);
            String modelAssetId = assetIds.modelAssetId(hash);
            String billboardModelAssetId = assetIds.billboardModelAssetId(hash);

            RenderedNameplateImage rendered = new RenderedNameplateImage(
                    key,
                    image,
                    image.getWidth(),
                    image.getHeight()
            );

            RuntimeNameplateAsset runtimeAsset = runtimeAssets.getOrCreate(
                    key,
                    modelAssetId,
                    billboardModelAssetId,
                    image
            );

            if (runtimeAsset == null) {
                return null;
            }

            return new ImageNameplateRenderPayload(rendered, runtimeAsset);
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t)
                    .log("[MysticNameTags] Failed to build ImageNameplateRenderPayload for %s", playerRef.getUsername());
            return null;
        }
    }
}