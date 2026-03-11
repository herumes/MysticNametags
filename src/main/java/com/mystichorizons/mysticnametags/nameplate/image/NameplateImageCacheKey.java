package com.mystichorizons.mysticnametags.nameplate.image;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Stable cache key for rendered nameplate images.
 *
 * This should change whenever the final rendered result could change.
 */
public final class NameplateImageCacheKey {

    private final String text;
    private final boolean shadow;
    private final int fontSize;
    private final int paddingX;
    private final int paddingY;
    private final int maxWidth;
    private final int maxLines;
    private final int lineSpacing;
    private final String themeKey;

    public NameplateImageCacheKey(@Nonnull String text,
                                  boolean shadow,
                                  int fontSize,
                                  int paddingX,
                                  int paddingY,
                                  int maxWidth,
                                  int maxLines,
                                  int lineSpacing,
                                  @Nonnull String themeKey) {
        this.text = Objects.requireNonNull(text, "text");
        this.shadow = shadow;
        this.fontSize = fontSize;
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        this.maxWidth = maxWidth;
        this.maxLines = maxLines;
        this.lineSpacing = lineSpacing;
        this.themeKey = Objects.requireNonNull(themeKey, "themeKey");
    }

    @Nonnull
    public String getText() {
        return text;
    }

    public boolean isShadow() {
        return shadow;
    }

    public int getFontSize() {
        return fontSize;
    }

    public int getPaddingX() {
        return paddingX;
    }

    public int getPaddingY() {
        return paddingY;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public int getLineSpacing() {
        return lineSpacing;
    }

    @Nonnull
    public String getThemeKey() {
        return themeKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NameplateImageCacheKey)) return false;
        NameplateImageCacheKey that = (NameplateImageCacheKey) o;
        return shadow == that.shadow
                && fontSize == that.fontSize
                && paddingX == that.paddingX
                && paddingY == that.paddingY
                && maxWidth == that.maxWidth
                && maxLines == that.maxLines
                && lineSpacing == that.lineSpacing
                && text.equals(that.text)
                && themeKey.equals(that.themeKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                text,
                shadow,
                fontSize,
                paddingX,
                paddingY,
                maxWidth,
                maxLines,
                lineSpacing,
                themeKey
        );
    }

    @Override
    public String toString() {
        return "NameplateImageCacheKey{" +
                "text='" + text + '\'' +
                ", shadow=" + shadow +
                ", fontSize=" + fontSize +
                ", paddingX=" + paddingX +
                ", paddingY=" + paddingY +
                ", maxWidth=" + maxWidth +
                ", maxLines=" + maxLines +
                ", lineSpacing=" + lineSpacing +
                ", themeKey='" + themeKey + '\'' +
                '}';
    }
}