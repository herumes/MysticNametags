package com.mystichorizons.mysticnametags.nameplate.render.image;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.nameplate.model.ResolvedNameplate;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class ImageNameplateAnchorManager {

    public void updateAnchor(@Nonnull PlayerRef playerRef,
                             @Nonnull World world,
                             @Nonnull Store<EntityStore> store,
                             @Nonnull Ref<EntityStore> ownerRef,
                             @Nonnull UUID entityUuid,
                             @Nonnull ResolvedNameplate resolvedNameplate) {

        Ref<EntityStore> imageRef = ((EntityStore) store.getExternalData()).getRefFromUUID(entityUuid);
        if (imageRef == null || !imageRef.isValid()) {
            return;
        }

        TransformComponent ownerTransform = store.getComponent(ownerRef, TransformComponent.getComponentType());
        TransformComponent imageTransform = store.getComponent(imageRef, TransformComponent.getComponentType());

        if (ownerTransform == null || imageTransform == null) {
            return;
        }

        double yOffset = resolvedNameplate.getAppearance().getVerticalOffset();

        Vector3d ownerPos = ownerTransform.getPosition();
        imageTransform.getPosition().assign(
                ownerPos.getX(),
                ownerPos.getY() + yOffset,
                ownerPos.getZ()
        );
        imageTransform.markChunkDirty(store);
    }
}