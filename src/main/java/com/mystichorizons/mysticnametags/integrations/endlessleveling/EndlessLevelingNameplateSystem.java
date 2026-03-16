package com.mystichorizons.mysticnametags.integrations.endlessleveling;

import com.airijko.endlessleveling.data.PlayerData;
import com.airijko.endlessleveling.managers.PlayerDataManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.config.Settings;
import com.mystichorizons.mysticnametags.tags.TagManager;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EndlessLevelingNameplateSystem extends TickingSystem<EntityStore> {

    private static final Query<EntityStore> PLAYER_QUERY = Query.any();

    private final PlayerDataManager playerDataManager;
    private final TagManager tagManager;

    /**
     * Small fingerprint of EL state.
     * When this changes, we refresh the player through the unified resolver pipeline.
     */
    private final Map<UUID, String> lastStateKeys = new ConcurrentHashMap<>();

    public EndlessLevelingNameplateSystem(@Nonnull PlayerDataManager playerDataManager,
                                          @Nonnull TagManager tagManager) {
        this.playerDataManager = playerDataManager;
        this.tagManager = tagManager;
    }

    @Override
    public void tick(float deltaSeconds, int tickCount, Store<EntityStore> store) {
        if (store == null || store.isShutdown()) return;

        Settings s = Settings.get();
        if (!s.isNameplatesEnabled()) return;
        if (!s.isEndlessLevelingNameplatesEnabled()) return;

        store.forEachChunk(PLAYER_QUERY, (chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                if (ref == null) continue;

                PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null || !playerRef.isValid()) continue;

                UUID uuid = playerRef.getUuid();
                if (uuid == null) continue;

                String baseName = playerRef.getUsername();
                if (baseName == null || baseName.isBlank()) {
                    baseName = "Player";
                }

                PlayerData data = playerDataManager.get(uuid);
                if (data == null) {
                    data = playerDataManager.loadOrCreate(uuid, baseName);
                    if (data == null) continue;
                }

                int level = Math.max(1, data.getLevel());
                int prestige = Math.max(0, data.getPrestigeLevel());

                String raceId = data.getRaceId();
                if (raceId == null || raceId.isBlank()) {
                    raceId = PlayerData.DEFAULT_RACE_ID;
                }

                String stateKey = level + "|" + prestige + "|" + raceId;

                String previous = lastStateKeys.put(uuid, stateKey);
                if (stateKey.equals(previous)) {
                    continue;
                }

                World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
                if (world == null) continue;

                try {
                    tagManager.refreshNameplate(playerRef, world);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    public void invalidate(@Nonnull UUID uuid) {
        lastStateKeys.remove(uuid);
    }

    public void forget(@Nonnull UUID uuid) {
        lastStateKeys.remove(uuid);
    }
}