package com.mystichorizons.mysticnametags.nameplate.render;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public final class NameplateRendererRegistry {

    private final Map<NameplateRenderMode, NameplateRenderer> renderers =
            new EnumMap<>(NameplateRenderMode.class);

    public void register(@Nonnull NameplateRenderMode mode,
                         @Nonnull NameplateRenderer renderer) {
        renderers.put(mode, renderer);
    }

    @Nullable
    public NameplateRenderer get(@Nonnull NameplateRenderMode mode) {
        return renderers.get(mode);
    }

    public void initializeAll() {
        for (NameplateRenderer renderer : renderers.values()) {
            renderer.initialize();
        }
    }

    public void shutdownAll() {
        for (NameplateRenderer renderer : renderers.values()) {
            renderer.shutdown();
        }
    }
}