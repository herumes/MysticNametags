package com.mystichorizons.mysticnametags.nameplate;

import com.mystichorizons.mysticnametags.tags.Tag;

public final class PlayerNameplateSnapshot {

    private final String rank;
    private final String playerName;
    private final Tag activeTag;

    public PlayerNameplateSnapshot(String rank, String playerName, Tag activeTag) {
        this.rank = (rank == null ? "" : rank);
        this.playerName = (playerName == null ? "" : playerName);
        this.activeTag = activeTag;
    }

    public String getRank() {
        return rank;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Tag getActiveTag() {
        return activeTag;
    }
}