package com.mystichorizons.mysticnametags.nameplate.render.image.backend;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mystichorizons.mysticnametags.nameplate.image.RuntimeNameplateAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class HytaleImageNameplateBackend implements ImageNameplateBackend {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    @Override
    public ImageBackendSpawnResult spawn(@Nonnull PlayerRef owner,
                                         @Nonnull World world,
                                         @Nonnull Store<EntityStore> store,
                                         @Nonnull Ref<EntityStore> ownerRef,
                                         @Nonnull RuntimeNameplateAsset asset,
                                         boolean billboard) {

        String assetId = billboard
                ? asset.getBillboardModelAssetId()
                : asset.getModelAssetId();

        try {
            ModelAsset modelAsset = (ModelAsset) ModelAsset.getAssetMap().getAsset(assetId);
            if (modelAsset == null) {
                return ImageBackendSpawnResult.failure("ModelAsset not loaded: " + assetId);
            }

            Ref<EntityStore> entityRef = createImageEntity(world, store, ownerRef, modelAsset, assetId, billboard);
            if (entityRef == null || !entityRef.isValid()) {
                return ImageBackendSpawnResult.failure("Entity creation returned null/invalid ref");
            }

            UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
            if (uuidComponent == null) {
                return ImageBackendSpawnResult.failure("Spawned entity missing UUIDComponent");
            }

            return ImageBackendSpawnResult.success(uuidComponent.getUuid());
        } catch (Throwable t) {
            LOGGER.at(Level.WARNING).withCause(t)
                    .log("[MysticNameTags] Failed to spawn image nameplate backend entity for %s", owner.getUsername());
            return ImageBackendSpawnResult.failure("Exception during spawn: " + t.getMessage());
        }
    }

    @Override
    public void despawn(@Nonnull World world,
                        @Nonnull Store<EntityStore> store,
                        @Nullable UUID entityUuid) {
        if (entityUuid == null) {
            return;
        }

        try {
            Ref<EntityStore> ref = ((EntityStore) store.getExternalData()).getRefFromUUID(entityUuid);
            if (ref != null && ref.isValid()) {
                store.removeEntity(ref, RemoveReason.REMOVE);
            }
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).withCause(t)
                    .log("[MysticNameTags] Failed to despawn image nameplate entity %s", entityUuid);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return ModelAsset.getAssetMap() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Nullable
    private Ref<EntityStore> createImageEntity(@Nonnull World world,
                                               @Nonnull Store<EntityStore> store,
                                               @Nonnull Ref<EntityStore> ownerRef,
                                               @Nonnull ModelAsset modelAsset,
                                               @Nonnull String assetId,
                                               boolean billboard) {

        TransformComponent ownerTransform =
                store.getComponent(ownerRef, TransformComponent.getComponentType());

        if (ownerTransform == null) {
            return null;
        }

        Vector3d ownerPos = ownerTransform.getPosition();

        // Initial spawn slightly above player head.
        // Follow logic will keep it in sync later.
        Vector3d pos = new Vector3d(
                ownerPos.getX(),
                ownerPos.getY() + 2.35D,
                ownerPos.getZ()
        );

        Vector3f rotation = new Vector3f(0.0F, 0.0F, 0.0F);

        Model model = Model.createStaticScaledModel(modelAsset, 1.0F);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        UUIDComponent uuidComponent = UUIDComponent.randomUUID();

        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(pos, rotation));
        holder.addComponent(UUIDComponent.getComponentType(), uuidComponent);
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));

        Model.ModelReference modelRef =
                new Model.ModelReference(model.getModelAssetId(), 1.0F, (Map<?, ?>) null, true);

        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(modelRef));
        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(1.0F));
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore) store.getExternalData()).takeNextNetworkId()));

        holder.ensureComponent(Intangible.getComponentType());
        holder.ensureComponent(EntityModule.get().getVisibleComponentType());
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        if (!billboard) {
            holder.ensureComponent(Frozen.getComponentType());
        }

        Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);

        if (ref != null && ref.isValid()) {
            LOGGER.at(Level.FINE)
                    .log("[MysticNameTags] Spawned image nameplate entity %s using asset %s",
                            uuidComponent.getUuid(), assetId);
        }

        return ref;
    }
}