package com.mystichorizons.mysticnametags.nameplate;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.config.Settings;
import com.mystichorizons.mysticnametags.nameplate.glyph.GlyphAssets;
import com.mystichorizons.mysticnametags.nameplate.glyph.GlyphInfoCompat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class GlyphNameplateManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final GlyphNameplateManager INSTANCE = new GlyphNameplateManager();

    public static GlyphNameplateManager get() {
        return INSTANCE;
    }

    private final Map<UUID, RenderState> states = new ConcurrentHashMap<>();

    // Position above head
    private static final double ANCHOR_Y_OFFSET = 2.25;

    private GlyphNameplateManager() {}

    /**
     * Apply or update a glyph nameplate for a player.
     * MUST be called on the world thread.
     */
    public void apply(@Nonnull UUID uuid,
                      @Nonnull World world,
                      @Nonnull Store<EntityStore> store,
                      @Nonnull Ref<EntityStore> playerRef,
                      @Nonnull String formattedText) {

        store.assertThread();

        if (!Settings.get().isExperimentalGlyphNameplatesEnabled()) {
            remove(uuid, world, store);
            return;
        }

        String clamped = clampVisibleLength(formattedText, Settings.get().getExperimentalGlyphMaxChars());

        RenderState state = states.computeIfAbsent(uuid, u -> new RenderState());

        // Rebuild only if text changed
        if (!Objects.equals(state.lastText, clamped)) {
            rebuild(world, store, playerRef, state, clamped);
            state.lastText = clamped;
        }

        // Always follow on apply
        follow(store, playerRef, state);
    }

    /**
     * Remove glyph entities for this player.
     * MUST be called on the world thread.
     */
    public void remove(@Nonnull UUID uuid,
                       @Nonnull World world,
                       @Nonnull Store<EntityStore> store) {

        store.assertThread();

        RenderState state = states.remove(uuid);
        if (state == null) return;

        despawnAll(store, world.getEntityStore(), state);
    }

    /**
     * Remove state cache when player fully leaves (no store access).
     */
    public void forget(@Nonnull UUID uuid) {
        states.remove(uuid);
    }

    /**
     * Follow/update transforms only (cheap).
     * MUST be called on the world thread.
     */
    public void followOnly(@Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> playerRef,
                           @Nonnull UUID uuid) {
        store.assertThread();
        RenderState state = states.get(uuid);
        if (state == null) return;
        follow(store, playerRef, state);
    }

    private void rebuild(@Nonnull World world,
                         @Nonnull Store<EntityStore> store,
                         @Nonnull Ref<EntityStore> playerRef,
                         @Nonnull RenderState state,
                         @Nonnull String text) {

        despawnAll(store, world.getEntityStore(), state);
        state.glyphRefs.clear();
        state.glyphOffsets.clear();

        TransformComponent playerTx = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTx == null) return;

        Vector3d p = playerTx.getTransform().getPosition();
        Vector3f playerRot = playerTx.getTransform().getRotation();
        
        float faceYaw = playerRot.getY() + 180f + 90f;
        double faceYawRad = Math.toRadians(faceYaw);

        double cos = Math.cos(faceYawRad);
        double sin = Math.sin(faceYawRad);

        Vector3d anchor = new Vector3d(p.getX(), p.getY() + ANCHOR_Y_OFFSET, p.getZ());

        List<ColoredChar> chars = SimpleColorParser.parse(text);

        double charWidth = GlyphInfoCompat.CHAR_WIDTH * state.scale;
        float modelScale = GlyphInfoCompat.BASE_MODEL_SCALE * (float) state.scale;

        int count = 0;
        for (ColoredChar cc : chars) {
            if (cc.ch == '\n' || cc.ch == '\r') continue;
            count++;
        }

        double totalWidth = count * charWidth;
        double start = -totalWidth / 2.0;

        int idx = 0;
        for (ColoredChar cc : chars) {
            char ch = cc.ch;
            if (ch == '\n' || ch == '\r') continue;

            double offset = start + ((idx + 0.5) * charWidth);
            idx++;

            if (ch == ' ') continue;

            double newX = offset * cos;
            double newZ = offset * sin;

            Vector3d charPos = new Vector3d(
                    anchor.getX() + newX,
                    anchor.getY(),
                    anchor.getZ() + newZ
            );

            // Get gradient info from the color (instead of effect)
            GradientInfo gradient = findClosestGradient(cc.color);

            Ref<EntityStore> glyphRef = spawnGlyph(
                    store,
                    world.getEntityStore(),
                    ch,
                    gradient.set,
                    gradient.id,
                    charPos,
                    new Vector3f(0, faceYaw, 0),
                    modelScale
            );

            if (glyphRef != null) {
                state.glyphRefs.add(glyphRef);
                state.glyphOffsets.add(offset);
            }
        }
    }

    private void follow(@Nonnull Store<EntityStore> store,
                        @Nonnull Ref<EntityStore> playerRef,
                        @Nonnull RenderState state) {

        TransformComponent playerTx = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTx == null) return;

        Vector3d p = playerTx.getTransform().getPosition();
        Vector3f r = playerTx.getTransform().getRotation();

        float faceYaw = r.getY() + 180f;
        double faceYawRad = Math.toRadians(faceYaw);

        double cos = Math.cos(faceYawRad);
        double sin = Math.sin(faceYawRad);

        Vector3d anchor = new Vector3d(p.getX(), p.getY() + ANCHOR_Y_OFFSET, p.getZ());

        for (int i = 0; i < state.glyphRefs.size(); i++) {
            Ref<EntityStore> glyphRef = state.glyphRefs.get(i);
            if (glyphRef == null || !glyphRef.isValid()) continue;

            double offset = state.glyphOffsets.get(i);

            double newX = offset * cos;
            double newZ = offset * sin;

            Vector3d pos = new Vector3d(
                    anchor.getX() + newX,
                    anchor.getY(),
                    anchor.getZ() + newZ
            );

            TransformComponent tx = store.getComponent(glyphRef, TransformComponent.getComponentType());
            if (tx == null) continue;

            TransformComponentCompat.apply(store, tx, pos, new Vector3f(0f, faceYaw, 0f));
        }
    }

    private void despawnAll(@Nonnull Store<EntityStore> store,
                            @Nonnull EntityStore entityStore,
                            @Nonnull RenderState state) {

        for (Ref<EntityStore> ref : state.glyphRefs) {
            if (ref == null || !ref.isValid()) continue;
            try {
                EntityRemoveCompat.remove(store, entityStore, ref);
            } catch (Throwable ignored) {}
        }
    }

    public void clearAllInWorld(@Nonnull World world) {
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            store.assertThread();

            List<UUID> uuids = new ArrayList<>(states.keySet());
            for (UUID uuid : uuids) {
                try {
                    remove(uuid, world, store);
                } catch (Throwable ignored) {}
            }
        });
    }

    // ---------------------------------------------------------------------
    // Spawning (Option B: lowercase-first + engine-case fallback)
    // ---------------------------------------------------------------------

    @Nullable
    private Ref<EntityStore> spawnGlyph(@Nonnull Store<EntityStore> store,
                                        @Nonnull EntityStore entityStore,
                                        char ch,
                                        @Nullable String gradientSet,
                                        @Nullable String gradientId,
                                        @Nonnull Vector3d pos,
                                        @Nonnull Vector3f rot,
                                        float scale) {

        try {
            Holder holder = EntityStore.REGISTRY.newHolder();

            String[] candidates = GlyphInfoCompat.getModelAssetIdCandidates(ch);
            if (candidates == null || candidates.length == 0) return null;

            ModelAsset asset = null;
            String usedModelId = null;

            for (String id : candidates) {
                if (id == null || id.isEmpty()) continue;
                asset = (ModelAsset) ModelAsset.getAssetMap().getAsset(id);
                if (asset != null) {
                    usedModelId = id;
                    break;
                }
            }

            if (asset == null) {
                String shortName = candidates[0];
                if (shortName.contains(":")) shortName = shortName.substring(shortName.lastIndexOf(':') + 1);
                shortName = shortName.toLowerCase(Locale.ROOT);

                for (Map.Entry<String, ?> entry : ModelAsset.getAssetMap().getAssetMap().entrySet()) {
                    if (entry.getKey().toLowerCase(Locale.ROOT).endsWith(shortName)) {
                        asset = (ModelAsset) entry.getValue();
                        usedModelId = entry.getKey();
                        break;
                    }
                }
            }

            if (asset == null) {
                return null;
            }

            Model model = Model.createScaledModel(asset, scale);

            // Apply gradient if we have it (using reflection as fallback)
            if (gradientSet != null && gradientId != null && model != null) {
                try {
                    Field fs = Model.class.getDeclaredField("gradientSet");
                    fs.setAccessible(true);
                    fs.set(model, gradientSet);
                    
                    Field fi = Model.class.getDeclaredField("gradientId");
                    fi.setAccessible(true);
                    fi.set(model, gradientId);
                } catch (Throwable ignored) {
                    LOGGER.at(Level.FINE).log("[MysticNameTags] Could not set gradient via reflection, model may be white.");
                }
            }
            
            // Create static reference for persistence
            com.hypixel.hytale.server.core.asset.type.model.config.Model.ModelReference staticRef = 
                new com.hypixel.hytale.server.core.asset.type.model.config.Model.ModelReference(usedModelId, scale, null, true);

            holder.putComponent(TransformComponent.getComponentType(), new TransformComponent(pos, rot));
            holder.putComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.putComponent(PersistentModel.getComponentType(), new PersistentModel(staticRef));
            holder.putComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale));
            holder.putComponent(NetworkId.getComponentType(), new NetworkId(entityStore.takeNextNetworkId()));

            UUIDComponent uuidComp = UUIDComponent.randomUUID();
            holder.putComponent(UUIDComponent.getComponentType(), uuidComp);
            holder.putComponent(Intangible.getComponentType(), Intangible.INSTANCE);

            // Ensure required components for visibility
            holder.ensureComponent(EntityModule.get().getVisibleComponentType());
            holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

            Ref<EntityStore> spawned = store.addEntity(holder, AddReason.SPAWN);
            return spawned;

        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t).log("[MysticNameTags] spawnGlyph failed for character " + ch);
            return null;
        }
    }

    public void remove(@Nonnull UUID uuid, @Nonnull World world) {
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            remove(uuid, world, store);
        });
    }

    // ---------------------------------------------------------------------
    // Formatting parsing + clamping
    // ---------------------------------------------------------------------

    private static String clampVisibleLength(String text, int maxVisible) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder out = new StringBuilder(text.length());
        int visible = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if ((c == '§' || c == '&') && i + 13 < text.length() && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')) {
                out.append(text, i, i + 14);
                i += 13;
                continue;
            }

            if ((c == '§' || c == '&') && i + 7 < text.length() && text.charAt(i + 1) == '#') {
                out.append(text, i, i + 8);
                i += 7;
                continue;
            }

            if ((c == '§' || c == '&') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdefklmnor".indexOf(code) != -1) {
                    out.append(text, i, i + 2);
                    i += 1;
                    continue;
                }
            }

            if (c == '<') {
                int end = text.indexOf('>', i);
                if (end > i) {
                    out.append(text, i, end + 1);
                    i = end;
                    continue;
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

    private static final class RenderState {
        String lastText = null;
        double scale = 1.0;

        final List<Ref<EntityStore>> glyphRefs = new ArrayList<>();
        final List<Double> glyphOffsets = new ArrayList<>();
    }

    private static final class ColoredChar {
        final char ch;
        final Color color;
        ColoredChar(char ch, Color color) { this.ch = ch; this.color = color; }
    }

    private static final class GradientInfo {
        final String hex;
        final String set;
        final String id;
        final int r, g, b;

        GradientInfo(String hex, String set, String id) {
            this.hex = hex; this.set = set; this.id = id;
            this.r = Integer.parseInt(hex.substring(1, 3), 16);
            this.g = Integer.parseInt(hex.substring(3, 5), 16);
            this.b = Integer.parseInt(hex.substring(5, 7), 16);
        }
    }

    private static final GradientInfo[] GRADIENTS = {
        new GradientInfo("#000000", "Flashy_Synthetic", "Black"),
        new GradientInfo("#0000AA", "Hair", "BlueDark"),
        new GradientInfo("#00AA00", "Colored_Cotton", "Green"),
        new GradientInfo("#00AAAA", "Hair", "Turquoise"),
        new GradientInfo("#AA0000", "Eyes_Gradient", "RedDark"),
        new GradientInfo("#AA00AA", "Hair", "Lavender"),
        new GradientInfo("#FFAA00", "Hair", "Copper"),
        new GradientInfo("#AAAAAA", "Hair", "Black"),
        new GradientInfo("#555555", "Flashy_Synthetic", "Black"),
        new GradientInfo("#5555FF", "Fantasy_Cotton_Dark", "Blue"),
        new GradientInfo("#55FF55", "Colored_Cotton", "Green"),
        new GradientInfo("#55FFFF", "Hair", "Turquoise"),
        new GradientInfo("#FF5555", "Eyes_Gradient", "RedDark"),
        new GradientInfo("#FF55FF", "Hair", "Pink"),
        new GradientInfo("#FFFF55", "Hair", "Blond"),
        new GradientInfo("#FFFFFF", "Colored_Cotton", "White"),
        new GradientInfo("#FFA500", "Hair", "Copper"),
        new GradientInfo("#FFC0CB", "Hair", "Pink"),
        new GradientInfo("#A52A2A", "Hair", "BrownDark"),
        new GradientInfo("#F5F5DC", "Hair", "BlondCaramel")
    };

    private static GradientInfo findClosestGradient(Color c) {
        GradientInfo closest = GRADIENTS[15]; // Default White
        double minDistance = Double.MAX_VALUE;
        for (GradientInfo g : GRADIENTS) {
            double dist = Math.sqrt(Math.pow(c.getRed() - g.r, 2) + Math.pow(c.getGreen() - g.g, 2) + Math.pow(c.getBlue() - g.b, 2));
            if (dist < minDistance) {
                minDistance = dist;
                closest = g;
            }
        }
        return closest;
    }

    private static final class SimpleColorParser {
        private static final Map<Character, Color> LEGACY_COLORS = new HashMap<>();
        static {
            LEGACY_COLORS.put('0', new Color(0, 0, 0));
            LEGACY_COLORS.put('1', new Color(0, 0, 170));
            LEGACY_COLORS.put('2', new Color(0, 170, 0));
            LEGACY_COLORS.put('3', new Color(0, 170, 170));
            LEGACY_COLORS.put('4', new Color(170, 0, 0));
            LEGACY_COLORS.put('5', new Color(170, 0, 170));
            LEGACY_COLORS.put('6', new Color(255, 170, 0));
            LEGACY_COLORS.put('7', new Color(170, 170, 170));
            LEGACY_COLORS.put('8', new Color(85, 85, 85));
            LEGACY_COLORS.put('9', new Color(85, 85, 255));
            LEGACY_COLORS.put('a', new Color(85, 255, 85));
            LEGACY_COLORS.put('b', new Color(85, 255, 255));
            LEGACY_COLORS.put('c', new Color(255, 85, 85));
            LEGACY_COLORS.put('d', new Color(255, 85, 255));
            LEGACY_COLORS.put('e', new Color(255, 255, 85));
            LEGACY_COLORS.put('f', new Color(255, 255, 255));
        }

        static List<ColoredChar> parse(String text) {
            List<ColoredChar> out = new ArrayList<>();
            if (text == null || text.isEmpty()) return out;

            Color current = Color.WHITE;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if ((c == '§' || c == '&') && i + 13 < text.length() && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')) {
                    StringBuilder hex = new StringBuilder();
                    for(int j=0; j<6; j++) {
                        hex.append(text.charAt(i + 3 + (j*2)));
                    }
                    Color parsed = GlyphAssets.tryParseHex6(hex.toString());
                    if (parsed != null) current = parsed;
                    i += 13;
                    continue;
                }

                if ((c == '§' || c == '&') && i + 7 < text.length() && text.charAt(i + 1) == '#') {
                    Color parsed = GlyphAssets.tryParseHex6(text.substring(i + 2, i + 8));
                    if (parsed != null) current = parsed;
                    i += 7;
                    continue;
                }

                if ((c == '§' || c == '&') && i + 1 < text.length()) {
                    char code = Character.toLowerCase(text.charAt(i + 1));
                    if (LEGACY_COLORS.containsKey(code)) {
                        current = LEGACY_COLORS.get(code);
                        i += 1;
                        continue;
                    }
                    if ("lmno kr".indexOf(code) != -1) {
                        if (code == 'r') current = Color.WHITE;
                        i += 1;
                        continue;
                    }
                }

                if (c == '<') {
                    int end = text.indexOf('>', i);
                    if (end > i) {
                        String tag = text.substring(i + 1, end).toLowerCase(Locale.ROOT);
                        if (tag.startsWith("#") && tag.length() == 7) {
                            Color parsed = GlyphAssets.tryParseHex6(tag.substring(1));
                            if (parsed != null) current = parsed;
                            i = end;
                            continue;
                        } else if (tag.equals("/") || tag.equals("reset")) {
                            current = Color.WHITE;
                            i = end;
                            continue;
                        }
                    }
                }

                out.add(new ColoredChar(c, current));
            }
            return out;
        }
    }

    // ---------------------------------------------------------------------
    // COMPAT HELPERS
    // ---------------------------------------------------------------------

    private static final class TransformComponentCompat {
        private static volatile boolean cached = false;

        private static Method mSetPosition;
        private static Method mSetRotation;
        private static Method mSetTransformPR;   // setTransform(Vector3d, Vector3f)
        private static Method mSetTransformObj;  // setTransform(Transform)
        private static Method mGetTransform;     // getTransform()
        private static Method mMarkChunkDirty;

        private static java.lang.reflect.Constructor<?> cTransform;

        static void apply(@Nonnull Store<EntityStore> store,
                          @Nonnull TransformComponent tx,
                          @Nonnull Vector3d pos,
                          @Nonnull Vector3f rot) {

            tryInit(tx);

            boolean updated = false;

            if (mSetTransformPR != null) {
                try {
                    mSetTransformPR.invoke(tx, pos, rot);
                    updated = true;
                } catch (Throwable ignored) {}
            }

            if (!updated) {
                boolean didPos = false;
                if (mSetPosition != null) {
                    try { mSetPosition.invoke(tx, pos); didPos = true; } catch (Throwable ignored) {}
                }
                boolean didRot = false;
                if (mSetRotation != null) {
                    try { mSetRotation.invoke(tx, rot); didRot = true; } catch (Throwable ignored) {}
                }
                if (didPos || didRot) updated = true;
            }

            if (!updated && mSetTransformObj != null) {
                Object transformObj = null;

                if (cTransform != null) {
                    try {
                        transformObj = cTransform.newInstance(pos, rot);
                    } catch (Throwable ignored) {}
                }

                if (transformObj == null && mGetTransform != null) {
                    try {
                        transformObj = mGetTransform.invoke(tx);
                    } catch (Throwable ignored) {}
                }

                if (transformObj != null) {
                    try {
                        mSetTransformObj.invoke(tx, transformObj);
                    } catch (Throwable ignored) {}
                }
            }

            if (mMarkChunkDirty != null) {
                try {
                    mMarkChunkDirty.invoke(tx, store);
                } catch (Throwable ignored) {}
            }
        }

        private static void tryInit(@Nonnull TransformComponent tx) {
            if (cached) return;

            synchronized (TransformComponentCompat.class) {
                if (cached) return;

                Class<?> cls = tx.getClass();

                mSetTransformPR = findMethod(cls, "setTransform", Vector3d.class, Vector3f.class);
                mSetPosition    = findMethod(cls, "setPosition", Vector3d.class);
                mSetRotation    = findMethod(cls, "setRotation", Vector3f.class);
                mMarkChunkDirty = findMethod(cls, "markChunkDirty", Store.class);

                for (Method m : cls.getMethods()) {
                    if (!m.getName().equals("setTransform")) continue;
                    if (m.getParameterCount() == 1) {
                        mSetTransformObj = m;
                        break;
                    }
                }

                mGetTransform = findMethod(cls, "getTransform");

                if (mSetTransformObj != null) {
                    Class<?> transformType = mSetTransformObj.getParameterTypes()[0];
                    try {
                        cTransform = transformType.getConstructor(Vector3d.class, Vector3f.class);
                    } catch (Throwable ignored) {
                        cTransform = null;
                    }
                }

                cached = true;
            }
        }

        private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
            try {
                return cls.getMethod(name, params);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    }

    private static final class EntityRemoveCompat {
        static void remove(@Nonnull Store<EntityStore> store,
                           @Nonnull EntityStore entityStore,
                           @Nonnull Ref<EntityStore> ref) {

            if (tryInvoke(store, "removeEntity", new Class[]{Ref.class, Object.class}, ref, null)) return;
            if (tryInvoke(store, "removeEntity", new Class[]{Ref.class}, ref)) return;
            if (tryInvoke(store, "deleteEntity", new Class[]{Ref.class}, ref)) return;

            if (tryInvoke(entityStore, "removeEntity", new Class[]{Ref.class, Object.class}, ref, null)) return;
            if (tryInvoke(entityStore, "removeEntity", new Class[]{Ref.class}, ref)) return;
        }

        private static boolean tryInvoke(Object target, String name, Class<?>[] sig, Object... args) {
            try {
                Method m = target.getClass().getMethod(name, sig);
                m.invoke(target, args);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}