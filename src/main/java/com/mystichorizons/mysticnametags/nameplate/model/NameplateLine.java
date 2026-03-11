package com.mystichorizons.mysticnametags.nameplate.model;

public final class NameplateLine {

    private final String text;
    private final String styleKey;

    public NameplateLine(String text, String styleKey) {
        this.text = (text == null ? "" : text);
        this.styleKey = (styleKey == null ? "default" : styleKey);
    }

    public String getText() {
        return text;
    }

    public String getStyleKey() {
        return styleKey;
    }
}