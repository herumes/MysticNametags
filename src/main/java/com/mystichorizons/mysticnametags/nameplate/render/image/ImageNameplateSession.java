package com.mystichorizons.mysticnametags.nameplate.render.image;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.UUID;

public final class ImageNameplateSession {

    private final UUID playerId;
    private final ImageNameplateInstance instance;

    private String lastFingerprint = "";
    private boolean spawned = false;
    private long lastRenderMillis = 0L;

    private ImageNameplateRenderPayload pendingPayload;

    private transient PlayerRef playerRef;
    private transient World world;

    public ImageNameplateSession(UUID playerId) {
        this.playerId = playerId;
        this.instance = new ImageNameplateInstance(playerId);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public ImageNameplateInstance getInstance() {
        return instance;
    }

    public String getLastFingerprint() {
        return lastFingerprint == null ? "" : lastFingerprint;
    }

    public void setLastFingerprint(String lastFingerprint) {
        this.lastFingerprint = (lastFingerprint == null ? "" : lastFingerprint);
    }

    public boolean isSpawned() {
        return spawned;
    }

    public void setSpawned(boolean spawned) {
        this.spawned = spawned;
    }

    public long getLastRenderMillis() {
        return lastRenderMillis;
    }

    public void setLastRenderMillis(long lastRenderMillis) {
        this.lastRenderMillis = Math.max(0L, lastRenderMillis);
    }

    public ImageNameplateRenderPayload getPendingPayload() {
        return pendingPayload;
    }

    public void setPendingPayload(ImageNameplateRenderPayload pendingPayload) {
        this.pendingPayload = pendingPayload;
    }

    public boolean hasPendingPayload() {
        return pendingPayload != null;
    }

    public void clearPendingPayload() {
        this.pendingPayload = null;
    }

    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    public void setPlayerRef(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }
}