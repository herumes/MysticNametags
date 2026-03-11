package com.mystichorizons.mysticnametags.nameplate.render.image;

import com.mystichorizons.mysticnametags.nameplate.image.RenderedNameplateImage;
import com.mystichorizons.mysticnametags.nameplate.image.RuntimeNameplateAsset;

import javax.annotation.Nonnull;

public final class ImageNameplateRenderPayload {

    private final RenderedNameplateImage renderedImage;
    private final RuntimeNameplateAsset runtimeAsset;

    public ImageNameplateRenderPayload(@Nonnull RenderedNameplateImage renderedImage,
                                       @Nonnull RuntimeNameplateAsset runtimeAsset) {
        this.renderedImage = renderedImage;
        this.runtimeAsset = runtimeAsset;
    }

    @Nonnull
    public RenderedNameplateImage getRenderedImage() {
        return renderedImage;
    }

    @Nonnull
    public RuntimeNameplateAsset getRuntimeAsset() {
        return runtimeAsset;
    }
}