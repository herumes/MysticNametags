package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.config.Settings;
import com.mystichorizons.mysticnametags.nameplate.glyph.GlyphAssets;
import com.mystichorizons.mysticnametags.nameplate.glyph.GlyphInfoCompat;
import com.mystichorizons.mysticnametags.util.ColorFormatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class GlyphNameplateManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final GlyphNameplateManager INSTANCE = new GlyphNameplateManager();

    private static final double ANCHOR_Y_OFFSET = 2.25d;
    private static final float YAW_EPSILON = 0.05f;

    private static final Map<Integer, String> RESOLVED_EFFECT_IDS = new ConcurrentHashMap<>();

    public static GlyphNameplateManager get() {
        return INSTANCE;
    }

    private final Map<UUID, RenderState> states = new ConcurrentHashMap<>();

    private GlyphNameplateManager() {}

    public void apply(@Nonnull UUID uuid,
                      @Nonnull World world,
                      @Nonnull Store<EntityStore> store,
                      @Nonnull Ref<EntityStore> playerRef,
                      @Nonnull String formattedText) {

        store.assertThread();

        Settings settings = Settings.get();
        if (!settings.isExperimentalGlyphNameplatesEnabled()) {
            remove(uuid, world, store);
            return;
        }

        String clamped = clampVisibleLength(formattedText, settings.getExperimentalGlyphMaxChars());

        RenderState state = states.computeIfAbsent(uuid, ignored -> new RenderState());
        state.worldName = world.getName();

        boolean needsRebuild = !Objects.equals(state.lastText, clamped);

        if (needsRebuild) {
            rebuild(uuid, world, store, playerRef, state, clamped, settings);
            state.lastText = clamped;
        }

        follow(uuid, world, store, playerRef, state);
    }

    public void remove(@Nonnull UUID uuid,
                       @Nonnull World world,
                       @Nonnull Store<EntityStore> store) {

        store.assertThread();

        RenderState state = states.remove(uuid);
        if (state == null) return;

        despawnAll(store, world.getEntityStore(), state);
    }

    public void remove(@Nonnull UUID uuid, @Nonnull World world) {
        RenderState state = states.remove(uuid);
        if (state == null) {
            return;
        }

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            store.assertThread();
            despawnAll(store, world.getEntityStore(), state);
        });
    }

    public void forget(@Nonnull UUID uuid) {
        states.remove(uuid);
    }

    public void followOnly(@Nonnull World world,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> playerRef,
                           @Nonnull UUID uuid) {
        store.assertThread();
        RenderState state = states.get(uuid);
        if (state == null) return;
        
        follow(uuid, world, store, playerRef, state);
    }

    public boolean hasState(@Nonnull UUID uuid) {
        return states.containsKey(uuid);
    }

    public void clearAllInWorld(@Nonnull World world) {
        final String worldName = world.getName();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            store.assertThread();

            List<UUID> uuids = new ArrayList<>(states.keySet());
            for (UUID uuid : uuids) {
                RenderState state = states.get(uuid);
                if (state == null) continue;
                if (!Objects.equals(worldName, state.worldName)) continue;

                try {
                    remove(uuid, world, store);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    // ---------------------------------------------------------------------
    // Build & follow
    // ---------------------------------------------------------------------

    private void rebuild(@Nonnull UUID uuid,
                         @Nonnull World world,
                         @Nonnull Store<EntityStore> store,
                         @Nonnull Ref<EntityStore> playerRef,
                         @Nonnull RenderState state,
                         @Nonnull String text,
                         @Nonnull Settings settings) {

        despawnAll(store, world.getEntityStore(), state);
        state.glyphRefs.clear();
        state.glyphOffsets.clear();
        state.lastFaceYaw = Float.NaN;

        TransformComponent playerTx = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTx == null) return;

        Vector3d playerPos = playerTx.getTransform().getPosition();
        Vector3f playerRot = playerTx.getTransform().getRotation();
        state.yawNativeLooksLikeDegrees = RotationCompat.looksLikeDegrees(playerRot.getY());

        boolean nativeParenting = MountCompat.isSupported();
        state.isParented = nativeParenting;

        Vector3d anchorPos = new Vector3d(playerPos.getX(), playerPos.getY() + ANCHOR_Y_OFFSET, playerPos.getZ());
        Vector3f anchorRot = new Vector3f(0, playerRot.getY(), 0);

        // 1. Spawn the Parent Anchor
        Holder anchorHolder = EntityStore.REGISTRY.newHolder();
        anchorHolder.putComponent(TransformComponent.getComponentType(), new TransformComponent(anchorPos, anchorRot));
        anchorHolder.putComponent(NetworkId.getComponentType(), new NetworkId(world.getEntityStore().takeNextNetworkId()));
        anchorHolder.putComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID());
        anchorHolder.putComponent(Intangible.getComponentType(), Intangible.INSTANCE);

        if (nativeParenting) {
            // Anchor perfectly tracks Player. Offset is purely vertical so it never orbits.
            MountCompat.mount(anchorHolder, playerRef, new Vector3f(0, (float)ANCHOR_Y_OFFSET, 0));
        }

        state.anchorRef = store.addEntity(anchorHolder, AddReason.SPAWN);

        List<ColoredChar> chars = SimpleColorParser.parse(text);

        int hardCap = settings.getExperimentalGlyphMaxEntitiesPerPlayer();
        int spawnedCount = 0;

        double charWidth = GlyphInfoCompat.CHAR_WIDTH * state.scale;
        float modelScale = GlyphInfoCompat.BASE_MODEL_SCALE * (float) state.scale;

        int visibleCount = 0;
        for (ColoredChar cc : chars) {
            if (cc.ch == '\n' || cc.ch == '\r') continue;
            visibleCount++;
        }

        float fallbackFaceYaw = RotationCompat.addYawNative(playerRot.getY(), 180f, state.yawNativeLooksLikeDegrees);
        double fallbackYawRadTrig = RotationCompat.toTrigYawRad(fallbackFaceYaw, state.yawNativeLooksLikeDegrees);
        
        // Setup initial rotation trig 
        double rightX = Math.cos(fallbackYawRadTrig);
        double rightZ = -Math.sin(fallbackYawRadTrig);

        int logicalIndex = 0;
        for (ColoredChar cc : chars) {
            char ch = cc.ch;
            if (ch == '\n' || ch == '\r') continue;

            // CORRECTED OFFSET MATH: Reads Left-to-Right naturally facing the viewer.
            double offset = ((visibleCount - 1) / 2.0d - logicalIndex) * charWidth;
            logicalIndex++;

            if (ch == ' ') continue;
            if (spawnedCount >= hardCap) break;
            if (!GlyphInfoCompat.isSupported(ch)) continue;

            int rgbQuant = TintPaletteCompat.quantizeRgb(cc.color);

            Vector3d gPos = new Vector3d(
                    anchorPos.getX() + rightX * offset,
                    anchorPos.getY(),
                    anchorPos.getZ() + rightZ * offset
            );
            
            // Models face +Z natively. We spawn them pointing straight forward relative to the Anchor.
            Vector3f gRot = new Vector3f(0, fallbackFaceYaw, 0);

            // 2. Chained Mount: Glyphs mount directly to the Anchor.
            Ref<EntityStore> glyphRef = spawnGlyph(
                    store,
                    world.getEntityStore(),
                    ch,
                    rgbQuant,
                    gPos,
                    gRot,
                    modelScale,
                    nativeParenting ? state.anchorRef : null,
                    nativeParenting ? new Vector3f((float) offset, 0f, 0f) : null
            );

            if (glyphRef != null) {
                state.glyphRefs.add(glyphRef);
                state.glyphOffsets.add(offset);
                spawnedCount++;
            }
        }
    }

    private void follow(@Nonnull UUID uuid,
                        @Nonnull World world,
                        @Nonnull Store<EntityStore> store,
                        @Nonnull Ref<EntityStore> playerRef,
                        @Nonnull RenderState state) {

        TransformComponent playerTx = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTx == null) return;

        Vector3d playerPos = playerTx.getTransform().getPosition();
        Vector3f playerRot = playerTx.getTransform().getRotation();

        boolean looksDegrees = state.yawNativeLooksLikeDegrees != null
                ? state.yawNativeLooksLikeDegrees
                : RotationCompat.looksLikeDegrees(playerRot.getY());

        if (state.yawNativeLooksLikeDegrees == null) {
            state.yawNativeLooksLikeDegrees = looksDegrees;
        }

        PlayerRef nearestViewer = findNearestViewer(world, uuid, playerPos, store);
        float faceYawNative = playerRot.getY();

        if (nearestViewer != null) {
            TransformComponent nearTx = store.getComponent(nearestViewer.getReference(), TransformComponent.getComponentType());
            if (nearTx != null) {
                Vector3d nearPos = nearTx.getTransform().getPosition();
                
                // Calculates the exact angle from the Nameplate to the Viewer
                double dx = nearPos.getX() - playerPos.getX();
                double dz = nearPos.getZ() - playerPos.getZ();
                double angleRad = Math.atan2(dx, dz);
                
                // NOTE: If the models are still backwards (e.g. "[" looks like "]"), 
                // you can uncomment the + Math.PI here to flip them 180 degrees.
                angleRad += Math.PI; 
                
                faceYawNative = looksDegrees ? (float) Math.toDegrees(angleRad) : (float) angleRad;
            }
        } else {
            faceYawNative = RotationCompat.addYawNative(playerRot.getY(), 180f, looksDegrees);
        }

        if (nearlyEqual(state.lastFaceYaw, faceYawNative)) {
            return;
        }

        state.lastFaceYaw = faceYawNative;

        // =========================================================================================
        // ZERO-LAG SMOOTH INTERPOLATION: 
        // We completely eradicated teleportPosition(). We strictly mutate the internal coordinates
        // so the client beautifully interpolates any network ticks without violently snapping.
        // Because the Glyphs are children of the Anchor, turning the Anchor spins them all together!
        // =========================================================================================
        
        if (state.anchorRef != null && state.anchorRef.isValid()) {
            TransformComponent anchorTx = store.getComponent(state.anchorRef, TransformComponent.getComponentType());
            if (anchorTx != null) {
                // Keep server chunk tracking perfectly aligned with the client's MountedComponent
                anchorTx.getPosition().setX(playerPos.getX());
                anchorTx.getPosition().setY(playerPos.getY() + ANCHOR_Y_OFFSET);
                anchorTx.getPosition().setZ(playerPos.getZ());
                
                // Billboard to face the camera
                anchorTx.setRotation(new Vector3f(0f, faceYawNative, 0f));
                StoreComponentUpdateCompat.update(store, state.anchorRef, anchorTx);
            }
        }

        double yawRadTrig = RotationCompat.toTrigYawRad(faceYawNative, looksDegrees);
        double rightX = Math.cos(yawRadTrig);
        double rightZ = -Math.sin(yawRadTrig);

        for (int i = 0; i < state.glyphRefs.size(); i++) {
            Ref<EntityStore> glyphRef = state.glyphRefs.get(i);
            if (glyphRef == null || !glyphRef.isValid()) continue;

            TransformComponent glyphTx = store.getComponent(glyphRef, TransformComponent.getComponentType());
            if (glyphTx != null) {
                double along = state.glyphOffsets.get(i);
                
                // Set perfectly matched mathematical positions to prevent any network fighting
                glyphTx.getPosition().setX(playerPos.getX() + rightX * along);
                glyphTx.getPosition().setY(playerPos.getY() + ANCHOR_Y_OFFSET);
                glyphTx.getPosition().setZ(playerPos.getZ() + rightZ * along);
                
                glyphTx.setRotation(new Vector3f(0f, faceYawNative, 0f));
                StoreComponentUpdateCompat.update(store, glyphRef, glyphTx);
            }
        }
    }

    private void despawnAll(@Nonnull Store<EntityStore> store,
                            @Nonnull EntityStore entityStore,
                            @Nonnull RenderState state) {

        if (state.anchorRef != null && state.anchorRef.isValid()) {
            try {
                EntityRemoveCompat.remove(store, entityStore, state.anchorRef);
            } catch (Throwable ignored) {}
            state.anchorRef = null;
        }

        List<Ref<EntityStore>> refs = state.glyphRefs;
        for (int i = 0; i < refs.size(); i++) {
            Ref<EntityStore> ref = refs.get(i);
            if (ref == null || !ref.isValid()) continue;
            try {
                EntityRemoveCompat.remove(store, entityStore, ref);
            } catch (Throwable ignored) {}
        }
    }

    private PlayerRef findNearestViewer(World world, UUID targetUuid, Vector3d targetPos, Store<EntityStore> store) {
        PlayerRef nearest = null;
        double minDistSq = 400.0;
        for (PlayerRef other : world.getPlayerRefs()) {
            if (other == null || other.getUuid() == null || other.getUuid().equals(targetUuid)) continue;
            Ref<EntityStore> otherRef = other.getReference();
            if (otherRef == null || !otherRef.isValid()) continue;
            
            try {
                TransformComponent otherTx = store.getComponent(otherRef, TransformComponent.getComponentType());
                if (otherTx == null) continue;
                
                double distSq = otherTx.getTransform().getPosition().distanceSquaredTo(targetPos);
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    nearest = other;
                }
            } catch (Throwable ignored) {}
        }
        return nearest;
    }

    // ---------------------------------------------------------------------
    // Spawning
    // ---------------------------------------------------------------------

    @Nullable
    private Ref<EntityStore> spawnGlyph(@Nonnull Store<EntityStore> store,
                                        @Nonnull EntityStore entityStore,
                                        char ch,
                                        int rgbQuant,
                                        @Nonnull Vector3d pos,
                                        @Nonnull Vector3f rot,
                                        float scale,
                                        @Nullable Ref<EntityStore> parentRef,
                                        @Nullable Vector3f mountOffset) {

        try {
            Holder holder = EntityStore.REGISTRY.newHolder();

            String[] candidates = GlyphInfoCompat.getModelAssetIdCandidates(ch);
            if (candidates == null || candidates.length == 0) return null;

            ModelAsset asset = null;
            String usedModelId = null;

            for (String id : candidates) {
                if (id == null || id.isEmpty()) continue;
                asset = (ModelAsset) ModelAsset.getAssetMap().getAsset(id);
                if (asset != null) { usedModelId = id; break; }
            }

            if (asset == null && candidates.length > 0) {
                String shortName = candidates[0];
                int colon = shortName.lastIndexOf(':');
                if (colon >= 0) shortName = shortName.substring(colon + 1);
                shortName = shortName.toLowerCase(Locale.ROOT);

                for (Map.Entry<String, ?> entry : ModelAsset.getAssetMap().getAssetMap().entrySet()) {
                    String key = entry.getKey();
                    if (key != null && key.toLowerCase(Locale.ROOT).endsWith(shortName)) {
                        asset = (ModelAsset) entry.getValue();
                        usedModelId = key;
                        break;
                    }
                }
            }

            if (asset == null || usedModelId == null) return null;

            Model model = Model.createScaledModel(asset, scale);
            holder.putComponent(TransformComponent.getComponentType(), new TransformComponent(pos, rot));
            holder.putComponent(ModelComponent.getComponentType(), new ModelComponent(model));

            try {
                Model.ModelReference staticRef = new Model.ModelReference(usedModelId, scale, null, true);
                holder.putComponent(PersistentModel.getComponentType(), new PersistentModel(staticRef));
            } catch (Throwable ignored) {
                holder.putComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            }

            try {
                holder.putComponent(
                        com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent.getComponentType(),
                        new com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent(scale)
                );
            } catch (Throwable ignored) {}

            holder.putComponent(NetworkId.getComponentType(), new NetworkId(entityStore.takeNextNetworkId()));
            holder.putComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID());
            holder.putComponent(Intangible.getComponentType(), Intangible.INSTANCE);

            try { holder.ensureComponent(EntityModule.get().getVisibleComponentType()); } catch (Throwable ignored) {}
            try { holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType()); } catch (Throwable ignored) {}

            holder.putComponent(EffectControllerComponent.getComponentType(), new EffectControllerComponent());

            // --- NATIVE ECS MOUNT LINKAGE ---
            if (parentRef != null && mountOffset != null) {
                MountCompat.mount(holder, parentRef, mountOffset);
            }

            Ref<EntityStore> spawned = store.addEntity(holder, AddReason.SPAWN);

            try {
                EffectControllerComponent spawnedEffects = store.getComponent(spawned, EffectControllerComponent.getComponentType());
                EntityEffect tint = null;
                String finalEffectId = RESOLVED_EFFECT_IDS.get(rgbQuant);

                if (finalEffectId != null) {
                    tint = (EntityEffect) EntityEffect.getAssetMap().getAsset(finalEffectId);
                } else {
                    String attemptId = GlyphAssets.tintEffectId(rgbQuant);
                    tint = (EntityEffect) EntityEffect.getAssetMap().getAsset(attemptId);
                    if (tint == null) {
                        attemptId = attemptId.toLowerCase(Locale.ROOT);
                        tint = (EntityEffect) EntityEffect.getAssetMap().getAsset(attemptId);
                    }
                    if (tint == null) {
                        String shortName = "httint_" + String.format("%06x", rgbQuant);
                        for (Map.Entry<String, ?> entry : EntityEffect.getAssetMap().getAssetMap().entrySet()) {
                            String key = entry.getKey();
                            if (key != null && key.toLowerCase(Locale.ROOT).contains(shortName)) {
                                attemptId = key;
                                tint = (EntityEffect) entry.getValue();
                                break;
                            }
                        }
                    }
                    if (tint != null) {
                        RESOLVED_EFFECT_IDS.put(rgbQuant, attemptId);
                        finalEffectId = attemptId;
                    }
                }

                if (tint != null && spawnedEffects != null) {
                    spawnedEffects.addEffect(spawned, tint, (float) Integer.MAX_VALUE, OverlapBehavior.OVERWRITE, store);
                }
            } catch (Throwable ignored) {}

            return spawned;
        } catch (Throwable t) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Formatting parsing + clamping
    // ---------------------------------------------------------------------

    private static String clampVisibleLength(String text, int maxVisible) {
        if (text == null || text.isEmpty()) return "";
        text = ColorFormatter.miniToLegacy(text);
        StringBuilder out = new StringBuilder(text.length());
        int visible = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if ((c == '&' || c == '§') && i + 7 < text.length() && text.charAt(i + 1) == '#') {
                out.append(text, i, i + 8); i += 7; continue;
            }

            if ((c == '&' || c == '§') && i + 13 < text.length() && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')) {
                out.append(text, i, i + 14); i += 13; continue;
            }

            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                if ("0123456789abcdefABCDEFklmnorKLMNORxX".indexOf(code) >= 0) {
                    out.append(c).append(code); i += 1; continue;
                }
            }

            if (c == '<') {
                int end = text.indexOf('>', i);
                if (end > i) {
                    out.append(text, i, end + 1); i = end; continue;
                }
            }

            out.append(c);
            if (c != '\n' && c != '\r') {
                visible++;
                if (visible >= maxVisible) break;
            }
        }
        return out.toString();
    }

    private static boolean nearlyEqual(float a, float b) { return Math.abs(a - b) < YAW_EPSILON; }

    private static final class RenderState {
        String lastText = null;
        double scale = 1.0d;
        String worldName = null;
        Boolean yawNativeLooksLikeDegrees = null;
        boolean isParented = false;
        Ref<EntityStore> anchorRef = null;
        double lastAnchorX = Double.NaN, lastAnchorY = Double.NaN, lastAnchorZ = Double.NaN;
        float lastFaceYaw = Float.NaN;
        final List<Ref<EntityStore>> glyphRefs = new ArrayList<>();
        final List<Double> glyphOffsets = new ArrayList<>();
    }

    private static final class ColoredChar {
        final char ch;
        final Color color;
        ColoredChar(char ch, Color color) { this.ch = ch; this.color = color; }
    }

    private static final class SimpleColorParser {

        private static final Map<Character, Color> LEGACY_COLORS = new HashMap<>();

        static {
            LEGACY_COLORS.put('0', Color.BLACK);
            LEGACY_COLORS.put('1', new Color(0x00, 0x00, 0xA0));
            LEGACY_COLORS.put('2', new Color(0x00, 0xA0, 0x00));
            LEGACY_COLORS.put('3', new Color(0x00, 0xA0, 0xA0));
            LEGACY_COLORS.put('4', new Color(0xA0, 0x00, 0x00));
            LEGACY_COLORS.put('5', new Color(0xA0, 0x00, 0xA0));
            LEGACY_COLORS.put('6', new Color(0xFF, 0xA0, 0x00));
            LEGACY_COLORS.put('7', new Color(0xA0, 0xA0, 0xA0));
            LEGACY_COLORS.put('8', new Color(0x60, 0x60, 0x60));
            LEGACY_COLORS.put('9', new Color(0x60, 0x60, 0xFF));
            LEGACY_COLORS.put('a', new Color(0x60, 0xFF, 0x60));
            LEGACY_COLORS.put('b', new Color(0x60, 0xFF, 0xFF));
            LEGACY_COLORS.put('c', new Color(0xFF, 0x60, 0x60));
            LEGACY_COLORS.put('d', new Color(0xFF, 0x60, 0xFF));
            LEGACY_COLORS.put('e', new Color(0xFF, 0xFF, 0x60));
            LEGACY_COLORS.put('f', Color.WHITE);
        }

        static List<ColoredChar> parse(String text) {
            List<ColoredChar> out = new ArrayList<>();
            if (text == null || text.isEmpty()) return out;

            text = ColorFormatter.miniToLegacy(text);
            Color current = Color.WHITE;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if ((c == '&' || c == '§') && i + 7 < text.length() && text.charAt(i + 1) == '#') {
                    Color parsed = GlyphAssets.tryParseHex6(text.substring(i + 2, i + 8));
                    if (parsed != null) current = parsed;
                    i += 7;
                    continue;
                }

                if ((c == '&' || c == '§') && i + 13 < text.length() && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')) {
                    StringBuilder hex = new StringBuilder(6);
                    boolean valid = true;
                    for (int j = i + 2; j <= i + 12; j += 2) {
                        if (j + 1 >= text.length()) { valid = false; break; }
                        char marker = text.charAt(j);
                        char digit = text.charAt(j + 1);
                        if ((marker != '&' && marker != '§') || !isHexDigit(digit)) { valid = false; break; }
                        hex.append(digit);
                    }
                    if (valid) {
                        Color parsed = GlyphAssets.tryParseHex6(hex.toString());
                        if (parsed != null) current = parsed;
                        i += 13;
                        continue;
                    }
                }

                if ((c == '&' || c == '§') && i + 1 < text.length()) {
                    char code = Character.toLowerCase(text.charAt(i + 1));
                    if (LEGACY_COLORS.containsKey(code)) { current = LEGACY_COLORS.get(code); i += 1; continue; }
                    if (code == 'r') { current = Color.WHITE; i += 1; continue; }
                    if ("klmno".indexOf(code) >= 0) { i += 1; continue; }
                }

                if (c == '<') {
                    int end = text.indexOf('>', i);
                    if (end > i) {
                        String tag = text.substring(i + 1, end).trim().toLowerCase(Locale.ROOT);
                        if (tag.startsWith("#") && tag.length() == 7) {
                            Color parsed = GlyphAssets.tryParseHex6(tag.substring(1));
                            if (parsed != null) current = parsed;
                            i = end; continue;
                        }
                        if (tag.equals("/") || tag.equals("reset")) {
                            current = Color.WHITE;
                            i = end; continue;
                        }
                    }
                }

                out.add(new ColoredChar(c, current));
            }
            return out;
        }

        private static boolean isHexDigit(char c) {
            return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
        }
    }

    // ---------------------------------------------------------------------
    // Native Engine ECS Injection - Mount System 
    // ---------------------------------------------------------------------

    private static final class MountCompat {
        private static Class<?> mountedClass;
        private static Constructor<?> mountedConstructor;
        private static Object defaultController;
        private static Method getComponentTypeMethod;
        private static Method putComponentMethod;

        static {
            try {
                mountedClass = Class.forName("com.hypixel.hytale.builtin.mounts.MountedComponent");
                Class<?> controllerClass = Class.forName("com.hypixel.hytale.protocol.MountController");
                
                for (Object c : controllerClass.getEnumConstants()) {
                    String name = c.toString().toUpperCase();
                    if (name.equals("NONE")) {
                        defaultController = c;
                        break;
                    }
                }
                if (defaultController == null) defaultController = controllerClass.getEnumConstants()[0];

                mountedConstructor = mountedClass.getConstructor(Ref.class, Vector3f.class, controllerClass);
                getComponentTypeMethod = mountedClass.getMethod("getComponentType");
                
                for (Method m : Holder.class.getMethods()) {
                    if (m.getName().equals("putComponent") && m.getParameterCount() == 2) {
                        putComponentMethod = m;
                        break;
                    }
                }
            } catch (Throwable ignored) {}
        }

        static boolean isSupported() {
            return mountedClass != null && mountedConstructor != null && putComponentMethod != null;
        }

        static boolean mount(Holder holder, Ref<EntityStore> target, Vector3f offset) {
            if (!isSupported()) return false;
            try {
                Object comp = mountedConstructor.newInstance(target, offset, defaultController);
                Object compType = getComponentTypeMethod.invoke(null);
                putComponentMethod.invoke(holder, compType, comp);
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
    }

    private static final class EntityRemoveCompat {
        @SuppressWarnings({"unchecked", "rawtypes"})
        static void remove(@Nonnull Store<EntityStore> store, @Nonnull EntityStore entityStore, @Nonnull Ref<EntityStore> ref) {
            try {
                Class<?> rr = Class.forName("com.hypixel.hytale.component.RemoveReason");
                Object remove = Enum.valueOf((Class<? extends Enum>) rr.asSubclass(Enum.class), "REMOVE");

                if (tryInvoke(store, "removeEntity", new Class[]{Ref.class, rr}, ref, remove)) return;
                if (tryInvoke(entityStore, "removeEntity", new Class[]{Ref.class, rr}, ref, remove)) return;
            } catch (Throwable ignored) {}

            if (tryInvoke(store, "removeEntity", new Class[]{Ref.class, Object.class}, ref, null)) return;
            if (tryInvoke(store, "removeEntity", new Class[]{Ref.class}, ref)) return;
            if (tryInvoke(store, "deleteEntity", new Class[]{Ref.class}, ref)) return;
            if (tryInvoke(entityStore, "removeEntity", new Class[]{Ref.class, Object.class}, ref, null)) return;
            if (tryInvoke(entityStore, "removeEntity", new Class[]{Ref.class}, ref)) return;
        }
        private static boolean tryInvoke(Object target, String name, Class<?>[] sig, Object... args) {
            try { Method m = target.getClass().getMethod(name, sig); m.invoke(target, args); return true; } catch (Throwable ignored) { return false; }
        }
    }

    private static final class StoreComponentUpdateCompat {
        private static volatile boolean cached = false;
        private static Method mUpdateComponent3;
        private static Method mSetComponent3;
        private static Method mPutComponent3;

        static void update(Store<EntityStore> store, Ref<EntityStore> ref, TransformComponent tx) {
            tryInit(store);
            try {
                Object type = TransformComponent.getComponentType();
                if (mUpdateComponent3 != null) mUpdateComponent3.invoke(store, ref, type, tx);
                else if (mSetComponent3 != null) mSetComponent3.invoke(store, ref, type, tx);
                else if (mPutComponent3 != null) mPutComponent3.invoke(store, ref, type, tx);
            } catch (Throwable ignored) {}
        }
        private static void tryInit(Store<EntityStore> store) {
            if (cached) return;
            synchronized (StoreComponentUpdateCompat.class) {
                if (cached) return;
                for (Method m : store.getClass().getMethods()) {
                    if (m.getParameterCount() == 3) {
                        if (m.getName().equals("updateComponent")) mUpdateComponent3 = m;
                        else if (m.getName().equals("setComponent")) mSetComponent3 = m;
                        else if (m.getName().equals("putComponent")) mPutComponent3 = m;
                    }
                }
                cached = true;
            }
        }
    }

    private static final class RotationCompat {
        static boolean looksLikeDegrees(float yaw) { return Math.abs(yaw) > 6.4f; }
        static double toTrigYawRad(float yawNative, boolean looksDegrees) { return looksDegrees ? Math.toRadians(yawNative) : yawNative; }
        static float addYawNative(float yawNative, float addDegrees, boolean looksDegrees) { return (float) (looksDegrees ? yawNative + addDegrees : yawNative + Math.toRadians(addDegrees)); }
    }

    private static final class TintPaletteCompat {

        private static final int[] ALLOWED_VALUES = {
            0x00, 0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0, 0xE0, 0xFF
        };

        private static final int[] LUT = new int[256];

        static {
            for (int i = 0; i < 256; i++) {
                int closest = 0;
                int minDiff = Integer.MAX_VALUE;
                for (int val : ALLOWED_VALUES) {
                    int diff = Math.abs(i - val);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closest = val;
                    }
                }
                LUT[i] = closest;
            }
        }

        static int quantizeRgb(Color c) {
            int r = LUT[c.getRed() & 0xFF];
            int g = LUT[c.getGreen() & 0xFF];
            int b = LUT[c.getBlue() & 0xFF];
            return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }
    }
}