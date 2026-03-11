package com.mystichorizons.mysticnametags.nameplate.render.image;

import java.util.UUID;

public final class ImageNameplateInstance {

    private final UUID playerId;

    private Object anchorHandle;
    private Object visualHandle;

    public ImageNameplateInstance(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Object getAnchorHandle() {
        return anchorHandle;
    }

    public void setAnchorHandle(Object anchorHandle) {
        this.anchorHandle = anchorHandle;
    }

    public Object getVisualHandle() {
        return visualHandle;
    }

    public void setVisualHandle(Object visualHandle) {
        this.visualHandle = visualHandle;
    }

    public boolean hasAnchorHandle() {
        return anchorHandle != null;
    }

    public boolean hasVisualHandle() {
        return visualHandle != null;
    }

    public boolean isBound() {
        return hasAnchorHandle() || hasVisualHandle();
    }

    public void clearHandles() {
        this.anchorHandle = null;
        this.visualHandle = null;
    }
}