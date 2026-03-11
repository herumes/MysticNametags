package com.mystichorizons.mysticnametags.config.render;

public final class GlyphSettings {

    private boolean enabled = false;
    private int maxChars = 32;
    private int updateTicks = 10;
    private float renderDistance = 24.0f;
    private int maxEntitiesPerPlayer = 40;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxChars() {
        return Math.max(8, maxChars);
    }

    public void setMaxChars(int maxChars) {
        this.maxChars = maxChars;
    }

    public int getUpdateTicks() {
        return Math.max(1, updateTicks);
    }

    public void setUpdateTicks(int updateTicks) {
        this.updateTicks = updateTicks;
    }

    public float getRenderDistance() {
        return Math.max(8f, renderDistance);
    }

    public void setRenderDistance(float renderDistance) {
        this.renderDistance = renderDistance;
    }

    public int getMaxEntitiesPerPlayer() {
        return Math.max(8, maxEntitiesPerPlayer);
    }

    public void setMaxEntitiesPerPlayer(int maxEntitiesPerPlayer) {
        this.maxEntitiesPerPlayer = maxEntitiesPerPlayer;
    }
}