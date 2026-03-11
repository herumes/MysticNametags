package com.mystichorizons.mysticnametags.config.render;

public final class ImageLayoutSettings {

    private int maxLines = 3;
    private String alignment = "CENTER";

    private int paddingX = 8;
    private int paddingY = 6;
    private int lineSpacing = 2;

    private int minWidth = 64;
    private int maxWidth = 220;
    private int iconSize = 10;

    public int getMaxLines() {
        return Math.max(1, maxLines);
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public String getAlignment() {
        return (alignment == null || alignment.isBlank())
                ? "CENTER"
                : alignment.trim().toUpperCase();
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public int getPaddingX() {
        return Math.max(0, paddingX);
    }

    public void setPaddingX(int paddingX) {
        this.paddingX = paddingX;
    }

    public int getPaddingY() {
        return Math.max(0, paddingY);
    }

    public void setPaddingY(int paddingY) {
        this.paddingY = paddingY;
    }

    public int getLineSpacing() {
        return Math.max(0, lineSpacing);
    }

    public void setLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public int getMinWidth() {
        return Math.max(1, minWidth);
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public int getMaxWidth() {
        return Math.max(getMinWidth(), maxWidth);
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getIconSize() {
        return Math.max(0, iconSize);
    }

    public void setIconSize(int iconSize) {
        this.iconSize = iconSize;
    }
}