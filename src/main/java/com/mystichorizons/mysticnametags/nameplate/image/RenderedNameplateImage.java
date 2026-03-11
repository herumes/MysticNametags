package com.mystichorizons.mysticnametags.nameplate.image;

import java.awt.image.BufferedImage;

public final class RenderedNameplateImage {

    private final String contentHash;
    private final BufferedImage image;

    public RenderedNameplateImage(String contentHash, BufferedImage image) {
        this.contentHash = contentHash;
        this.image = image;
    }

    public String getContentHash() {
        return contentHash;
    }

    public BufferedImage getImage() {
        return image;
    }
}