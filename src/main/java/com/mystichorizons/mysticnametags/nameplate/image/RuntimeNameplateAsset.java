package com.mystichorizons.mysticnametags.nameplate.image;

public final class RuntimeNameplateAsset {

    private final String hash;
    private final String modelAssetId;
    private final String billboardModelAssetId;
    private final String texturePath;
    private final int width;
    private final int height;
    private final boolean loaded;
    private final boolean billboardLoaded;

    public RuntimeNameplateAsset(String hash,
                                 String modelAssetId,
                                 String billboardModelAssetId,
                                 String texturePath,
                                 int width,
                                 int height,
                                 boolean loaded,
                                 boolean billboardLoaded) {
        this.hash = hash;
        this.modelAssetId = modelAssetId;
        this.billboardModelAssetId = billboardModelAssetId;
        this.texturePath = texturePath;
        this.width = width;
        this.height = height;
        this.loaded = loaded;
        this.billboardLoaded = billboardLoaded;
    }

    public String getHash() {
        return hash;
    }

    public String getModelAssetId() {
        return modelAssetId;
    }

    public String getBillboardModelAssetId() {
        return billboardModelAssetId;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isBillboardLoaded() {
        return billboardLoaded;
    }
}