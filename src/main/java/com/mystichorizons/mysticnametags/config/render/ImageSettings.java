package com.mystichorizons.mysticnametags.config.render;

public final class ImageSettings {

    private boolean enabled = false;
    private boolean hideVanillaNameplate = true;

    private int minTicksBetweenUpdates = 5;
    private int placeholderRefreshTicks = 20;
    private int movementRefreshTicks = 2;
    private int maxBatchUpdatesPerTick = 10;

    private float renderDistance = 32.0f;
    private float verticalOffset = 2.15f;

    private boolean cacheComposedLayouts = true;
    private int maxCachedLayouts = 512;

    private boolean showRank = true;
    private boolean showName = true;
    private boolean showTag = true;
    private boolean showPrestige = false;
    private boolean showRace = false;

    private ImageLayoutSettings layout = new ImageLayoutSettings();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHideVanillaNameplate() {
        return hideVanillaNameplate;
    }

    public void setHideVanillaNameplate(boolean hideVanillaNameplate) {
        this.hideVanillaNameplate = hideVanillaNameplate;
    }

    public int getMinTicksBetweenUpdates() {
        return Math.max(1, minTicksBetweenUpdates);
    }

    public void setMinTicksBetweenUpdates(int minTicksBetweenUpdates) {
        this.minTicksBetweenUpdates = minTicksBetweenUpdates;
    }

    public int getPlaceholderRefreshTicks() {
        return Math.max(1, placeholderRefreshTicks);
    }

    public void setPlaceholderRefreshTicks(int placeholderRefreshTicks) {
        this.placeholderRefreshTicks = placeholderRefreshTicks;
    }

    public int getMovementRefreshTicks() {
        return Math.max(1, movementRefreshTicks);
    }

    public void setMovementRefreshTicks(int movementRefreshTicks) {
        this.movementRefreshTicks = movementRefreshTicks;
    }

    public int getMaxBatchUpdatesPerTick() {
        return Math.max(1, maxBatchUpdatesPerTick);
    }

    public void setMaxBatchUpdatesPerTick(int maxBatchUpdatesPerTick) {
        this.maxBatchUpdatesPerTick = maxBatchUpdatesPerTick;
    }

    public float getRenderDistance() {
        return Math.max(8f, renderDistance);
    }

    public void setRenderDistance(float renderDistance) {
        this.renderDistance = renderDistance;
    }

    public float getVerticalOffset() {
        return Math.max(0f, verticalOffset);
    }

    public void setVerticalOffset(float verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

    public boolean isCacheComposedLayouts() {
        return cacheComposedLayouts;
    }

    public void setCacheComposedLayouts(boolean cacheComposedLayouts) {
        this.cacheComposedLayouts = cacheComposedLayouts;
    }

    public int getMaxCachedLayouts() {
        return Math.max(16, maxCachedLayouts);
    }

    public void setMaxCachedLayouts(int maxCachedLayouts) {
        this.maxCachedLayouts = maxCachedLayouts;
    }

    public boolean isShowRank() {
        return showRank;
    }

    public void setShowRank(boolean showRank) {
        this.showRank = showRank;
    }

    public boolean isShowName() {
        return showName;
    }

    public void setShowName(boolean showName) {
        this.showName = showName;
    }

    public boolean isShowTag() {
        return showTag;
    }

    public void setShowTag(boolean showTag) {
        this.showTag = showTag;
    }

    public boolean isShowPrestige() {
        return showPrestige;
    }

    public void setShowPrestige(boolean showPrestige) {
        this.showPrestige = showPrestige;
    }

    public boolean isShowRace() {
        return showRace;
    }

    public void setShowRace(boolean showRace) {
        this.showRace = showRace;
    }

    public ImageLayoutSettings getLayout() {
        if (layout == null) {
            layout = new ImageLayoutSettings();
        }
        return layout;
    }

    public void setLayout(ImageLayoutSettings layout) {
        this.layout = (layout == null ? new ImageLayoutSettings() : layout);
    }
}