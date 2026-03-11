package com.mystichorizons.mysticnametags.nameplate.model;

import java.util.Collections;
import java.util.List;

public final class NameplateAppearance {

    private final String backgroundAssetId;
    private final float verticalOffset;
    private final boolean hideVanillaNameplate;
    private final List<String> iconAssetIds;

    public NameplateAppearance(String backgroundAssetId,
                               float verticalOffset,
                               boolean hideVanillaNameplate,
                               List<String> iconAssetIds) {
        this.backgroundAssetId = (backgroundAssetId == null ? "" : backgroundAssetId);
        this.verticalOffset = verticalOffset;
        this.hideVanillaNameplate = hideVanillaNameplate;
        this.iconAssetIds = (iconAssetIds == null ? Collections.emptyList() : List.copyOf(iconAssetIds));
    }

    public String getBackgroundAssetId() {
        return backgroundAssetId;
    }

    public float getVerticalOffset() {
        return verticalOffset;
    }

    public boolean isHideVanillaNameplate() {
        return hideVanillaNameplate;
    }

    public List<String> getIconAssetIds() {
        return iconAssetIds;
    }
}