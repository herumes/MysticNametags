package com.mystichorizons.mysticnametags.nameplate.state;

public enum NameplateDirtyReason {
    PLAYER_JOIN,
    PLAYER_QUIT,
    TAG_CHANGED,
    RANK_CHANGED,
    PLACEHOLDER_REFRESH,
    PRESTIGE_CHANGED,
    RACE_CHANGED,
    CONFIG_RELOAD,
    MODE_CHANGED,
    VISIBILITY_CHANGED,
    FORCED_REFRESH
}