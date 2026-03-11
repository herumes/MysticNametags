package com.mystichorizons.mysticnametags.nameplate.image;

import javax.annotation.Nonnull;

public final class NameplateAssetIds {

    private final String baseId;
    private final String modelAssetId;
    private final String billboardModelAssetId;
    private final String textureFileName;
    private final String blockyModelFileName;
    private final String billboardBlockyModelFileName;

    private NameplateAssetIds(String baseId,
                              String modelAssetId,
                              String billboardModelAssetId,
                              String textureFileName,
                              String blockyModelFileName,
                              String billboardBlockyModelFileName) {
        this.baseId = baseId;
        this.modelAssetId = modelAssetId;
        this.billboardModelAssetId = billboardModelAssetId;
        this.textureFileName = textureFileName;
        this.blockyModelFileName = blockyModelFileName;
        this.billboardBlockyModelFileName = billboardBlockyModelFileName;
    }

    @Nonnull
    public static NameplateAssetIds fromHash(@Nonnull String hash, @Nonnull String prefix) {
        String cleanHash = hash.toLowerCase();
        String baseId = prefix + cleanHash;
        return new NameplateAssetIds(
                baseId,
                baseId,
                baseId + "_Billboard",
                baseId + ".png",
                baseId + ".blockymodel",
                baseId + "_Billboard.blockymodel"
        );
    }

    public String baseId() {
        return baseId;
    }

    public String modelAssetId() {
        return modelAssetId;
    }

    public String billboardModelAssetId() {
        return billboardModelAssetId;
    }

    public String textureFileName() {
        return textureFileName;
    }

    public String blockyModelFileName() {
        return blockyModelFileName;
    }

    public String billboardBlockyModelFileName() {
        return billboardBlockyModelFileName;
    }
}