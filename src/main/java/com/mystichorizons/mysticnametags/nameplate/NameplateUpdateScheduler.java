package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.mystichorizons.mysticnametags.nameplate.state.NameplateDirtyReason;

import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.logging.Level;

public final class NameplateUpdateScheduler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Queue<UUID> queue = new ConcurrentLinkedQueue<>();
    private final Set<UUID> queued = ConcurrentHashMap.newKeySet();

    public void enqueue(UUID playerId) {
        if (playerId == null) {
            return;
        }
        if (queued.add(playerId)) {
            queue.offer(playerId);
        }
    }

    public void enqueue(PlayerRef playerRef) {
        if (playerRef != null) {
            enqueue(playerRef.getUuid());
        }
    }

    public void clear() {
        queue.clear();
        queued.clear();
    }

    public int drain(int maxCount, Function<UUID, Boolean> consumer) {
        if (consumer == null || maxCount <= 0) {
            return 0;
        }

        int processed = 0;

        while (processed < maxCount) {
            UUID playerId = queue.poll();
            if (playerId == null) {
                break;
            }

            queued.remove(playerId);

            try {
                Boolean ok = consumer.apply(playerId);
                if (Boolean.TRUE.equals(ok)) {
                    processed++;
                }
            } catch (Throwable t) {
                LOGGER.at(Level.WARNING).withCause(t)
                        .log("[MysticNameTags] Failed to process queued nameplate refresh for " + playerId);
            }
        }

        return processed;
    }
}