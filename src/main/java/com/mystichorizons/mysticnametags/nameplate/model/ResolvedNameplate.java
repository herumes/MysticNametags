package com.mystichorizons.mysticnametags.nameplate.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ResolvedNameplate {

    private final UUID playerId;
    private final List<NameplateLine> lines;
    private final NameplateAppearance appearance;
    private final String fingerprint;

    public ResolvedNameplate(UUID playerId,
                             List<NameplateLine> lines,
                             NameplateAppearance appearance,
                             String fingerprint) {
        this.playerId = playerId;
        this.lines = (lines == null ? Collections.emptyList() : List.copyOf(lines));
        this.appearance = appearance;
        this.fingerprint = (fingerprint == null ? "" : fingerprint);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public List<NameplateLine> getLines() {
        return lines;
    }

    public NameplateAppearance getAppearance() {
        return appearance;
    }

    public String getFingerprint() {
        return fingerprint;
    }
}