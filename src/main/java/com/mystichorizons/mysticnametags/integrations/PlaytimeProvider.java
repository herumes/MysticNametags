package com.mystichorizons.mysticnametags.integrations;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Abstraction for playtime providers.
 *
 * Returns total playtime in MINUTES.
 */
public interface PlaytimeProvider {

    /**
     * Total playtime in minutes (floor).
     *
     * Implementations may clamp or floor as needed, but should not throw.
     */
    long getPlaytimeMinutes(@Nonnull UUID uuid);
}