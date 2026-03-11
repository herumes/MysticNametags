package com.mystichorizons.mysticnametags.nameplate.image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class SpawnedNameplateState {

    @Nullable
    private final UUID entityUuid;
    @Nonnull
    private final String assetKey;
    @Nonnull
    private final String modelAssetId;
    @Nonnull
    private final String billboardModelAssetId;
    private final int width;
    private final int height;
    private final boolean billboard;

    public SpawnedNameplateState(@Nullable UUID entityUuid,
                                 @Nonnull String assetKey,
                                 @Nonnull String modelAssetId,
                                 @Nonnull String billboardModelAssetId,
                                 int width,
                                 int height,
                                 boolean billboard) {
        this.entityUuid = entityUuid;
        this.assetKey = assetKey;
        this.modelAssetId = modelAssetId;
        this.billboardModelAssetId = billboardModelAssetId;
        this.width = width;
        this.height = height;
        this.billboard = billboard;
    }

    @Nullable
    public UUID getEntityUuid() {
        return entityUuid;
    }

    @Nonnull
    public String getAssetKey() {
        return assetKey;
    }

    @Nonnull
    public String getModelAssetId() {
        return modelAssetId;
    }

    @Nonnull
    public String getBillboardModelAssetId() {
        return billboardModelAssetId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isBillboard() {
        return billboard;
    }
}