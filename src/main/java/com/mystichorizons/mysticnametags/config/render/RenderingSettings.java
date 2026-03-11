package com.mystichorizons.mysticnametags.config.render;

import com.mystichorizons.mysticnametags.nameplate.render.NameplateRenderMode;

public final class RenderingSettings {

    private NameplateRenderMode mode = NameplateRenderMode.VANILLA_TEXT;
    private GlyphSettings glyph = new GlyphSettings();
    private ImageSettings image = new ImageSettings();

    public NameplateRenderMode getMode() {
        return mode == null ? NameplateRenderMode.VANILLA_TEXT : mode;
    }

    public void setMode(NameplateRenderMode mode) {
        this.mode = (mode == null ? NameplateRenderMode.VANILLA_TEXT : mode);
    }

    public GlyphSettings getGlyph() {
        if (glyph == null) {
            glyph = new GlyphSettings();
        }
        return glyph;
    }

    public void setGlyph(GlyphSettings glyph) {
        this.glyph = (glyph == null ? new GlyphSettings() : glyph);
    }

    public ImageSettings getImage() {
        if (image == null) {
            image = new ImageSettings();
        }
        return image;
    }

    public void setImage(ImageSettings image) {
        this.image = (image == null ? new ImageSettings() : image);
    }
}